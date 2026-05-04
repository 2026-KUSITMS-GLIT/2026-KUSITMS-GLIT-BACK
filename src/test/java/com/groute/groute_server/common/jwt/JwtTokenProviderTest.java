package com.groute.groute_server.common.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hs256!!";
    private static final long ACCESS_TTL_MILLIS = 900_000L;
    private static final long REFRESH_TTL_MILLIS = 1_209_600_000L;
    private static final Long USER_ID = 42L;

    private JwtTokenProvider providerWith(long accessTtl, long refreshTtl) {
        return new JwtTokenProvider(new JwtProperties(SECRET, accessTtl, refreshTtl));
    }

    private JwtTokenProvider provider() {
        return providerWith(ACCESS_TTL_MILLIS, REFRESH_TTL_MILLIS);
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("access 토큰 발급 시 subject에 userId, type claim에 ACCESS가 담긴다")
        void should_embedUserIdAndAccessType_when_createAccessToken() {
            // given
            JwtTokenProvider provider = provider();

            // when
            String token = provider.createAccessToken(USER_ID);

            // then
            assertThat(provider.getUserId(token)).isEqualTo(USER_ID);
            assertThat(provider.getTokenType(token)).isEqualTo(TokenType.ACCESS);
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("refresh 토큰 발급 시 subject에 userId, type claim에 REFRESH가 담긴다")
        void should_embedUserIdAndRefreshType_when_createRefreshToken() {
            // given
            JwtTokenProvider provider = provider();

            // when
            String token = provider.createRefreshToken(USER_ID);

            // then
            assertThat(provider.getUserId(token)).isEqualTo(USER_ID);
            assertThat(provider.getTokenType(token)).isEqualTo(TokenType.REFRESH);
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("정상 발급된 토큰을 검증하면 VALID를 반환한다")
        void should_returnValid_when_tokenIsFresh() {
            // given
            JwtTokenProvider provider = provider();
            String token = provider.createAccessToken(USER_ID);

            // when
            JwtValidationResult result = provider.validate(token);

            // then
            assertThat(result).isEqualTo(JwtValidationResult.VALID);
        }

        @Test
        @DisplayName("만료된 토큰을 검증하면 EXPIRED를 반환한다")
        void should_returnExpired_when_tokenIsExpired() {
            // given
            JwtTokenProvider provider = providerWith(-1_000L, REFRESH_TTL_MILLIS);
            String expiredToken = provider.createAccessToken(USER_ID);

            // when
            JwtValidationResult result = provider.validate(expiredToken);

            // then
            assertThat(result).isEqualTo(JwtValidationResult.EXPIRED);
        }

        @Test
        @DisplayName("서명 키가 다른 토큰을 검증하면 INVALID를 반환한다")
        void should_returnInvalid_when_signatureDoesNotMatch() {
            // given
            String otherSecret = "another-secret-key-also-32-bytes-long-for-hs256!!";
            SecretKey otherKey =
                    Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
            String foreignToken =
                    Jwts.builder().subject("42").signWith(otherKey).compact();
            JwtTokenProvider provider = provider();

            // when
            JwtValidationResult result = provider.validate(foreignToken);

            // then
            assertThat(result).isEqualTo(JwtValidationResult.INVALID);
        }

        @Test
        @DisplayName("형식이 깨진 토큰을 검증하면 INVALID를 반환한다")
        void should_returnInvalid_when_tokenIsMalformed() {
            // given
            JwtTokenProvider provider = provider();

            // when
            JwtValidationResult result = provider.validate("not-a-jwt");

            // then
            assertThat(result).isEqualTo(JwtValidationResult.INVALID);
        }

        @Test
        @DisplayName("토큰 문자열이 null/빈 문자열이면 INVALID를 반환한다")
        void should_returnInvalid_when_tokenIsNullOrEmpty() {
            // given
            JwtTokenProvider provider = provider();

            // when & then
            assertThat(provider.validate(null)).isEqualTo(JwtValidationResult.INVALID);
            assertThat(provider.validate("")).isEqualTo(JwtValidationResult.INVALID);
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("정상 발급된 토큰에서 subject(userId)를 추출한다")
        void should_returnUserId_when_tokenIsValid() {
            // given
            JwtTokenProvider provider = provider();
            String token = provider.createAccessToken(USER_ID);

            // when
            Long extracted = provider.getUserId(token);

            // then
            assertThat(extracted).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("만료된 토큰에서 userId를 추출하면 ExpiredJwtException이 발생한다")
        void should_throwExpiredJwtException_when_tokenIsExpired() {
            // given
            JwtTokenProvider provider = providerWith(-1_000L, REFRESH_TTL_MILLIS);
            String expiredToken = provider.createAccessToken(USER_ID);

            // when & then
            assertThatThrownBy(() -> provider.getUserId(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("getTokenType")
    class GetTokenType {

        @Test
        @DisplayName("access 토큰에서 type을 추출하면 ACCESS를 반환한다")
        void should_returnAccess_when_tokenIsAccess() {
            // given
            JwtTokenProvider provider = provider();
            String token = provider.createAccessToken(USER_ID);

            // when
            TokenType type = provider.getTokenType(token);

            // then
            assertThat(type).isEqualTo(TokenType.ACCESS);
        }

        @Test
        @DisplayName("refresh 토큰에서 type을 추출하면 REFRESH를 반환한다")
        void should_returnRefresh_when_tokenIsRefresh() {
            // given
            JwtTokenProvider provider = provider();
            String token = provider.createRefreshToken(USER_ID);

            // when
            TokenType type = provider.getTokenType(token);

            // then
            assertThat(type).isEqualTo(TokenType.REFRESH);
        }
    }
}
