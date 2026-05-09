package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

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
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand.ScrumCompetency;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ScrumCompetencyUpdateServiceTest {

    private static final Long USER_ID = 1L;

    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumWritePort scrumWritePort;

    @InjectMocks ScrumCompetencyUpdateService service;

    @Nested
    @DisplayName("예외 케이스")
    class Validation {

        @Test
        @DisplayName("요청한 scrumId가 모두 본인 소유가 아니면 SCRUM_NOT_FOUND를 던진다")
        void should_throwScrumNotFound_when_noneOwned() {
            given(scrumQueryPort.findAllByIdInAndUserId(List.of(10L, 20L), USER_ID))
                    .willReturn(List.of());

            assertThatThrownBy(
                            () ->
                                    service.updateCompetency(
                                            command(
                                                    item(10L, CompetencyCategory.COLLABORATION),
                                                    item(20L, CompetencyCategory.PROBLEM_SOLVING))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);

            verify(scrumWritePort, never()).updateCompetency(10L, CompetencyCategory.COLLABORATION);
            verify(scrumWritePort, never())
                    .updateCompetency(20L, CompetencyCategory.PROBLEM_SOLVING);
        }

        @Test
        @DisplayName("중복 scrumId가 포함되면 INVALID_INPUT을 던진다")
        void should_throwInvalidInput_when_duplicateScrumId() {
            assertThatThrownBy(
                            () ->
                                    service.updateCompetency(
                                            command(
                                                    item(10L, CompetencyCategory.COLLABORATION),
                                                    item(10L, CompetencyCategory.PROBLEM_SOLVING))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(scrumWritePort, never()).updateCompetency(10L, CompetencyCategory.COLLABORATION);
        }

        @Test
        @DisplayName("요청한 scrumId 중 일부만 본인 소유이면 SCRUM_NOT_FOUND를 던진다")
        void should_throwScrumNotFound_when_partiallyOwned() {
            given(scrumQueryPort.findAllByIdInAndUserId(List.of(10L, 20L), USER_ID))
                    .willReturn(List.of(scrum(10L)));

            assertThatThrownBy(
                            () ->
                                    service.updateCompetency(
                                            command(
                                                    item(10L, CompetencyCategory.COLLABORATION),
                                                    item(20L, CompetencyCategory.PROBLEM_SOLVING))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("단일 스크럼 역량을 정상 업데이트한다")
        void should_updateCompetency_when_singleItem() {
            given(scrumQueryPort.findAllByIdInAndUserId(List.of(10L), USER_ID))
                    .willReturn(List.of(scrum(10L)));

            service.updateCompetency(command(item(10L, CompetencyCategory.COLLABORATION)));

            verify(scrumWritePort).updateCompetency(10L, CompetencyCategory.COLLABORATION);
        }

        @Test
        @DisplayName("복수 스크럼 역량을 각각 업데이트한다")
        void should_updateEachCompetency_when_multipleItems() {
            given(scrumQueryPort.findAllByIdInAndUserId(List.of(10L, 20L), USER_ID))
                    .willReturn(List.of(scrum(10L), scrum(20L)));

            service.updateCompetency(
                    command(
                            item(10L, CompetencyCategory.COLLABORATION),
                            item(20L, CompetencyCategory.PROBLEM_SOLVING)));

            verify(scrumWritePort).updateCompetency(10L, CompetencyCategory.COLLABORATION);
            verify(scrumWritePort).updateCompetency(20L, CompetencyCategory.PROBLEM_SOLVING);
        }
    }

    // ============== helpers ==============

    private static UpdateScrumCompetencyCommand command(ScrumCompetency... items) {
        return new UpdateScrumCompetencyCommand(USER_ID, List.of(items));
    }

    private static ScrumCompetency item(Long scrumId, CompetencyCategory competency) {
        return new ScrumCompetency(scrumId, competency);
    }

    private static Scrum scrum(Long id) {
        Scrum scrum =
                Scrum.create(
                        User.createForSocialLogin(),
                        new ScrumTitle(),
                        "내용",
                        LocalDate.of(2026, 5, 9));
        ReflectionTestUtils.setField(scrum, "id", id);
        return scrum;
    }
}
