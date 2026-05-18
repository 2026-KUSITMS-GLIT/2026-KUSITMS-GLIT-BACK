package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/* 공통 요청 body. */
public record AiReportRequest(
        String nickname,
        String job,
        String status,
        List<StarRecordItem> records,
        @JsonProperty("scrums_by_date") List<ScrumsByDate> scrumsByDate,
        @JsonProperty("record_period") RecordPeriod recordPeriod,
        @JsonProperty("total_count") int totalCount) {

    public record StarRecordItem(
            @JsonProperty("star_record_id") Long starRecordId,
            @JsonProperty("situation_task") String situationTask,
            String action,
            String result,
            @JsonProperty("completed_at") String completedAt,
            String competency,
            @JsonProperty("detail_tags") List<String> detailTags) {}

    public record ScrumsByDate(
            String date,
            List<ScrumItem> scrums) {

        public record ScrumItem(
                @JsonProperty("project_name") String projectName,
                @JsonProperty("scrum_title") String scrumTitle,
                String content,
                @JsonProperty("star_record_id") Long starRecordId) {}
    }

    public record RecordPeriod(
            @JsonProperty("from") String from,
            String to) {}
}
