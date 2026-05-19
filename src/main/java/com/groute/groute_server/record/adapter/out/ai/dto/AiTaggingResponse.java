package com.groute.groute_server.record.adapter.out.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI POST /api/tagging 응답 body. */
public record AiTaggingResponse(
        @JsonProperty("primaryCategory") String primaryCategory,
        @JsonProperty("detailTags") List<String> detailTags) {}
