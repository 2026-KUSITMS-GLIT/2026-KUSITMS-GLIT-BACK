package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/* 공통 요청 body. */
public record AiReportRequest(
        String nickname,
        String job,
        String status,
        List<StarRecordItem> records,
        @JsonProperty("scrumsByDate") List<ScrumsByDate> scrumsByDate,
        @JsonProperty("recordPeriod") RecordPeriod recordPeriod,
        @JsonProperty("totalCount") int totalCount) {

    public record StarRecordItem(
            @JsonProperty("starRecordId") Long starRecordId,
            @JsonProperty("situationTask") String situationTask,
            String action,
            String result,
            @JsonProperty("completedAt") String completedAt,
            String competency,
            @JsonProperty("detailTags") List<String> detailTags) {}

    public record ScrumsByDate(String date, List<ScrumItem> scrums) {

        public record ScrumItem(
                @JsonProperty("projectName") String projectName,
                @JsonProperty("scrumTitle") String scrumTitle,
                String content,
                @JsonProperty("starRecordId") Long starRecordId) {}
    }

    public record RecordPeriod(@JsonProperty("from") String from, String to) {}
}
