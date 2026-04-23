package com.groute.groute_server.user.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.user.config.UserProperties;
import com.groute.groute_server.user.dto.ProfileResponse;
import com.groute.groute_server.user.dto.ProfileUpdateRequest;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 내 정보 조회(MYP001) / 프로필 수정(MYP002) 엔드포인트.
 *
 * <p>둘 다 로그인 사용자 본인 리소스만 다루므로 {@link CurrentUser}로 userId를 주입받는다. 응답의 {@code profileImage}는 현재 모든
 * 유저 공통 기본 이미지로 고정되어 {@link UserProperties#defaultProfileImageUrl()}에서 주입된다.
 */
@Tag(name = "User", description = "마이페이지 내 정보 조회/수정")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProperties userProperties;

    @Operation(summary = "내 정보 조회", description = "로그인 사용자 본인의 프로필 정보를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(@CurrentUser Long userId) {
        User user = userService.getMyProfile(userId);
        return ApiResponse.ok(ProfileResponse.from(user, userProperties.defaultProfileImageUrl()));
    }

    @Operation(summary = "프로필 수정", description = "직군·상태를 덮어쓴다. 변경 사항이 없어도 두 필드 모두 한글 라벨로 포함해 요청한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공 — 업데이트된 프로필 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필수 필드 누락 또는 지원하지 않는 라벨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/me")
    public ApiResponse<ProfileResponse> updateMyProfile(
            @CurrentUser Long userId, @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userService.updateMyProfile(userId, request.jobRole(), request.userStatus());
        return ApiResponse.ok(ProfileResponse.from(user, userProperties.defaultProfileImageUrl()));
    }
}
