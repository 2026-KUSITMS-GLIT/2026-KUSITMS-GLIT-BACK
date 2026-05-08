package com.groute.groute_server.user.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.common.util.DateTimeFormatters;
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
 * 마이페이지 내 정보 조회(MYP001) / 프로필 수정(MYP002) / 회원 탈퇴(MYP005) 엔드포인트.
 *
 * <p>모두 로그인 사용자 본인 리소스만 다루므로 {@link CurrentUser}로 userId를 주입받는다. 응답의 {@code profileImage}는 현재 모든 유저
 * 공통 기본 이미지로 고정되어 {@link UserProperties#defaultProfileImageUrl()}에서 주입된다.
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
        LocalDate kstToday = LocalDate.now(DateTimeFormatters.ZONE_KST);
        return ApiResponse.ok(
                ProfileResponse.from(
                        user,
                        userProperties.defaultProfileImageUrl(),
                        user.streakSnapshotAsOf(kstToday)));
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
        LocalDate kstToday = LocalDate.now(DateTimeFormatters.ZONE_KST);
        return ApiResponse.ok(
                ProfileResponse.from(
                        user,
                        userProperties.defaultProfileImageUrl(),
                        user.streakSnapshotAsOf(kstToday)));
    }

    @Operation(
            summary = "회원 탈퇴",
            description =
                    "본인 계정을 즉시 soft delete하고 30일 뒤 물리 삭제될 시각을 예약한다. 보유 refresh token은 즉시 무효화되며,"
                            + " 액세스 토큰은 stateless 정책상 만료까지 유효(클라이언트가 보관 토큰을 폐기하는 책임).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "탈퇴 처리 성공 — 이미 탈퇴된 사용자 재호출도 멱등 200(grace 기간 미연장)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount(@CurrentUser Long userId) {
        userService.deleteMyAccount(userId);
        return ApiResponse.ok("탈퇴 처리 성공");
    }
}
