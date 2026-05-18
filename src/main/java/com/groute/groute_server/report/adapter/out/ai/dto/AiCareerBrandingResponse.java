package com.groute.groute_server.report.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI /api/reports/career/branding 응답. */
public record AiCareerBrandingResponse(
        @JsonProperty("branding_statement") String brandingStatement,
        @JsonProperty("branding_pattern") String brandingPattern,
        @JsonProperty("top_detail_tags") List<DetailTagStat> topDetailTags) {

    public record DetailTagStat(
            String tag,
            int count) {}
}
