package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/mini 응답. */
public record AiMiniReportResponse(
        @JsonProperty("activitySummary") String activitySummary,
        @JsonProperty("nextFocusPoint") String nextFocusPoint,
        @JsonProperty("competencyFrequency") List<CompetencyCount> competencyFrequency,
        @JsonProperty("topDetailTags") List<String> topDetailTags) {

    public record CompetencyCount(String competency, int count) {}
}
