package com.groute.groute_server.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.service.ScrumTitleCommitter;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;

@ExtendWith(MockitoExtension.class)
class ScrumTitleCommitterTest {

    private static final long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 9);

    @Mock StarRecordRepositoryPort starRecordPort;
    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;

    @InjectMocks ScrumTitleCommitter scrumTitleCommitter;

    private static Scrum scrumWithTitle(Long titleId) {
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", titleId);
        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "title", title);
        return scrum;
    }

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("성공 — 미완료가 없으면 distinct title id로 commitAllByIds 호출")
        void commitsDistinctTitleIds_whenAllTagged() {
            // given
            given(starRecordPort.existsUntaggedByUserAndDate(USER_ID, DATE)).willReturn(false);
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(
                            List.of(
                                    scrumWithTitle(100L),
                                    scrumWithTitle(100L),
                                    scrumWithTitle(200L)));

            // when
            scrumTitleCommitter.commitIfFullyTagged(USER_ID, DATE);

            // then
            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(scrumTitleRepositoryPort).commitAllByIds(captor.capture());
            assertThat(captor.getValue()).containsExactly(100L, 200L);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("실패 — 미완료 StarRecord가 남아있으면 commit 호출 안 됨")
        void skipsCommit_whenUntaggedRemain() {
            // given
            given(starRecordPort.existsUntaggedByUserAndDate(USER_ID, DATE)).willReturn(true);

            // when
            scrumTitleCommitter.commitIfFullyTagged(USER_ID, DATE);

            // then
            verify(scrumTitleRepositoryPort, never()).commitAllByIds(any());
        }
    }
}
