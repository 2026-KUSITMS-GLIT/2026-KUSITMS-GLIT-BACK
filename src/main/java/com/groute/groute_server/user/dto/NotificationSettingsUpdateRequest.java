package com.groute.groute_server.user.dto;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.groute.groute_server.user.dto.validator.HalfHourSlot;
import com.groute.groute_server.user.enums.DayOfWeek;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 설정 저장 요청(MYP-004 PATCH).
 *
 * <p>전체 교체 방식. 서비스 계층은 기존 슬롯을 일괄 삭제 후 {@code daysOfWeek} × {@code notifyTime} 조합으로 재삽입한다(요일 N개면 row
 * N개, 모두 같은 시각). 새로 저장되는 슬롯들의 {@code is_active} 컬럼은 본 DTO의 {@code isActive} 플래그로 일괄 결정된다.
 *
 * <p>{@code isActive=false}이면 슬롯은 보존되지만 모두 {@code is_active=false}로 마킹돼 발송 대상에서 제외된다(이슈 본문 "슬롯 유지"
 * 정책). 빈 {@code daysOfWeek} 리스트는 {@code isActive=false}와 함께 보낼 경우 모든 슬롯 삭제와 동일한 효과이며, {@code
 * isActive=true}와 함께 보내면 의미적 모순으로 거부된다(USER_005).
 *
 * <p>요일 중복 검증(기획 E — 동일 요일은 한 번만 선택 가능)은 서비스 계층에서 수행한다.
 */
public record NotificationSettingsUpdateRequest(
        @NotNull(message = "알림 활성화 여부는 필수입니다.") @Schema(description = "알림 활성화 여부", example = "true")
                Boolean isActive,
        @NotNull(message = "요일 목록은 필수입니다 (빈 배열 허용).")
                @Schema(
                        description = "알림 요일 목록. 빈 배열은 isActive=false와 함께일 때만 허용 (모든 슬롯 삭제).",
                        example = "[\"MON\",\"TUE\"]")
                List<DayOfWeek> daysOfWeek,
        @NotNull(message = "알림 시각은 필수입니다.")
                @HalfHourSlot
                @JsonFormat(pattern = "HH:mm")
                @Schema(
                        description = "알림 시각 (HH:mm, 00:00 또는 07:00~23:30 30분 단위)",
                        example = "09:00",
                        type = "string")
                LocalTime notifyTime) {}
