package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.config.AuthProperties.RefreshToken;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.common.jwt.JwtProperties;

@ExtendWith(MockitoExtension.class)
class TokenDeliveryServiceTest {

    @Mock private JwtProperties jwtProperties;
    @Mock private AuthProperties authProperties;

    @InjectMocks private TokenDeliveryService tokenDeliveryService;

    // ── 1. 쿠키 모드: refreshToken이 본문에서 null ───────────────────
    @Test
    void deliver_cookieEnabled_refreshTokenNullInBody() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenResponse result =
                tokenDeliveryService.deliver(response, "access-token", "refresh-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNull();
    }

    // ── 2. 쿠키 모드: Set-Cookie 헤더 설정 확인 ───────────────────────
    @Test
    void deliver_cookieEnabled_setsSetCookieHeader() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(604_800_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access-token", "my-refresh-token");

        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("refreshToken=my-refresh-token");
    }

    // ── 3. 쿠키 모드: HttpOnly 속성 포함 확인 ─────────────────────────
    @Test
    void deliver_cookieEnabled_cookieIsHttpOnly() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access", "refresh");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).containsIgnoringCase("HttpOnly");
    }

    // ── 4. 쿠키 모드: Secure 속성 포함 확인 ───────────────────────────
    @Test
    void deliver_cookieEnabled_cookieIsSecure() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access", "refresh");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).containsIgnoringCase("Secure");
    }

    // ── 5. 쿠키 모드: SameSite=Strict 포함 확인 ──────────────────────
    @Test
    void deliver_cookieEnabled_cookieHasSameSiteStrict() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access", "refresh");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).containsIgnoringCase("SameSite=Strict");
    }

    // ── 6. 본문 모드: 둘 다 본문에 포함 ───────────────────────────────
    @Test
    void deliver_cookieDisabled_bothTokensInBody() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(false));

        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenResponse result =
                tokenDeliveryService.deliver(response, "access-token", "refresh-token");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    // ── 7. 본문 모드: Set-Cookie 헤더 없음 ───────────────────────────
    @Test
    void deliver_cookieDisabled_noSetCookieHeader() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(false));

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access-token", "refresh-token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    // ── 8. 쿠키 모드: path는 "/" ──────────────────────────────────────
    @Test
    void deliver_cookieEnabled_cookiePathIsRoot() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(3_600_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access", "refresh");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).containsIgnoringCase("Path=/");
    }

    // ── 9. 쿠키 모드: TTL이 올바르게 Max-Age에 반영 ─────────────────
    @Test
    void deliver_cookieEnabled_maxAgeMatchesExpiration() {
        long expirationMs = 3_600_000L; // 1시간
        given(authProperties.refreshToken()).willReturn(new RefreshToken(true));
        given(jwtProperties.refreshTokenExpiration()).willReturn(expirationMs);

        MockHttpServletResponse response = new MockHttpServletResponse();
        tokenDeliveryService.deliver(response, "access", "refresh");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        // Max-Age = 3600 seconds
        assertThat(cookie).containsIgnoringCase("Max-Age=3600");
    }

    // ── 10. 경계값: accessToken이 null이어도 그대로 반환 ─────────────
    @Test
    void deliver_cookieDisabled_nullAccessTokenPassedThrough() {
        given(authProperties.refreshToken()).willReturn(new RefreshToken(false));

        MockHttpServletResponse response = new MockHttpServletResponse();
        TokenResponse result = tokenDeliveryService.deliver(response, null, "refresh");

        assertThat(result.accessToken()).isNull();
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }
}