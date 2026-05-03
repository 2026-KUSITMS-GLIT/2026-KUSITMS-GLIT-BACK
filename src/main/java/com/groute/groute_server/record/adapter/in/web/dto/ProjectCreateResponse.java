package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.domain.Project;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로젝트 태그 생성 응답 DTO. */
public record ProjectCreateResponse(
        @Schema(description = "생성된 프로젝트 식별자", example = "157") Long projectId,
        @Schema(description = "생성된 프로젝트 이름", example = "2026 U-KATHON") String name) {

    public static ProjectCreateResponse from(Project project) {
        return new ProjectCreateResponse(project.getId(), project.getName());
    }
}
