package com.groute.groute_server.user.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.groute.groute_server.user.dto.validator.HalfHourSlot;
import com.groute.groute_server.user.enums.DayOfWeek;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 슬롯 단위 DTO(MYP-004). 요청·응답 공용으로 사용한다.
 *
 * <p>{@code notifyTime}은 {@code HH:mm} 문자열로 직렬화/역직렬화하며, 그리드/범위 검증은 {@link HalfHourSlot}이 담당한다.
 */
public record NotificationSettingItem(
        @NotNull(message = "요일은 필수입니다.") @Schema(description = "알림 요일", example = "MON")
                DayOfWeek dayOfWeek,
        @NotNull(message = "알림 시각은 필수입니다.")
                @HalfHourSlot
                @JsonFormat(pattern = "HH:mm")
                @Schema(
                        description = "알림 시각 (HH:mm, 00:00 또는 07:00~23:30 30분 단위)",
                        example = "09:00",
                        type = "string")
                LocalTime notifyTime) {}
