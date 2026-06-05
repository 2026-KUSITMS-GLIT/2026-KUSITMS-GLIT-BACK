package com.groute.groute_server.record.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.record.adapter.out.ai.dto.AiTaggingResponse;
import com.groute.groute_server.record.application.port.in.CompleteAiTaggingUseCase;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.JobStatus;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(MockitoExtension.class)
class AiTaggingClientAdapterTest {

    MockWebServer mockWebServer;
    static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock AiTaggingJobPort aiTaggingJobPort;
    @Mock CompleteAiTaggingUseCase completeAiTaggingUseCase;

    AiTaggingClientAdapter adapter;

    private static final Long STAR_RECORD_ID = 1L;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("").toString();
        adapter =
                new AiTaggingClientAdapter(
                        baseUrl,
                        "test-token",
                        5000,
                        30000,
                        aiTaggingJobPort,
                        completeAiTaggingUseCase);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private AiTaggingJob makeJob() {
        User user = User.createForSocialLogin();
        ReflectionTestUtils.setField(user, "jobRole", JobRole.DEVELOPER);

        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(
                scrum, "selectedCompetency", CompetencyCategory.PROBLEM_SOLVING);

        StarRecord starRecord = new StarRecord();
        ReflectionTestUtils.setField(starRecord, "id", STAR_RECORD_ID);
        ReflectionTestUtils.setField(starRecord, "user", user);
        ReflectionTestUtils.setField(starRecord, "scrum", scrum);
        ReflectionTestUtils.setField(starRecord, "situationTask", "상황");
        ReflectionTestUtils.setField(starRecord, "action", "행동");
        ReflectionTestUtils.setField(starRecord, "result", "결과");

        AiTaggingJob job = new AiTaggingJob(starRecord);
        ReflectionTestUtils.setField(job, "id", 10L);
        return job;
    }

    private MockResponse jsonResponse(Object body) throws Exception {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(body));
    }

    @Nested
    @DisplayName("AI 태깅 성공")
    class Success {

        @Test
        @DisplayName("FastAPI 정상 응답 시 job SUCCESS 전환 및 completeTagging 호출")
        void should_succeedAndCompleteTagging_when_fastApiRespondsNormally() throws Exception {
            AiTaggingJob job = makeJob();
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));
            mockWebServer.enqueue(
                    jsonResponse(new AiTaggingResponse("PROBLEM_SOLVING", List.of("문제해결", "개선"))));

            adapter.requestTagging(job);

            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/v1/tagging");
            assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCESS);
            then(completeAiTaggingUseCase)
                    .should()
                    .completeTagging(STAR_RECORD_ID, "PROBLEM_SOLVING", List.of("문제해결", "개선"));
            then(aiTaggingJobPort).should(times(2)).saveJob(job);
        }
    }

    @Nested
    @DisplayName("AI 태깅 실패")
    class Failure {

        @Test
        @DisplayName("1차 실패 후 재시도 성공 시 job SUCCESS 전환")
        void should_succeedOnRetry_when_firstCallFails() throws Exception {
            AiTaggingJob job = makeJob();
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockWebServer.enqueue(
                    jsonResponse(new AiTaggingResponse("PROBLEM_SOLVING", List.of("문제해결"))));

            adapter.requestTagging(job);

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCESS);
            then(completeAiTaggingUseCase)
                    .should()
                    .completeTagging(STAR_RECORD_ID, "PROBLEM_SOLVING", List.of("문제해결"));
        }

        @Test
        @DisplayName("1차 실패 후 재시도도 실패 시 job FAILED 확정")
        void should_failPermanently_when_retryAlsoFails() throws Exception {
            AiTaggingJob job = makeJob();
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            adapter.requestTagging(job);

            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
            assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(job.getRetryCount()).isEqualTo((short) 2);
            then(completeAiTaggingUseCase).should().revertTaggingComplete(STAR_RECORD_ID);
        }
    }
}
