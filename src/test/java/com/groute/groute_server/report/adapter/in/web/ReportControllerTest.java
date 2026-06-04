package com.groute.groute_server.report.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.application.port.in.CreateReportUseCase;
import com.groute.groute_server.report.application.port.in.GetReportDetailUseCase;
import com.groute.groute_server.report.application.port.in.GetReportGaugeUseCase;
import com.groute.groute_server.report.application.port.in.GetReportListUseCase;
import com.groute.groute_server.report.application.port.in.GetReportStatusUseCase;
import com.groute.groute_server.report.application.port.in.GetSelectableInfoUseCase;
import com.groute.groute_server.report.application.port.in.RetryReportUseCase;
import com.groute.groute_server.report.application.port.in.dto.ReportGaugeView;
import com.groute.groute_server.report.application.port.in.dto.ReportListView;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.support.WebMvcTestBase;

@WebMvcTest(ReportController.class)
class ReportControllerTest extends WebMvcTestBase {

    @MockitoBean private GetReportGaugeUseCase getReportGaugeUseCase;
    @MockitoBean private GetReportListUseCase getReportListUseCase;
    @MockitoBean private GetReportDetailUseCase getReportDetailUseCase;
    @MockitoBean private GetSelectableInfoUseCase getSelectableInfoUseCase;
    @MockitoBean private CreateReportUseCase createReportUseCase;
    @MockitoBean private GetReportStatusUseCase getReportStatusUseCase;
    @MockitoBean private RetryReportUseCase retryReportUseCase;

    private static final String BASE_URL = "/api/reports";

    @Nested
    @DisplayName("GET /api/reports/gauge")
    class GetGauge {

        @Test
        @DisplayName("인증된 사용자 요청 시 200 반환")
        void should_return200_when_authenticated() throws Exception {
            given(getReportGaugeUseCase.getGauge(anyLong())).willReturn(ReportGaugeView.of(7, 10));

            mockMvc.perform(get(BASE_URL + "/gauge").with(auth(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentCount").value(7))
                    .andExpect(jsonPath("$.data.nextThreshold").value(10));
        }

        @Test
        @DisplayName("미인증 요청 시 401 반환")
        void should_return401_when_unauthenticated() throws Exception {
            mockMvc.perform(get(BASE_URL + "/gauge")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/reports")
    class GetList {

        @Test
        @DisplayName("인증된 사용자 요청 시 200 반환")
        void should_return200_when_authenticated() throws Exception {
            given(getReportListUseCase.getList(anyLong()))
                    .willReturn(new ReportListView(List.of()));

            mockMvc.perform(get(BASE_URL).with(auth(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reports").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/reports")
    class CreateReport {

        @Test
        @DisplayName("유효한 요청 시 200 반환")
        void should_return200_when_validRequest() throws Exception {
            given(createReportUseCase.createReport(any())).willReturn(42L);

            mockMvc.perform(
                            post(BASE_URL)
                                    .with(auth(1L))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"reportType\":\"MINI\",\"starRecordIds\":[1,2,3]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.reportId").value(42));

            then(createReportUseCase)
                    .should()
                    .createReport(
                            argThat(
                                    (CreateReportCommand cmd) ->
                                            cmd.userId().equals(1L)
                                                    && cmd.reportType() == ReportType.MINI
                                                    && cmd.starRecordIds()
                                                            .equals(List.of(1L, 2L, 3L))));
        }

        @Test
        @DisplayName("reportType 누락 시 400 반환")
        void should_return400_when_reportTypeIsNull() throws Exception {
            mockMvc.perform(
                            post(BASE_URL)
                                    .with(auth(1L))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"starRecordIds\":[1,2,3]}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("starRecordIds 빈 배열 시 400 반환")
        void should_return400_when_starRecordIdsIsEmpty() throws Exception {
            mockMvc.perform(
                            post(BASE_URL)
                                    .with(auth(1L))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"reportType\":\"MINI\",\"starRecordIds\":[]}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
