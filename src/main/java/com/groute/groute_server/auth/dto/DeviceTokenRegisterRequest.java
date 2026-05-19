package com.groute.groute_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.groute.groute_server.auth.enums.DevicePlatform;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 디바이스 푸시 토큰 등록 요청(MYP-004).
 *
 * <p>PWA에서 service worker 등록 후 Firebase SDK {@code getToken} 으로 발급받은 토큰을 서버에 전달한다. 같은 {@code
 * pushToken}이 이미 존재하면 소유자/플랫폼/활성 여부가 갱신되며, 없으면 신규 insert(upsert 의미).
 */
public record DeviceTokenRegisterRequest(
        @NotNull(message = "디바이스 플랫폼은 필수입니다.") @Schema(description = "디바이스 플랫폼", example = "WEB")
                DevicePlatform platform,
        @NotBlank(message = "푸시 토큰은 필수입니다.")
                @Schema(description = "FCM/APNs 디바이스 토큰", example = "fcm-token-xyz...")
                String pushToken) {}
