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
 * 인증 관련 비즈니스 로직 — 리프레시 토큰 재발급(ONB001)과 로그아웃(MYP004)을 담당.
 *
 * <p>검증·매칭 실패는 전부 {@link ErrorCode#INVALID_REFRESH_TOKEN} (401)로 일원화. 토큰 자체 누락은 그보다 앞 단계에서 {@link
 * ErrorCode#REFRESH_TOKEN_REQUIRED} (400)로 분리해, 클라이언트 버그(누락)와 실제 인증 실패를 구분.
 *
 * <p>재발급 성공 시 refresh 토큰을 rotate 한다. {@link RefreshTokenRepository#rotate(Long, String, String)}가
 * Lua 스크립트로 "이전 값 일치 확인 + 새 값 저장"을 원자 실행하므로, 동시 요청이 들어와도 한 건만 성공하고 나머지는 401로 거절되어 일회성 회전 보장이 유지된다.
 * 단 CAS 실패 시 저장값을 삭제하지는 않는다. 정상적인 동시 reissue 경합에서 패배한 요청까지 키를 지우면 회전에 성공한 세션이 끊기기 때문이며, 회전 불가 자체가
 * 일회성 보장을 담당한다.
 *
 * <p>로그아웃은 rotate의 대칭쌍으로 {@link RefreshTokenRepository#deleteByUserId(Long)}만 수행 — userId 하나로 해당
 * 유저의 refresh를 통째로 무효화한다(탈취된 refresh 포함). 브라우저 쿠키 제거는 Controller 계층이 {@code
 * TokenDeliveryService#clear}로 위임하여, Service는 Redis 상태만 책임진다(재발급 플로우와 동일한 레이어 분담).
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

        // CAS 실패 시 저장값을 삭제하지 않는다. 앱 콜드 로드처럼 같은 refresh로 동시에 여러 reissue가
        // 들어오면 한 건만 회전에 성공하고 나머지는 직전 토큰으로 CAS가 불일치하는데, 여기서 키를 지우면
        // 회전에 성공한 멀쩡한 세션까지 끊긴다. 일회성 회전은 CAS 자체로 이미 보장되므로(불일치 토큰은
        // 회전 불가) 패배한 요청은 401만 받고 키는 보존한다.
        if (!refreshTokenRepository.rotate(userId, refreshToken, newRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        log.debug("리프레시 성공: userId={}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 — Redis에 저장된 해당 유저의 refresh 토큰을 삭제해 재발급 경로를 차단한다.
     *
     * <p>키 {@code refresh:{userId}}가 이미 없어도(TTL 만료 · {@code maxmemory-policy volatile-lru} eviction
     * 등) {@code redisTemplate.delete}는 예외 없이 no-op이라 자연 멱등성 보장. access 토큰은 stateless 원칙 유지 —
     * TTL(1h) 만료 대기, 블랙리스트 도입은 별도 이슈.
     */
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.debug("로그아웃 성공: userId={}", userId);
    }
}
