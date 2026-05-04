package com.groute.groute_server.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.auth.dto.TokenReissueRequest;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.service.AuthService;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 엔드포인트.
 *
 * <p>재발급({@code POST /api/auth/reissue}): 쿠키에 실려온 refresh를 우선 사용하고, 쿠키가 없으면 요청 본문에서 폴백 조회. 전달 방식은
 * {@link TokenDeliveryService}가 통일해 처리.
 *
 * <p>로그아웃({@code POST /api/auth/logout}, MYP004): 인증된 사용자의 refresh 토큰을 Redis에서 삭제하고, 쿠키 프로필이면 브라우저
 * refresh 쿠키도 만료시킨다. Service는 Redis 상태만 책임지고 쿠키 정리는 Controller에서 {@link
 * TokenDeliveryService#clear}로 직접 위임.
 */
@Tag(name = "Auth", description = "인증 토큰 재발급·로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenDeliveryService tokenDeliveryService;

    @Operation(
            summary = "액세스 토큰 재발급",
            description = "유효한 리프레시 토큰으로 새 access 토큰을 발급받는다. 리프레시 토큰은 rotate.")
    @SecurityRequirement(name = "")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "재발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "리프레시 토큰 누락 또는 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 리프레시 토큰")
    })
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshCookie,
            @Valid @RequestBody(required = false) TokenReissueRequest request,
            HttpServletResponse response) {
        String refreshToken = pickRefreshToken(refreshCookie, request);
        TokenResponse tokens = authService.reissue(refreshToken);
        return ApiResponse.ok(
                tokenDeliveryService.deliver(
                        response, tokens.accessToken(), tokens.refreshToken()));
    }

    private String pickRefreshToken(String cookieValue, TokenReissueRequest request) {
        if (cookieValue != null && !cookieValue.isBlank()) {
            return cookieValue;
        }
        if (request != null) {
            return request.refreshToken();
        }
        return null;
    }

    @Operation(
            summary = "로그아웃",
            description = "인증된 사용자의 refresh 토큰을 Redis에서 삭제하고, 쿠키 프로필이면 브라우저 refresh 쿠키도 만료시킨다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser Long userId, HttpServletResponse response) {
        authService.logout(userId);
        tokenDeliveryService.clear(response);
        return ApiResponse.ok("로그아웃 성공");
    }
}
