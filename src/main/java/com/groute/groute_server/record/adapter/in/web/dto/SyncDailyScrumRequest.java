package com.groute.groute_server.record.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumCommand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 일자별 스크럼 일괄 sync 요청 DTO.
 *
 * <p>요청 payload만 살아남는 sync 시맨틱. 일자당 5개 제한·14일·STAR·소유권 검증은 서비스에서 수행한다.
 */
@Schema(description = "일자별 스크럼 sync 요청")
public record SyncDailyScrumRequest(
        @NotNull @Valid @Schema(description = "ScrumTitle 단위 그룹 목록") List<GroupRequest> groups) {

    public SyncDailyScrumCommand toCommand(Long userId, LocalDate date) {
        return new SyncDailyScrumCommand(
                userId, date, groups.stream().map(GroupRequest::toCommand).toList());
    }

    @Schema(description = "ScrumTitle 그룹")
    public record GroupRequest(
            @NotNull @Schema(description = "기존 스크럼 제목 ID", example = "12") Long titleId,
            @NotNull @Valid @Schema(description = "그룹 내 항목 목록") List<ItemRequest> items) {

        SyncDailyScrumCommand.GroupCommand toCommand() {
            return new SyncDailyScrumCommand.GroupCommand(
                    titleId, items.stream().map(ItemRequest::toCommand).toList());
        }
    }

    @Schema(description = "스크럼 항목")
    public record ItemRequest(
            @Schema(description = "스크럼 ID (null이면 신규 생성)", example = "37", nullable = true)
                    Long scrumId,
            @NotBlank
                    @Size(max = 50, message = "본문은 최대 50자입니다.")
                    @Schema(description = "본문 (최대 50자)", example = "와이어프레임 설계")
                    String content) {

        SyncDailyScrumCommand.ItemCommand toCommand() {
            return new SyncDailyScrumCommand.ItemCommand(scrumId, content);
        }
    }
}
