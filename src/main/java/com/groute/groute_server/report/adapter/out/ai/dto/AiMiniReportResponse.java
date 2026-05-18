package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/mini 응답. */
public record AiMiniReportResponse(
        @JsonProperty("activity_summary") String activitySummary,
        @JsonProperty("next_focus_point") String nextFocusPoint,
        @JsonProperty("competency_frequency") List<CompetencyCount> competencyFrequency,
        @JsonProperty("top_detail_tags") List<String> topDetailTags) {

    public record CompetencyCount(
            String competency,
            int count) {}
}
