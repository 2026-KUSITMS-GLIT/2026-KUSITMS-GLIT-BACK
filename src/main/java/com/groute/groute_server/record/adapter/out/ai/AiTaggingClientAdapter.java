package com.groute.groute_server.record.adapter.out.ai;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.adapter.out.ai.dto.AiTaggingRequest;
import com.groute.groute_server.record.adapter.out.ai.dto.AiTaggingResponse;
import com.groute.groute_server.record.application.port.in.CompleteAiTaggingUseCase;
import com.groute.groute_server.record.application.port.out.AiTaggingClient;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.Scrum;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link AiTaggingClient}의 FastAPI 실 구현체.
 *
 * <p>STAR 기록의 ST/A/R 텍스트와 직군, 선택 역량을 FastAPI {@code POST /api/tagging}에 전송하고,
 * 응답으로 받은 primaryCategory + detailTags를 {@link CompleteAiTaggingUseCase}에 전달한다.
 * 실패 시 최대 1회 재시도하며, 재시도 후에도 실패 시 FAILED로 확정한다.
 */
@Slf4j
@Component
public class AiTaggingClientAdapter implements AiTaggingClient {

    private final RestClient restClient;
    private final AiTaggingJobPort aiTaggingJobPort;
    private final CompleteAiTaggingUseCase completeAiTaggingUseCase;

    @Autowired
    public AiTaggingClientAdapter(
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ai.internal-token:}") String internalToken,
            @Value("${ai.timeout.connect:5000}") int connectTimeoutMs,
            @Value("${ai.timeout.read:30000}") int readTimeoutMs,
            AiTaggingJobPort aiTaggingJobPort,
            CompleteAiTaggingUseCase completeAiTaggingUseCase) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs));
        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .requestFactory(factory)
                        .defaultHeader("X-Internal-Token", internalToken)
                        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .build();
        this.aiTaggingJobPort = aiTaggingJobPort;
        this.completeAiTaggingUseCase = completeAiTaggingUseCase;
    }

    /** 테스트용 생성자. RestClient를 직접 주입받는다. */
    AiTaggingClientAdapter(
            RestClient restClient,
            AiTaggingJobPort aiTaggingJobPort,
            CompleteAiTaggingUseCase completeAiTaggingUseCase) {
        this.restClient = restClient;
        this.aiTaggingJobPort = aiTaggingJobPort;
        this.completeAiTaggingUseCase = completeAiTaggingUseCase;
    }

    @Override
    public void requestTagging(AiTaggingJob job) {
        StarRecord starRecord = job.getStarRecord();
        Scrum scrum = starRecord.getScrum();

        AiTaggingRequest request =
                new AiTaggingRequest(
                        starRecord.getUser().getJobRole().name(),
                        scrum.getSelectedCompetency() != null
                                ? scrum.getSelectedCompetency().name()
                                : "",
                        starRecord.getSituationTask(),
                        starRecord.getAction(),
                        starRecord.getResult());

        // request payload 저장 및 RUNNING 전환
        Map<String, Object> requestPayload =
                Map.of(
                        "jobRole", request.jobRole(),
                        "selectedCompetency", request.selectedCompetency(),
                        "situationTask", request.situationTask(),
                        "action", request.action(),
                        "result", request.result());
        job.start(requestPayload);
        aiTaggingJobPort.saveJob(job);

        try {
            AiTaggingResponse response = callFastApi(request);

            Map<String, Object> responsePayload =
                    Map.of(
                            "primaryCategory", response.primaryCategory(),
                            "detailTags", response.detailTags());
            job.succeed(responsePayload);
            aiTaggingJobPort.saveJob(job);

            completeAiTaggingUseCase.completeTagging(starRecord.getId());

        } catch (BusinessException e) {
            handleFailure(job, e.getMessage(), request);
        }
    }

    private AiTaggingResponse callFastApi(AiTaggingRequest request) {
        try {
            return restClient
                    .post()
                    .uri("/api/tagging")
                    .body(request)
                    .retrieve()
                    .body(AiTaggingResponse.class);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("[AI Tagging] 타임아웃 발생", e);
                throw new BusinessException(ErrorCode.AI_SERVER_TIMEOUT);
            }
            log.error("[AI Tagging] 네트워크 오류", e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (RestClientException e) {
            log.error("[AI Tagging] 호출 실패 — {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    private void handleFailure(AiTaggingJob job, String errorMessage, AiTaggingRequest request) {
        if (job.isRetryable()) {
            log.warn(
                    "[AI Tagging] 1차 실패, 재시도 — jobId={}, starRecordId={}",
                    job.getId(),
                    job.getStarRecord().getId());
            job.fail(errorMessage);
            aiTaggingJobPort.saveJob(job);
            try {
                AiTaggingResponse response = callFastApi(request);
                Map<String, Object> responsePayload =
                        Map.of(
                                "primaryCategory", response.primaryCategory(),
                                "detailTags", response.detailTags());
                job.succeed(responsePayload);
                aiTaggingJobPort.saveJob(job);
                completeAiTaggingUseCase.completeTagging(job.getStarRecord().getId());
            } catch (BusinessException retryEx) {
                log.error(
                        "[AI Tagging] 재시도 실패 확정 — jobId={}, error={}",
                        job.getId(),
                        retryEx.getMessage());
                job.fail(retryEx.getMessage());
                aiTaggingJobPort.saveJob(job);
            }
        } else {
            log.error(
                    "[AI Tagging] 최종 실패 — jobId={}, error={}",
                    job.getId(),
                    errorMessage);
            job.fail(errorMessage);
            aiTaggingJobPort.saveJob(job);
        }
    }
}
