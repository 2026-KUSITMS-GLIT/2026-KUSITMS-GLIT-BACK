package com.groute.groute_server.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.groute.groute_server.common.jwt.JwtProperties;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private JwtProperties jwtProperties;
    @Mock private ValueOperations<String, String> valueOps;

    private RefreshTokenRepository repository;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        repository = new RefreshTokenRepository(redisTemplate, jwtProperties);
    }

    // ── 1. save: 올바른 키 형식으로 저장 ────────────────────────────
    @Test
    void save_storesHashedTokenWithCorrectKey() {
        long expirationMs = 604_800_000L; // 7일
        given(jwtProperties.refreshTokenExpiration()).willReturn(expirationMs);

        repository.save(1L, "my-refresh-token");

        String expectedKey = "refresh:1";
        String expectedHash = sha256("my-refresh-token");
        then(valueOps)
                .should()
                .set(eq(expectedKey), eq(expectedHash), eq(Duration.ofMillis(expirationMs)));
    }

    // ── 2. save: 토큰 원문이 아닌 해시를 저장 ────────────────────────
    @Test
    void save_storesHashNotPlaintext() {
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        repository.save(100L, "plain-token");

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        then(valueOps).should().set(anyString(), valueCaptor.capture(), any(Duration.class));

        String storedValue = valueCaptor.getValue();
        assertThat(storedValue).isNotEqualTo("plain-token");
        assertThat(storedValue).isEqualTo(sha256("plain-token"));
        assertThat(storedValue).hasSize(64); // SHA-256 hex = 64 chars
    }

    // ── 3. deleteByUserId: 정확한 키 삭제 ───────────────────────────
    @Test
    void deleteByUserId_deletesCorrectKey() {
        repository.deleteByUserId(42L);
        then(redisTemplate).should().delete("refresh:42");
    }

    // ── 4. rotate: Lua 스크립트 실행 + 성공 시 true 반환 ─────────────
    @Test
    void rotate_whenScriptReturnsOne_returnsTrue() {
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);
        given(redisTemplate.execute(any(), anyList(), anyString(), anyString(), anyString()))
                .willReturn(1L);

        boolean result = repository.rotate(1L, "old-token", "new-token");

        assertThat(result).isTrue();
    }

    // ── 5. rotate: Lua 스크립트 불일치 시 false 반환 ─────────────────
    @Test
    void rotate_whenScriptReturnsZero_returnsFalse() {
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);
        given(redisTemplate.execute(any(), anyList(), anyString(), anyString(), anyString()))
                .willReturn(0L);

        boolean result = repository.rotate(1L, "wrong-old-token", "new-token");

        assertThat(result).isFalse();
    }

    // ── 6. rotate: 스크립트에 해시된 값 전달 확인 ────────────────────
    @Test
    void rotate_passesHashedValuesToScript() {
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);
        given(redisTemplate.execute(any(), anyList(), anyString(), anyString(), anyString()))
                .willReturn(1L);

        repository.rotate(5L, "old-token", "new-token");

        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg3 = ArgumentCaptor.forClass(String.class);
        then(redisTemplate)
                .should()
                .execute(any(), anyList(), arg1.capture(), arg2.capture(), arg3.capture());

        assertThat(arg1.getValue()).isEqualTo(sha256("old-token"));
        assertThat(arg2.getValue()).isEqualTo(sha256("new-token"));
        assertThat(arg3.getValue()).isEqualTo("604800"); // 604_800_000 ms / 1000 = 604800 seconds
    }

    // ── 7. rotate: null 반환 시 false 반환 ───────────────────────────
    @Test
    void rotate_whenScriptReturnsNull_returnsFalse() {
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);
        given(redisTemplate.execute(any(), anyList(), anyString(), anyString(), anyString()))
                .willReturn(null);

        boolean result = repository.rotate(1L, "old-token", "new-token");

        assertThat(result).isFalse();
    }

    // ── 8. 경계값: 서로 다른 userId는 독립된 키 사용 ──────────────────
    @Test
    void deleteByUserId_differentUserIds_useDifferentKeys() {
        repository.deleteByUserId(1L);
        then(redisTemplate).should().delete("refresh:1");

        repository.deleteByUserId(2L);
        then(redisTemplate).should().delete("refresh:2");
    }

    // ── 9. 동일 토큰의 해시는 항상 동일 ───────────────────────────────
    @Test
    void hashIsDeterministic() throws Exception {
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        repository.save(1L, "same-token");
        repository.save(1L, "same-token");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(valueOps)
                .should(org.mockito.Mockito.times(2))
                .set(anyString(), captor.capture(), any(Duration.class));

        assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
    }

    // ── 헬퍼: SHA-256 해시 계산 (테스트용 검증) ──────────────────────
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}