package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.common.jwt.JwtProperties;

class TokenDeliveryServiceTest {

    private static final long REFRESH_TTL_MILLIS = 3_600_000L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private TokenDeliveryService serviceWithCookieEnabled(boolean enabled) {
        JwtProperties jwtProperties = new JwtProperties("secret", 900_000L, REFRESH_TTL_MILLIS);
        AuthProperties authProperties =
                new AuthProperties(new AuthProperties.RefreshToken(enabled));
        return new TokenDeliveryService(jwtProperties, authProperties);
    }

    @Nested
    @DisplayName("deliver")
    class Deliver {

        @Test
        @DisplayName("쿠키 모드일 때 Set-Cookie에 refresh를 담고 응답 본문의 refresh는 null로 둔다")
        void should_setRefreshCookieAndOmitBody_when_cookieEnabled() {
            // given
            TokenDeliveryService service = serviceWithCookieEnabled(true);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            TokenResponse result = service.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN);

            // then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNull();

            List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(cookies).hasSize(1);
            assertThat(cookies.get(0))
                    .contains("refreshToken=" + REFRESH_TOKEN)
                    .contains("Max-Age=" + (REFRESH_TTL_MILLIS / 1000))
                    .contains("Path=/")
                    .contains("HttpOnly")
                    .contains("Secure")
                    .contains("SameSite=Strict");
        }

        @Test
        @DisplayName("쿠키 모드가 꺼져 있을 때 access·refresh 모두 본문으로 반환하고 Set-Cookie를 설정하지 않는다")
        void should_returnBothTokensInBody_when_cookieDisabled() {
            // given
            TokenDeliveryService service = serviceWithCookieEnabled(false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            TokenResponse result = service.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN);

            // then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("쿠키 모드일 때 Set-Cookie에 Max-Age=0 refresh 쿠키를 심어 브라우저 측에서 삭제되게 한다")
        void should_setExpiredRefreshCookie_when_cookieEnabled() {
            // given
            TokenDeliveryService service = serviceWithCookieEnabled(true);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            service.clear(response);

            // then
            List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(cookies).hasSize(1);
            assertThat(cookies.get(0))
                    .contains("refreshToken=")
                    .contains("Max-Age=0")
                    .contains("Path=/")
                    .contains("HttpOnly")
                    .contains("Secure")
                    .contains("SameSite=Strict");
        }

        @Test
        @DisplayName("쿠키 모드가 꺼져 있을 때 Set-Cookie 헤더를 추가하지 않는다")
        void should_notSetCookie_when_cookieDisabled() {
            // given
            TokenDeliveryService service = serviceWithCookieEnabled(false);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            service.clear(response);

            // then
            assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        }
    }
}
