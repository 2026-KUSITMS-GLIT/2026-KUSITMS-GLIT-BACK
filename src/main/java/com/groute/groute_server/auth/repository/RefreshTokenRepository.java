package com.groute.groute_server.auth.repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
 * #rotate(Long, String, String)}로 이전 값과 일치 확인 + 새 값 저장을 Lua 스크립트로 원자 실행한다. 두 단계가 분리되면 동시 요청이 같은
 * refresh로 모두 통과해 일회성 회전이 깨지므로 CAS가 필수.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    /** 이전 값과 일치할 때만 새 값 + TTL로 덮어쓰는 compare-and-set 스크립트. 성공 시 1, 불일치 시 0 반환. */
    private static final DefaultRedisScript<Long> COMPARE_AND_SET_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                            + "  redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3]) "
                            + "  return 1 "
                            + "else return 0 end",
                    Long.class);

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

    /**
     * 저장된 값이 {@code expectedOld}와 일치하면 {@code newToken}으로 덮어쓴다. 동시 요청에서 먼저 도달한 한 건만 성공하고 나머지는
     * false를 반환해 401로 거절된다.
     *
     * @return 회전 성공 여부
     */
    public boolean rotate(Long userId, String expectedOld, String newToken) {
        long ttlSeconds = jwtProperties.refreshTokenExpiration() / 1000;
        Long result =
                redisTemplate.execute(
                        COMPARE_AND_SET_SCRIPT,
                        List.of(buildKey(userId)),
                        expectedOld,
                        newToken,
                        String.valueOf(ttlSeconds));
        return Long.valueOf(1L).equals(result);
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
