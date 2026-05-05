package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.ProjectPage;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로젝트 태그 목록 조회 응답 DTO. */
public record ProjectsResponse(
        @Schema(description = "현재 페이지", example = "0") int page,
        @Schema(description = "페이지 사이즈", example = "5") int size,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages,
        @Schema(description = "프로젝트 목록") List<ProjectSummary> projects) {

    public static ProjectsResponse from(ProjectPage projectPage) {
        return new ProjectsResponse(
                projectPage.page(),
                projectPage.size(),
                projectPage.totalPages(),
                projectPage.content().stream().map(ProjectSummary::from).toList());
    }

    /** 목록 내 개별 프로젝트 항목. */
    public record ProjectSummary(
            @Schema(description = "프로젝트 식별자", example = "153") Long projectId,
            @Schema(description = "프로젝트 이름", example = "졸업프로젝트") String name,
            @Schema(description = "삭제 가능 여부 (연결된 기록 없는 경우 true)", example = "true")
                    boolean deletable) {

        public static ProjectSummary from(Project project) {
            return new ProjectSummary(
                    project.getId(), project.getName(), project.getTitleCount() == 0);
        }
    }
}
