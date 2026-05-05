package com.groute.groute_server.user.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 설정 저장 요청(MYP-004 PATCH).
 *
 * <p>전체 교체 방식. 서비스 계층은 기존 슬롯을 일괄 삭제 후 {@code settings}로 재삽입한다. 새로 저장되는 슬롯들의 {@code is_active} 컬럼은 본
 * DTO의 {@code isActive} 플래그로 일괄 결정된다.
 *
 * <p>{@code isActive=false}이면 슬롯은 보존되지만 모두 {@code is_active=false}로 마킹돼 발송 대상에서 제외된다(이슈 본문 "슬롯 유지"
 * 정책). 빈 {@code settings} 리스트도 허용 — 이 경우 모든 슬롯 삭제와 동일한 효과.
 *
 * <p>같은 요일 내 시간 중복 검증(MYP-004 "동일 요일 내 중복 시간 설정 불가")은 서비스 계층에서 수행한다.
 */
public record NotificationSettingsUpdateRequest(
        @NotNull(message = "알림 활성화 여부는 필수입니다.") @Schema(description = "알림 활성화 여부", example = "true")
                Boolean isActive,
        @NotNull(message = "알림 슬롯 목록은 필수입니다 (빈 배열 허용).")
                @Valid
                @Schema(description = "알림 슬롯 목록 (요일+시간). 빈 배열은 모든 슬롯 삭제와 동일.")
                List<NotificationSettingItem> settings) {}
