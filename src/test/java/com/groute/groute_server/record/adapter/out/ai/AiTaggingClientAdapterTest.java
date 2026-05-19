package com.groute.groute_server.record.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.adapter.out.ai.dto.AiTaggingRequest;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiTaggingClientAdapterTest {

    @Mock private AiTaggingJobPort aiTaggingJobPort;
    @Mock private CompleteAiTaggingUseCase completeAiTaggingUseCase;

    private RestClient mockRestClient;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    private AiTaggingClientAdapter adapter;

    private static final Long STAR_RECORD_ID = 1L;

    @BeforeEach
    void setUp() {
        mockRestClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        given(mockRestClient.post()).willReturn(uriSpec);
        given(uriSpec.uri("/api/tagging")).willReturn(bodySpec);
        given(bodySpec.body(ArgumentMatchers.any(AiTaggingRequest.class))).willReturn(bodySpec);
        given(bodySpec.retrieve()).willReturn(responseSpec);

        adapter =
                new AiTaggingClientAdapter(
                        mockRestClient, aiTaggingJobPort, completeAiTaggingUseCase);
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

    @Nested
    @DisplayName("AI 태깅 성공")
    class Success {

        @Test
        @DisplayName("FastAPI 정상 응답 시 job SUCCESS 전환 및 completeTagging 호출")
        void should_succeedAndCompleteTagging_when_fastApiRespondsNormally() {
            // given
            AiTaggingJob job = makeJob();
            given(responseSpec.body(AiTaggingResponse.class))
                    .willReturn(new AiTaggingResponse("PROBLEM_SOLVING", List.of("문제해결", "개선")));
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            adapter.requestTagging(job);

            // then
            assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCESS);
            then(completeAiTaggingUseCase).should().completeTagging(STAR_RECORD_ID);
            then(aiTaggingJobPort).should(times(2)).saveJob(job);
        }
    }

    @Nested
    @DisplayName("AI 태깅 실패")
    class Failure {

        @Test
        @DisplayName("1차 실패 후 재시도 성공 시 job SUCCESS 전환")
        void should_succeedOnRetry_when_firstCallFails() {
            // given
            AiTaggingJob job = makeJob();
            given(responseSpec.body(AiTaggingResponse.class))
                    .willThrow(new BusinessException(ErrorCode.AI_SERVER_ERROR))
                    .willReturn(new AiTaggingResponse("PROBLEM_SOLVING", List.of("문제해결")));
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            adapter.requestTagging(job);

            // then
            assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCESS);
            then(completeAiTaggingUseCase).should().completeTagging(STAR_RECORD_ID);
        }

        @Test
        @DisplayName("1차 실패 후 재시도도 실패 시 job FAILED 확정")
        void should_failPermanently_when_retryAlsoFails() {
            // given
            AiTaggingJob job = makeJob();
            given(responseSpec.body(AiTaggingResponse.class))
                    .willThrow(new BusinessException(ErrorCode.AI_SERVER_ERROR))
                    .willThrow(new BusinessException(ErrorCode.AI_SERVER_ERROR));
            given(aiTaggingJobPort.saveJob(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            adapter.requestTagging(job);

            // then
            assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(job.getRetryCount()).isEqualTo((short) 2);
            then(completeAiTaggingUseCase).shouldHaveNoInteractions();
        }
    }
}
