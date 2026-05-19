package com.groute.groute_server.record.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FastAPI POST /api/tagging 요청 body. */
public record AiTaggingRequest(
        @JsonProperty("jobRole") String jobRole,
        @JsonProperty("selectedCompetency") String selectedCompetency,
        @JsonProperty("situationTask") String situationTask,
        String action,
        String result) {}
