package com.groute.groute_server.user.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.user.dto.NotificationSettingsResponse;
import com.groute.groute_server.user.dto.NotificationSettingsUpdateRequest;
import com.groute.groute_server.user.service.NotificationSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 알림 설정 조회/저장 엔드포인트(MYP-004).
 *
 * <p>GET·PATCH 모두 본인 리소스만 다루므로 {@link CurrentUser}로 userId를 주입받는다. PATCH는 전체 교체 방식이며 검증·중복 거부는
 * {@link NotificationSettingService}가 담당한다. 저장 후 응답에는 변경된 최종 상태를 함께 반환해 클라가 별도 GET 없이도 화면을 동기화할 수
 * 있다.
 */
@Tag(name = "Notification", description = "알림 설정 조회·저장")
@RestController
@RequestMapping("/api/users/me/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "내 알림 설정 조회", description = "로그인 사용자 본인의 알림 슬롯 목록을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping
    public ApiResponse<NotificationSettingsResponse> getMySettings(@CurrentUser Long userId) {
        return ApiResponse.ok("알림 설정 조회 성공", notificationSettingService.getMySettings(userId));
    }

    @Operation(
            summary = "내 알림 설정 저장",
            description = "기존 슬롯을 모두 삭제하고 요청의 슬롯들로 교체한다. 빈 settings는 모든 슬롯 삭제와 동일.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필수 필드 누락 / 시간 그리드 위반 / 동일 요일 내 중복 시간"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @PatchMapping
    public ApiResponse<Void> updateMySettings(
            @CurrentUser Long userId,
            @Valid @RequestBody NotificationSettingsUpdateRequest request) {
        notificationSettingService.replaceMySettings(userId, request);
        return ApiResponse.ok("알림 설정 저장 성공");
    }
}
