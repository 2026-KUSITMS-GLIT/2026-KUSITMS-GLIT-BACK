package com.groute.groute_server.auth.service;

import org.springframework.stereotype.Service;

import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.jwt.JwtTokenProvider;
import com.groute.groute_server.auth.jwt.JwtValidationResult;
import com.groute.groute_server.auth.jwt.TokenType;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 비즈니스 로직 — 현재는 리프레시 토큰으로 access를 재발급(ONB001)하는 플로우만 담당.
 *
 * <p>검증·매칭 실패는 전부 {@link ErrorCode#INVALID_REFRESH_TOKEN} (401)로 일원화. 토큰 자체 누락은 그보다 앞 단계에서 {@link
 * ErrorCode#REFRESH_TOKEN_REQUIRED} (400)로 분리해, 클라이언트 버그(누락)와 실제 인증 실패를 구분.
 *
 * <p>성공 시 refresh 토큰을 rotate — 새 값으로 Redis 덮어쓰기. 탈취된 이전 토큰으로 재시도해도 Redis의 신규 값과 불일치라 실패하므로 일회성 토큰
 * 취약점을 완화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }
        if (jwtTokenProvider.validate(refreshToken) != JwtValidationResult.VALID) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (jwtTokenProvider.getTokenType(refreshToken) != TokenType.REFRESH) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        if (!refreshTokenRepository.matches(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, newRefreshToken);

        log.debug("리프레시 성공: userId={}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
