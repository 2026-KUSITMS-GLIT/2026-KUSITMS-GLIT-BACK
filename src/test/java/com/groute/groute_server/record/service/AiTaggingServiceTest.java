package com.groute.groute_server.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingResultResponse;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingStatusResponse;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.application.service.AiTaggingService;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.JobStatus;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AiTaggingServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long STAR_RECORD_ID = 10L;

    private static final LocalDate DATE = LocalDate.of(2026, 5, 9);

    @Mock private StarRecordRepositoryPort starRecordPort;
    @Mock private AiTaggingJobPort aiTaggingJobPort;
    @Mock private StarTagQueryPort starTagPort;
    @Mock private ScrumQueryPort scrumQueryPort;
    @Mock private ScrumTitleRepositoryPort scrumTitleRepositoryPort;

    @InjectMocks private AiTaggingService aiTaggingService;

    // =========================================================
    // 테스트 픽스처 헬퍼
    // =========================================================

    private StarRecord makeStarRecord(Long userId, StarStep step) {
        User user = User.createForSocialLogin();
        ReflectionTestUtils.setField(user, "id", userId);

        StarRecord record = new StarRecord();
        ReflectionTestUtils.setField(record, "user", user);
        ReflectionTestUtils.setField(record, "currentStep", step);
        if (step == StarStep.DONE) {
            ReflectionTestUtils.setField(record, "status", StarRecordStatus.WRITTEN);
        }
        return record;
    }

    private StarRecord makeStarRecordWithScrum(Long userId, StarStep step) {
        StarRecord record = makeStarRecord(userId, step);
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 100L);
        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "scrumDate", DATE);
        ReflectionTestUtils.setField(scrum, "title", title);
        ReflectionTestUtils.setField(record, "scrum", scrum);
        return record;
    }

    private static Scrum scrumWithTitle(Long scrumId, Long titleId) {
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", titleId);
        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "id", scrumId);
        ReflectionTestUtils.setField(scrum, "title", title);
        return scrum;
    }

    private AiTaggingJob makeJob(JobStatus status, int retryCount) {
        AiTaggingJob job = new AiTaggingJob();
        ReflectionTestUtils.setField(job, "status", status);
        ReflectionTestUtils.setField(job, "retryCount", (short) retryCount);
        return job;
    }

    private StarTag makeStarTag(CompetencyCategory category, String detailTag) {
        StarTag tag = new StarTag();
        ReflectionTestUtils.setField(tag, "primaryCategory", category);
        ReflectionTestUtils.setField(tag, "detailTag", detailTag);
        return tag;
    }

    // =========================================================
    // REC-005: trigger
    // =========================================================

    @Nested
    @DisplayName("AI 태깅 트리거 (REC-005)")
    class Trigger {

        @Test
        @DisplayName("성공 — 잡 없을 때 새 잡 생성")
        void createsNewJob_whenNoJobExists() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.empty());

            aiTaggingService.trigger(STAR_RECORD_ID, USER_ID);

            verify(aiTaggingJobPort).save(record);
        }

        @Test
        @DisplayName("성공 — FAILED && retryCount=0 일 때 새 잡 생성")
        void createsNewJob_whenPreviousJobFailedWithRetryCount0() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob failedJob = makeJob(JobStatus.FAILED, 0);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(failedJob));

            aiTaggingService.trigger(STAR_RECORD_ID, USER_ID);

            verify(aiTaggingJobPort).save(record);
        }

        @Test
        @DisplayName("실패 — StarRecord 없으면 STAR_RECORD_NOT_FOUND")
        void throwsNotFound_whenStarRecordMissing() {
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.trigger(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.STAR_RECORD_NOT_FOUND);

            verify(aiTaggingJobPort, never()).save(any());
        }

        @Test
        @DisplayName("실패 — 본인 소유 아니면 FORBIDDEN")
        void throwsForbidden_whenNotOwner() {
            StarRecord record = makeStarRecord(OTHER_USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> aiTaggingService.trigger(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(aiTaggingJobPort, never()).save(any());
        }

        @Test
        @DisplayName("실패 — DONE 단계 아니면 STAR_RECORD_NOT_READY")
        void throwsNotReady_whenStepIsNotDone() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.R);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> aiTaggingService.trigger(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.STAR_RECORD_NOT_READY);

            verify(aiTaggingJobPort, never()).save(any());
        }

        @Test
        @DisplayName("실패 — RUNNING이면 AI_TAGGING_ALREADY_RUNNING")
        void throwsAlreadyRunning_whenJobIsRunning() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob runningJob = makeJob(JobStatus.RUNNING, 0);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(runningJob));

            assertThatThrownBy(() -> aiTaggingService.trigger(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.AI_TAGGING_ALREADY_RUNNING);

            verify(aiTaggingJobPort, never()).save(any());
        }

        @Test
        @DisplayName("실패 — FAILED && retryCount=1이면 AI_TAGGING_PERMANENTLY_FAILED")
        void throwsPermanentlyFailed_whenRetryCountIs1() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob failedJob = makeJob(JobStatus.FAILED, 1);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(failedJob));

            assertThatThrownBy(() -> aiTaggingService.trigger(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.AI_TAGGING_PERMANENTLY_FAILED);

            verify(aiTaggingJobPort, never()).save(any());
        }
    }

    // =========================================================
    // REC-006: getStatus
    // =========================================================

    @Nested
    @DisplayName("AI 태깅 상태 폴링 (REC-006)")
    class GetStatus {

        @Test
        @DisplayName("성공 — 잡 상태와 retryCount 반환")
        void returnsStatus_whenJobExists() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob job = makeJob(JobStatus.RUNNING, 0);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(job));

            AiTaggingStatusResponse response = aiTaggingService.getStatus(STAR_RECORD_ID, USER_ID);

            assertThat(response.status()).isEqualTo(JobStatus.RUNNING);
            assertThat(response.retryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패 — StarRecord 없으면 STAR_RECORD_NOT_FOUND")
        void throwsNotFound_whenStarRecordMissing() {
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.getStatus(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.STAR_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — 본인 소유 아니면 FORBIDDEN")
        void throwsForbidden_whenNotOwner() {
            StarRecord record = makeStarRecord(OTHER_USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> aiTaggingService.getStatus(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("실패 — 잡 없으면 AI_TAGGING_JOB_NOT_FOUND")
        void throwsJobNotFound_whenJobMissing() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.getStatus(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.AI_TAGGING_JOB_NOT_FOUND);
        }
    }

    // =========================================================
    // REC-007: getResult
    // =========================================================

    @Nested
    @DisplayName("AI 태깅 결과 조회 (REC-007)")
    class GetResult {

        @Test
        @DisplayName("성공 — SUCCESS 상태일 때 태그 반환")
        void returnsResult_whenJobIsSuccess() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob job = makeJob(JobStatus.SUCCESS, 0);
            List<StarTag> tags =
                    List.of(
                            makeStarTag(CompetencyCategory.DISCOVERY_ANALYSIS, "이해관계자 조율"),
                            makeStarTag(CompetencyCategory.DISCOVERY_ANALYSIS, "UX 설계"),
                            makeStarTag(CompetencyCategory.DISCOVERY_ANALYSIS, "품질 관리"));
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(job));
            given(starTagPort.findAllByStarRecordId(STAR_RECORD_ID)).willReturn(tags);

            AiTaggingResultResponse response = aiTaggingService.getResult(STAR_RECORD_ID, USER_ID);

            assertThat(response.status()).isEqualTo(JobStatus.SUCCESS);
            assertThat(response.primaryCategory()).isEqualTo(CompetencyCategory.DISCOVERY_ANALYSIS);
            assertThat(response.detailTags()).containsExactly("이해관계자 조율", "UX 설계", "품질 관리");
        }

        @Test
        @DisplayName("실패 — StarRecord 없으면 STAR_RECORD_NOT_FOUND")
        void throwsNotFound_whenStarRecordMissing() {
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.getResult(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.STAR_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — 본인 소유 아니면 FORBIDDEN")
        void throwsForbidden_whenNotOwner() {
            StarRecord record = makeStarRecord(OTHER_USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> aiTaggingService.getResult(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("실패 — 잡 없으면 AI_TAGGING_JOB_NOT_FOUND")
        void throwsJobNotFound_whenJobMissing() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.getResult(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.AI_TAGGING_JOB_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — SUCCESS 아니면 AI_TAGGING_NOT_COMPLETED")
        void throwsNotCompleted_whenJobIsNotSuccess() {
            StarRecord record = makeStarRecord(USER_ID, StarStep.DONE);
            AiTaggingJob job = makeJob(JobStatus.RUNNING, 0);
            given(starRecordPort.findById(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(aiTaggingJobPort.findLatestByStarRecordId(STAR_RECORD_ID))
                    .willReturn(Optional.of(job));

            assertThatThrownBy(() -> aiTaggingService.getResult(STAR_RECORD_ID, USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.AI_TAGGING_NOT_COMPLETED);

            verify(starTagPort, never()).findAllByStarRecordId(any());
        }
    }

    // =========================================================
    // completeTagging
    // =========================================================

    @Nested
    @DisplayName("AI 태깅 완료 처리")
    class CompleteTagging {

        @Test
        @DisplayName("성공 — 세션 내 마지막 태깅 완료 시 StarRecord TAGGED + ScrumTitle COMMITTED")
        void marksTaggedAndCommits_whenAllTagged() {
            StarRecord record = makeStarRecordWithScrum(USER_ID, StarStep.DONE);
            given(starRecordPort.findByIdWithScrum(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(starRecordPort.existsUntaggedByUserAndDate(USER_ID, DATE)).willReturn(false);
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of(scrumWithTitle(1L, 100L), scrumWithTitle(2L, 100L)));

            aiTaggingService.completeTagging(STAR_RECORD_ID);

            assertThat(record.getStatus()).isEqualTo(StarRecordStatus.TAGGED);
            verify(scrumTitleRepositoryPort).commitAllByIds(List.of(100L));
        }

        @Test
        @DisplayName("성공 — 아직 미완료 StarRecord 있으면 TAGGED 전환만 하고 COMMITTED 안 함")
        void marksTaggedOnly_whenUntaggedRemain() {
            StarRecord record = makeStarRecordWithScrum(USER_ID, StarStep.DONE);
            given(starRecordPort.findByIdWithScrum(STAR_RECORD_ID)).willReturn(Optional.of(record));
            given(starRecordPort.existsUntaggedByUserAndDate(USER_ID, DATE)).willReturn(true);

            aiTaggingService.completeTagging(STAR_RECORD_ID);

            assertThat(record.getStatus()).isEqualTo(StarRecordStatus.TAGGED);
            verify(scrumTitleRepositoryPort, never()).commitAllByIds(any());
        }

        @Test
        @DisplayName("실패 — StarRecord 없으면 STAR_RECORD_NOT_FOUND")
        void throwsNotFound_whenStarRecordMissing() {
            given(starRecordPort.findByIdWithScrum(STAR_RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiTaggingService.completeTagging(STAR_RECORD_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.STAR_RECORD_NOT_FOUND);
        }
    }
}
