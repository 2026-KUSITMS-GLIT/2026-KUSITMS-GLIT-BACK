package com.groute.groute_server.user.dto;

import java.util.Comparator;
import java.util.List;

import com.groute.groute_server.user.entity.NotificationSetting;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 설정 조회 응답(MYP-004 GET).
 *
 * <p>최상위 {@code isActive}는 활성 슬롯이 1개 이상이면 {@code true}로 도출한다(슬롯 존재 ⇔ 알림 활성, 기획). 향후 설정 마스터 토글 컬럼이
 * 도입되면 도출 로직만 교체하면 되도록 응답 스키마는 그대로 유지한다.
 */
public record NotificationSettingsResponse(
        @Schema(description = "알림 활성화 여부 (활성 슬롯이 1개 이상이면 true)", example = "true") boolean isActive,
        @Schema(description = "알림 슬롯 목록 (요일 자연 순서: MON→SUN, 같은 요일 내 시간 오름차순)")
                List<NotificationSettingItem> settings) {

    /**
     * 엔티티 리스트로부터 응답 DTO를 생성하는 정적 팩토리.
     *
     * <p>리포지토리는 day_of_week ENUM STRING 컬럼이라 알파벳순으로 반환하므로, 응답 단계에서 enum 자연 순서(MON→SUN)로 재정렬한다. 같은
     * 요일 내 시간은 오름차순.
     */
    public static NotificationSettingsResponse from(List<NotificationSetting> entities) {
        boolean active = entities.stream().anyMatch(NotificationSetting::isActive);
        List<NotificationSettingItem> items =
                entities.stream()
                        .sorted(
                                Comparator.comparingInt(
                                                (NotificationSetting s) ->
                                                        s.getDayOfWeek().ordinal())
                                        .thenComparing(NotificationSetting::getNotifyTime))
                        .map(s -> new NotificationSettingItem(s.getDayOfWeek(), s.getNotifyTime()))
                        .toList();
        return new NotificationSettingsResponse(active, items);
    }
}
