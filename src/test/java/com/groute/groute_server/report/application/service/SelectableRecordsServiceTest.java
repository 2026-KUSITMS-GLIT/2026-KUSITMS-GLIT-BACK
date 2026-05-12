package com.groute.groute_server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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

import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.application.port.in.SelectableRecordsView;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;

@ExtendWith(MockitoExtension.class)
class SelectableRecordsServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 9);

    @Mock LoadStarRecordPort loadStarRecordPort;

    @InjectMocks SelectableRecordsService service;

    @Nested
    @DisplayName("날짜별 심화기록 모달 조회")
    class GetSelectableRecords {

        @Test
        @DisplayName("해당 날짜에 완료된 심화기록이 있으면 목록을 반환한다")
        void should_returnItems_when_starRecordsExist() {
            // given
            StarRecord sr1 = starRecord(1L, "KOPLE", "PRD와 기능명세서를 완성");
            StarRecord sr2 = starRecord(2L, "밋업 기획", "이해관계자 요구사항 정리");
            given(loadStarRecordPort.findCompletedByUserIdAndDate(USER_ID, DATE))
                    .willReturn(List.of(sr1, sr2));

            // when
            SelectableRecordsView view = service.getSelectableRecords(USER_ID, DATE);

            // then
            assertThat(view.date()).isEqualTo("2026-04-09");
            assertThat(view.starRecords()).hasSize(2);
            assertThat(view.starRecords().get(0).starRecordId()).isEqualTo(1L);
            assertThat(view.starRecords().get(0).projectName()).isEqualTo("KOPLE");
            assertThat(view.starRecords().get(0).scrumContent()).isEqualTo("PRD와 기능명세서를 완성");
        }

        @Test
        @DisplayName("해당 날짜에 완료된 심화기록이 없으면 빈 배열을 반환한다")
        void should_returnEmptyList_when_noStarRecords() {
            // given
            given(loadStarRecordPort.findCompletedByUserIdAndDate(USER_ID, DATE))
                    .willReturn(List.of());

            // when
            SelectableRecordsView view = service.getSelectableRecords(USER_ID, DATE);

            // then
            assertThat(view.date()).isEqualTo("2026-04-09");
            assertThat(view.starRecords()).isEmpty();
        }
    }

    // =========================================================
    // helpers
    // =========================================================

    private static StarRecord starRecord(Long id, String projectName, String scrumContent) {
        Project project = Project.builder().name(projectName).build();

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "project", project);

        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "title", title);
        ReflectionTestUtils.setField(scrum, "content", scrumContent);
        ReflectionTestUtils.setField(scrum, "scrumDate", LocalDate.of(2026, 4, 9));

        StarRecord sr = new StarRecord();
        ReflectionTestUtils.setField(sr, "id", id);
        ReflectionTestUtils.setField(sr, "scrum", scrum);
        return sr;
    }
}
