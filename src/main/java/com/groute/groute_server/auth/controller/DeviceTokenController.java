package com.groute.groute_server.auth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.auth.dto.DeviceTokenRegisterRequest;
import com.groute.groute_server.auth.service.DeviceTokenService;
import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 디바이스 푸시 토큰 등록 엔드포인트(MYP-004).
 *
 * <p>PWA의 service worker가 FCM에서 받은 토큰을 서버에 전달하면, 서버는 user_id ↔ push_token 매핑을 유지해 알림 발송 시 토큰을
 * 역조회한다. 같은 토큰의 재호출({@code onTokenRefresh} 등)은 갱신/활성화로 처리된다.
 */
@Tag(name = "DeviceToken", description = "FCM/APNs 디바이스 푸시 토큰 등록")
@RestController
@RequestMapping("/api/users/me/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(
            summary = "디바이스 푸시 토큰 등록",
            description =
                    "PWA에서 발급받은 FCM 디바이스 토큰을 서버에 등록한다. 같은 토큰이 이미 있으면 소유자·플랫폼·활성 여부가 갱신된다(upsert).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필수 필드 누락 또는 지원하지 않는 플랫폼"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @PostMapping
    public ApiResponse<Void> register(
            @CurrentUser Long userId, @Valid @RequestBody DeviceTokenRegisterRequest request) {
        deviceTokenService.register(userId, request);
        return ApiResponse.ok("디바이스 토큰 등록 성공");
    }
}
