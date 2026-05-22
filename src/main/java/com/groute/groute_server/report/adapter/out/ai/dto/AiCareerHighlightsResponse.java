package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/career/highlights 응답. */
public record AiCareerHighlightsResponse(
        @JsonProperty("experienceHighlights") List<String> experienceHighlights) {}
