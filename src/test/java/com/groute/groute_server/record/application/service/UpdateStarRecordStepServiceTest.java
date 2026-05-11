package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class UpdateStarRecordStepServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 2L;
    private static final Long STAR_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 9);

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock ScrumWritePort scrumWritePort;

    @InjectMocks UpdateStarRecordStepService service;

    private User owner;
    private Scrum scrum;
    private StarRecord record;

    @BeforeEach
    void setUp() {
        owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 10L);

        scrum = Scrum.create(owner, title, "내용", DATE);
        ReflectionTestUtils.setField(scrum, "id", 50L);

        record = StarRecord.create(owner, scrum);
        ReflectionTestUtils.setField(record, "id", STAR_ID);
    }

    @Nested
    @DisplayName("예외 케이스")
    class Validation {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
        }

        @Test
        @DisplayName("본인 소유가 아닌 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            User anotherUser = User.createForSocialLogin();
            ReflectionTestUtils.setField(anotherUser, "id", ANOTHER_USER_ID);
            StarRecord otherRecord = StarRecord.create(anotherUser, scrum);
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(otherRecord));

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 완료된(WRITTEN) StarRecord면 STAR_WRITE_LOCKED를 던진다")
        void should_throwWriteLocked_when_alreadyWritten() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            record.complete(OffsetDateTime.now());
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
        }
    }

    @Nested
    @DisplayName("단계 저장 — 중간 단계")
    class IntermediateStep {

        @Test
        @DisplayName("ST 단계 저장 시 completeStar는 호출되지 않는다")
        void should_notComplete_when_stepIsST() {
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            service.updateStep(command(StarStep.ST));

            verify(scrumWritePort, never()).completeStar(anyLong());
        }

        @Test
        @DisplayName("A 단계 저장 시 completeStar는 호출되지 않는다")
        void should_notComplete_when_stepIsA() {
            record.saveStep(StarStep.ST, "ST 답변");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            service.updateStep(command(StarStep.A));

            verify(scrumWritePort, never()).completeStar(anyLong());
        }
    }

    @Nested
    @DisplayName("R 단계 완료 처리")
    class CompleteStep {

        @Test
        @DisplayName("R 단계 저장 시 record.complete()와 scrumWritePort.completeStar가 호출된다")
        void should_callCompleteStar_when_stepIsR() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            service.updateStep(command(StarStep.R));

            verify(scrumWritePort).completeStar(scrum.getId());
        }
    }

    // ============== helpers ==============

    private UpdateStarRecordStepCommand command(StarStep step) {
        return new UpdateStarRecordStepCommand(USER_ID, STAR_ID, step, "답변 내용");
    }
}
