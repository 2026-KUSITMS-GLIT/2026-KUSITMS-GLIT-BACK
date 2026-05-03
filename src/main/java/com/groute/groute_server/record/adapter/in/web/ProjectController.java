package com.groute.groute_server.record.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.ProjectCreateResponse;
import com.groute.groute_server.record.adapter.in.web.dto.ProjectNameRequest;
import com.groute.groute_server.record.adapter.in.web.dto.ProjectsResponse;
import com.groute.groute_server.record.application.port.in.ProjectUseCase;
import com.groute.groute_server.record.domain.Project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 프로젝트 태그 CRUD 엔드포인트. */
@Tag(name = "Project", description = "프로젝트 태그 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    @Operation(summary = "프로젝트 태그 생성", description = "새 프로젝트 태그를 생성한다. 동일 이름이 존재하면 409를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400", description = "유효하지 않은 입력값"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409", description = "이미 존재하는 태그 이름")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectCreateResponse> createProject(
            @CurrentUser Long userId, @Valid @RequestBody ProjectNameRequest request) {
        Project project = projectUseCase.createProject(userId, request.name());
        return ApiResponse.created("프로젝트 태그가 생성되었습니다.", ProjectCreateResponse.from(project));
    }

    @Operation(summary = "프로젝트 태그 목록 조회", description = "생성 시간 내림차순으로 페이지 단위 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공")
    @GetMapping
    public ApiResponse<ProjectsResponse> getProjects(
            @CurrentUser Long userId, @PageableDefault(size = 5, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<Project> projects = projectUseCase.getProjects(userId, pageable);
        return ApiResponse.ok("프로젝트 태그 목록을 조회했습니다.", ProjectsResponse.from(projects));
    }

    @Operation(summary = "프로젝트 태그 이름 수정", description = "태그 이름을 수정한다. 동일 이름이 존재하면 409를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400", description = "유효하지 않은 입력값"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", description = "프로젝트 태그를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409", description = "이미 존재하는 태그 이름")
    })
    @PatchMapping("/{projectId}")
    public ApiResponse<Void> updateProject(
            @CurrentUser Long userId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectNameRequest request) {
        projectUseCase.updateProject(userId, projectId, request.name());
        return ApiResponse.ok("프로젝트 태그 이름이 수정되었습니다.");
    }

    @Operation(summary = "프로젝트 태그 삭제", description = "연결된 기록이 없는 경우에만 삭제 가능하다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", description = "프로젝트 태그를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409", description = "연결된 기록이 있어 삭제 불가")
    })
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @CurrentUser Long userId, @PathVariable Long projectId) {
        projectUseCase.deleteProject(userId, projectId);
        return ApiResponse.ok("프로젝트 태그가 삭제되었습니다.");
    }
}