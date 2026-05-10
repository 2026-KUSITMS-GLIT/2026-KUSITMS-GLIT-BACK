package com.groute.groute_server.user.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.user.config.UserProperties;
import com.groute.groute_server.user.dto.OnboardCompleteRequest;
import com.groute.groute_server.user.dto.ProfileResponse;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 온보딩 완료(ONB003~005) 엔드포인트. */
@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final UserService userService;
    private final UserProperties userProperties;

    @Operation(
            summary = "온보딩 완료",
            description = "닉네임·직군·현재 상태를 일괄 저장하고 프로필을 반환한다. 이미 온보딩이 완료된 경우 409를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "온보딩 완료 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력값"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이미 온보딩이 완료된 사용자")
    })
    @PostMapping("/complete")
    public ApiResponse<ProfileResponse> completeOnboarding(
            @CurrentUser Long userId, @Valid @RequestBody OnboardCompleteRequest request) {
        User user =
                userService.completeOnboarding(
                        userId, request.nickname(), request.jobRole(), request.userStatus());
        LocalDate kstToday = LocalDate.now(ZoneId.systemDefault());
        return ApiResponse.ok(
                "온보딩이 완료되었습니다.",
                ProfileResponse.from(
                        user,
                        userProperties.defaultProfileImageUrl(),
                        user.streakSnapshotAsOf(kstToday)));
    }
}
