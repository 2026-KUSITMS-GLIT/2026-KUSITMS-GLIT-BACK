package com.groute.groute_server.auth.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.groute.groute_server.common.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

/**
 * 리프레시 토큰의 Redis 저장소.
 *
 * <p>키 포맷 {@code refresh:{userId}}, 값은 리프레시 토큰 평문, TTL은 {@link
 * JwtProperties#refreshTokenExpiration()}과 동일. TTL 만료 시 Redis가 자동 제거하므로 별도 정리 배치는 불필요.
 *
 * <p>로그아웃/탈퇴(MYP004·MYP005) 시 {@link #deleteByUserId(Long)}로 무효화하고, 재발급(ONB001) 시 {@link
 * #matches(Long, String)}로 쿠키에 실려온 값과 저장값의 일치 여부를 검증한다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(Long userId, String refreshToken) {
        redisTemplate
                .opsForValue()
                .set(
                        buildKey(userId),
                        refreshToken,
                        Duration.ofMillis(jwtProperties.refreshTokenExpiration()));
    }

    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(userId)));
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }

    /** 저장된 리프레시 토큰과 요청값이 일치하는지 확인. ==로 인한 timing attack 방지를 위해 상수 시간 비교. */
    public boolean matches(Long userId, String refreshToken) {
        return findByUserId(userId)
                .map(
                        stored ->
                                MessageDigest.isEqual(
                                        stored.getBytes(StandardCharsets.UTF_8),
                                        refreshToken.getBytes(StandardCharsets.UTF_8)))
                .orElse(false);
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
