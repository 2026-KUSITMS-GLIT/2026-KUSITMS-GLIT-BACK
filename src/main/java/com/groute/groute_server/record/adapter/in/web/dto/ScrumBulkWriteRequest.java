package com.groute.groute_server.record.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumCommand;

import io.swagger.v3.oas.annotations.media.Schema;

/** 스크럼 일괄 저장 요청 DTO (POST /api/scrums/write). */
@Schema(description = "스크럼 일괄 저장 요청")
public record ScrumBulkWriteRequest(
        @NotBlank @Schema(description = "기준일 (yyyy-MM-dd)", example = "2024-04-10") String date,
        @NotNull @NotEmpty @Valid @Schema(description = "제목 단위 스크럼 묶음")
                List<ScrumByTitleRequest> scrumsByTitle) {

    public BulkWriteScrumCommand toCommand(Long userId, LocalDate date) {
        return new BulkWriteScrumCommand(
                userId, date, scrumsByTitle.stream().map(ScrumByTitleRequest::toCommand).toList());
    }

    @Schema(description = "스크럼 제목 단위 묶음")
    public record ScrumByTitleRequest(
            @NotNull @Schema(description = "프로젝트 식별자", example = "1000") Long projectId,
            @NotBlank
                    @Size(max = 20, message = "자유 입력란은 최대 20자입니다.")
                    @Schema(description = "자유 입력란 (최대 20자)", example = "4/22 기획 작업")
                    String freeText,
            @NotNull @NotEmpty @Valid @Schema(description = "스크럼 목록")
                    List<ScrumContentRequest> scrums) {

        BulkWriteScrumCommand.GroupCommand toCommand() {
            return new BulkWriteScrumCommand.GroupCommand(
                    projectId,
                    freeText,
                    scrums.stream().map(ScrumContentRequest::content).toList());
        }
    }

    @Schema(description = "스크럼 항목")
    public record ScrumContentRequest(
            @NotBlank
                    @Size(max = 50, message = "본문은 최대 50자입니다.")
                    @Schema(description = "스크럼 내용 (최대 50자)", example = "AUTH API")
                    String content) {}
}
