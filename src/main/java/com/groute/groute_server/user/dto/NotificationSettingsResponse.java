package com.groute.groute_server.user.dto;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.enums.DayOfWeek;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 설정 조회 응답(MYP-004 GET).
 *
 * <p>한 유저의 모든 슬롯은 동일한 {@code notifyTime}을 갖는다(요일 다중 선택 + 공통 시간 1개). 따라서 응답은 시간을 단일 필드로 노출하고 요일은 배열로
 * 묶는다.
 *
 * <p>{@code isActive}는 활성 슬롯이 1개 이상이면 {@code true}로 도출한다. 슬롯이 비어 있으면 {@code daysOfWeek=[],
 * notifyTime=null, isActive=false}.
 */
public record NotificationSettingsResponse(
        @Schema(description = "알림 활성화 여부 (활성 슬롯이 1개 이상이면 true)", example = "true") boolean isActive,
        @Schema(description = "알림 요일 목록 (자연 순서: MON→SUN)", example = "[\"MON\",\"TUE\"]")
                List<DayOfWeek> daysOfWeek,
        @JsonFormat(pattern = "HH:mm")
                @Schema(
                        description = "알림 시각 (HH:mm). 슬롯이 없으면 null.",
                        example = "09:00",
                        nullable = true,
                        type = "string")
                LocalTime notifyTime) {

    /**
     * 엔티티 리스트로부터 응답 DTO를 생성하는 정적 팩토리.
     *
     * <p>리포지토리는 day_of_week ENUM STRING 컬럼이라 알파벳순으로 반환하므로, 응답 단계에서 enum 자연 순서(MON→SUN)로 재정렬한다. 모든
     * 슬롯이 동일한 {@code notifyTime}을 갖는다는 invariant 하에 첫 항목의 시간을 대표값으로 사용한다.
     */
    public static NotificationSettingsResponse from(List<NotificationSetting> entities) {
        boolean active = entities.stream().anyMatch(NotificationSetting::isActive);
        List<DayOfWeek> days =
                entities.stream()
                        .map(NotificationSetting::getDayOfWeek)
                        .distinct()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .toList();
        LocalTime time =
                entities.stream().map(NotificationSetting::getNotifyTime).findFirst().orElse(null);
        return new NotificationSettingsResponse(active, days, time);
    }
}
