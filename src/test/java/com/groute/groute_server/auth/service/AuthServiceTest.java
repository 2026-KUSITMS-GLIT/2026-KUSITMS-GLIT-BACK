package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.jwt.JwtTokenProvider;
import com.groute.groute_server.common.jwt.JwtValidationResult;
import com.groute.groute_server.common.jwt.TokenType;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private AuthService authService;

    // ── 1. 토큰 누락 케이스 ─────────────────────────────────────────
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @ParameterizedTest
    void reissue_nullOrBlankToken_throwsRefreshTokenRequired(String token) {
        assertThatThrownBy(() -> authService.reissue(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
    }

    // ── 2. JWT 검증 실패 케이스 ─────────────────────────────────────
    @Test
    void reissue_expiredToken_throwsInvalidRefreshToken() {
        given(jwtTokenProvider.validate("expired-token")).willReturn(JwtValidationResult.EXPIRED);

        assertThatThrownBy(() -> authService.reissue("expired-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void reissue_invalidToken_throwsInvalidRefreshToken() {
        given(jwtTokenProvider.validate("invalid-token")).willReturn(JwtValidationResult.INVALID);

        assertThatThrownBy(() -> authService.reissue("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // ── 3. 토큰 타입 불일치 케이스 ──────────────────────────────────
    @Test
    void reissue_accessTokenPassedAsRefresh_throwsInvalidRefreshToken() {
        String token = "access-token";
        given(jwtTokenProvider.validate(token)).willReturn(JwtValidationResult.VALID);
        given(jwtTokenProvider.getTokenType(token)).willReturn(TokenType.ACCESS);

        assertThatThrownBy(() -> authService.reissue(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // ── 4. Redis rotate 실패 케이스 ──────────────────────────────────
    @Test
    void reissue_rotateFailure_deletesTokenAndThrowsInvalidRefreshToken() {
        String oldToken = "old-refresh-token";
        String newAccess = "new-access-token";
        String newRefresh = "new-refresh-token";
        Long userId = 42L;

        given(jwtTokenProvider.validate(oldToken)).willReturn(JwtValidationResult.VALID);
        given(jwtTokenProvider.getTokenType(oldToken)).willReturn(TokenType.REFRESH);
        given(jwtTokenProvider.getUserId(oldToken)).willReturn(userId);
        given(jwtTokenProvider.createAccessToken(userId)).willReturn(newAccess);
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn(newRefresh);
        given(refreshTokenRepository.rotate(userId, oldToken, newRefresh)).willReturn(false);

        assertThatThrownBy(() -> authService.reissue(oldToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        then(refreshTokenRepository).should().deleteByUserId(userId);
    }

    // ── 5. 정상 재발급 케이스 ───────────────────────────────────────
    @Test
    void reissue_validToken_returnsNewTokens() {
        String oldToken = "valid-refresh-token";
        String newAccess = "new-access-token";
        String newRefresh = "new-refresh-token";
        Long userId = 1L;

        given(jwtTokenProvider.validate(oldToken)).willReturn(JwtValidationResult.VALID);
        given(jwtTokenProvider.getTokenType(oldToken)).willReturn(TokenType.REFRESH);
        given(jwtTokenProvider.getUserId(oldToken)).willReturn(userId);
        given(jwtTokenProvider.createAccessToken(userId)).willReturn(newAccess);
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn(newRefresh);
        given(refreshTokenRepository.rotate(userId, oldToken, newRefresh)).willReturn(true);

        TokenResponse result = authService.reissue(oldToken);

        assertThat(result.accessToken()).isEqualTo(newAccess);
        assertThat(result.refreshToken()).isEqualTo(newRefresh);
        then(refreshTokenRepository).should(never()).deleteByUserId(anyLong());
    }

    // ── 6. 경계값: rotate 성공 후 deleteByUserId 호출 없음 ───────────
    @Test
    void reissue_success_doesNotDeleteToken() {
        String oldToken = "refresh-token";
        Long userId = 99L;

        given(jwtTokenProvider.validate(oldToken)).willReturn(JwtValidationResult.VALID);
        given(jwtTokenProvider.getTokenType(oldToken)).willReturn(TokenType.REFRESH);
        given(jwtTokenProvider.getUserId(oldToken)).willReturn(userId);
        given(jwtTokenProvider.createAccessToken(userId)).willReturn("access");
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn("new-refresh");
        given(refreshTokenRepository.rotate(eq(userId), eq(oldToken), anyString()))
                .willReturn(true);

        authService.reissue(oldToken);

        then(refreshTokenRepository).should(never()).deleteByUserId(anyLong());
    }

    // ── 7. 회귀: rotate 실패 시 save() 는 호출되지 않음 ──────────────
    @Test
    void reissue_rotateFailure_doesNotSaveNewToken() {
        String oldToken = "old-token";
        Long userId = 7L;

        given(jwtTokenProvider.validate(oldToken)).willReturn(JwtValidationResult.VALID);
        given(jwtTokenProvider.getTokenType(oldToken)).willReturn(TokenType.REFRESH);
        given(jwtTokenProvider.getUserId(oldToken)).willReturn(userId);
        given(jwtTokenProvider.createAccessToken(userId)).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn("new-refresh");
        given(refreshTokenRepository.rotate(anyLong(), anyString(), anyString()))
                .willReturn(false);

        assertThatThrownBy(() -> authService.reissue(oldToken))
                .isInstanceOf(BusinessException.class);

        then(refreshTokenRepository).should(never()).save(anyLong(), anyString());
    }
}