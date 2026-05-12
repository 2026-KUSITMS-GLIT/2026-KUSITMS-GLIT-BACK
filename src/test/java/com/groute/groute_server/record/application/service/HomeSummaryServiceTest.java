package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.enums.ReportModalType;

@ExtendWith(MockitoExtension.class)
class HomeSummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;

    @InjectMocks HomeSummaryService service;

    @Nested
    @DisplayName("isFirstStar")
    class IsFirstStar {

        @Test
        @DisplayName("tagged STAR가 1건이면 isFirstStar=true")
        void should_returnTrue_when_countIsOne() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(1L);
            assertThat(service.getSummary(USER_ID).isFirstStar()).isTrue();
        }

        @Test
        @DisplayName("tagged STAR가 0건이면 isFirstStar=false")
        void should_returnFalse_when_countIsZero() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(0L);
            assertThat(service.getSummary(USER_ID).isFirstStar()).isFalse();
        }

        @Test
        @DisplayName("tagged STAR가 2건 이상이면 isFirstStar=false")
        void should_returnFalse_when_countIsMoreThanOne() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(2L);
            assertThat(service.getSummary(USER_ID).isFirstStar()).isFalse();
        }
    }

    @Nested
    @DisplayName("reportModal")
    class ReportModalTest {

        @Test
        @DisplayName("10건이면 show=true, type=MINI")
        void should_returnMini_when_countIsTen() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(10L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isTrue();
            assertThat(modal.type()).isEqualTo(ReportModalType.MINI);
        }

        @Test
        @DisplayName("20건이면 show=true, type=FULL")
        void should_returnFull_when_countIsTwenty() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(20L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isTrue();
            assertThat(modal.type()).isEqualTo(ReportModalType.FULL);
        }

        @Test
        @DisplayName("30건이면 show=true, type=FULL")
        void should_returnFull_when_countIsThirty() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(30L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isTrue();
            assertThat(modal.type()).isEqualTo(ReportModalType.FULL);
        }

        @Test
        @DisplayName("11건이면 show=false, type=null")
        void should_returnNone_when_countIsEleven() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(11L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isFalse();
            assertThat(modal.type()).isNull();
        }

        @Test
        @DisplayName("19건이면 show=false, type=null")
        void should_returnNone_when_countIsNineteen() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(19L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isFalse();
            assertThat(modal.type()).isNull();
        }

        @Test
        @DisplayName("0건이면 show=false, type=null")
        void should_returnNone_when_countIsZero() {
            given(starRecordRepositoryPort.countTaggedByUserId(USER_ID)).willReturn(0L);
            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();
            assertThat(modal.show()).isFalse();
            assertThat(modal.type()).isNull();
        }
    }
}
