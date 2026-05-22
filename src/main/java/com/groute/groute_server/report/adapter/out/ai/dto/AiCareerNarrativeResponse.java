package com.groute.groute_server.report.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/career/narrative 응답. */
public record AiCareerNarrativeResponse(
        @JsonProperty("narrativeSummary") String narrativeSummary) {}
