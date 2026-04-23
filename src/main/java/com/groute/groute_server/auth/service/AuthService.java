package com.groute.groute_server.auth.service;

import org.springframework.stereotype.Service;

import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.jwt.JwtTokenProvider;
import com.groute.groute_server.common.jwt.JwtValidationResult;
import com.groute.groute_server.common.jwt.TokenType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 비즈니스 로직 — 현재는 리프레시 토큰으로 access를 재발급(ONB001)하는 플로우만 담당.
 *
 * <p>검증·매칭 실패는 전부 {@link ErrorCode#INVALID_REFRESH_TOKEN} (401)로 일원화. 토큰 자체 누락은 그보다 앞 단계에서 {@link
 * ErrorCode#REFRESH_TOKEN_REQUIRED} (400)로 분리해, 클라이언트 버그(누락)와 실제 인증 실패를 구분.
 *
 * <p>성공 시 refresh 토큰을 rotate — {@link RefreshTokenRepository#rotate(Long, String, String)} 가 Lua
 * 스크립트로 "이전 값 일치 확인 + 새 값 저장"을 원자 실행한다. 동시 요청이 들어와도 한 건만 성공하고 나머지는 401로 거절되어 일회성 회전 보장이 유지된다.
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
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        if (!refreshTokenRepository.rotate(userId, refreshToken, newRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        log.debug("리프레시 성공: userId={}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
