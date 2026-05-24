package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.report.application.port.in.AutoSelectedStarRecordView;
import com.groute.groute_server.report.application.port.in.SelectableInfoView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 생성용 사전 정보 조회 응답 DTO. {@link SelectableInfoView}의 Web 표현. */
@Schema(description = "리포트 생성용 사전 정보 조회 응답")
public record ReportSelectableInfoResponse(
        @Schema(description = "서버가 결정한 리포트 타입", example = "MINI") String reportType,
        @Schema(description = "유저 전체 완료된 심화기록 수. 달력 하단 카운터 분모", example = "23") int totalStarCount,
        @Schema(
                        description = "달력 화면 진입 시 자동으로 체크될 심화기록 상세 목록 (최신순)",
                        example =
                                "[{\"starRecordId\":1,\"date\":\"2026-04-09\",\"projectName\":\"테스트프로젝트\",\"scrumContent\":\"어제 한 일...\"}]")
                List<AutoSelectedStarRecordView> autoSelectedStarRecords,
        @Schema(
                        description = "완료된 심화기록이 있는 날짜 목록. 달력 하이라이트 렌더링용 (전체 기간)",
                        example = "[\"2026-04-09\"]")
                List<String> starRecordDates) {

    public static ReportSelectableInfoResponse from(SelectableInfoView view) {
        return new ReportSelectableInfoResponse(
                view.reportType(),
                view.totalStarCount(),
                view.autoSelectedStarRecords(),
                view.starRecordDates());
    }
}
