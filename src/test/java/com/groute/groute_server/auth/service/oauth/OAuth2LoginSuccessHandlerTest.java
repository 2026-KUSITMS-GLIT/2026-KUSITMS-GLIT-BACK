package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.common.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    private static final String CALLBACK_URL = "http://localhost:3000/auth/callback";
    private static final String LOCAL_URL = "http://localhost:3000/auth/callback";
    private static final String PROD_URL = "https://glit.today/auth/callback";
    private static final Long USER_ID = 42L;
    private static final String ACCESS_TOKEN = "acc.tok.en";
    private static final String REFRESH_TOKEN = "ref.tok.en";

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TokenDeliveryService tokenDeliveryService;

    private OAuth2LoginSuccessHandler newHandler(boolean cookieEnabled, String callbackUrl) {
        return newHandler(cookieEnabled, Map.of("default", callbackUrl), "default");
    }

    private OAuth2LoginSuccessHandler newHandler(
            boolean cookieEnabled, Map<String, String> callback, String defaultEnv) {
        AuthProperties authProperties =
                new AuthProperties(
                        new AuthProperties.RefreshToken(cookieEnabled), callback, defaultEnv);
        return new OAuth2LoginSuccessHandler(
                jwtTokenProvider, refreshTokenRepository, tokenDeliveryService, authProperties);
    }

    @Nested
    @DisplayName("onAuthenticationSuccess - HappyPath")
    class HappyPath {

        @Test
        @DisplayName("쿠키 모드일 때 access만 query에 담아 콜백 URL로 redirect한다")
        void should_redirectWithAccessOnly_when_cookieMode() throws Exception {
            // given — cookieEnabled=true: refresh는 Set-Cookie로만 전달
            OAuth2LoginSuccessHandler handler = newHandler(true, CALLBACK_URL);
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, null));

            // when
            handler.onAuthenticationSuccess(
                    new MockHttpServletRequest(), response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            verify(refreshTokenRepository).save(USER_ID, REFRESH_TOKEN);
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(CALLBACK_URL + "?access=" + encode(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("본문 모드일 때 access·refresh 모두 query에 담아 redirect한다")
        void should_redirectWithBothTokens_when_bodyMode() throws Exception {
            // given — cookieEnabled=false: refresh는 query로 전달
            OAuth2LoginSuccessHandler handler = newHandler(false, CALLBACK_URL);
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, REFRESH_TOKEN));

            // when
            handler.onAuthenticationSuccess(
                    new MockHttpServletRequest(),
                    response,
                    authFor(USER_ID, SocialProvider.GOOGLE));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(
                            CALLBACK_URL
                                    + "?access="
                                    + encode(ACCESS_TOKEN)
                                    + "&refresh="
                                    + encode(REFRESH_TOKEN));
        }

        @Test
        @DisplayName("콜백 URL이 이미 query를 포함하면 `&`로 이어 붙인다")
        void should_appendWithAmpersand_when_callbackHasExistingQuery() throws Exception {
            // given — 콜백 URL에 기존 query 포함, separator 분기 검증
            String callbackWithQuery = "http://localhost:3000/auth/callback?from=signup";
            OAuth2LoginSuccessHandler handler = newHandler(true, callbackWithQuery);
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, null));

            // when
            handler.onAuthenticationSuccess(
                    new MockHttpServletRequest(), response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(callbackWithQuery + "&access=" + encode(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("redirect 호출 전에 활성 세션을 invalidate한다")
        void should_invalidateSession_before_redirect() throws Exception {
            // given
            OAuth2LoginSuccessHandler handler = newHandler(true, CALLBACK_URL);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.getSession(true); // 활성 세션 생성
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, null));

            // when
            handler.onAuthenticationSuccess(
                    request, response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(request.getSession(false)).isNull();
            assertThat(response.getRedirectedUrl()).startsWith(CALLBACK_URL + "?access=");
        }
    }

    @Nested
    @DisplayName("onAuthenticationSuccess - EnvCookie")
    class EnvCookie {

        @Test
        @DisplayName("oauth_env 쿠키가 production이면 production 콜백 URL로 redirect한다")
        void should_redirectToProductionCallback_when_envCookieIsProduction() throws Exception {
            // given
            OAuth2LoginSuccessHandler handler =
                    newHandler(true, Map.of("local", LOCAL_URL, "production", PROD_URL), "local");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("oauth_env", "production"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            stubTokens(response);

            // when
            handler.onAuthenticationSuccess(
                    request, response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(PROD_URL + "?access=" + encode(ACCESS_TOKEN));
            assertExpiredEnvCookie(response);
        }

        @Test
        @DisplayName("oauth_env 쿠키가 local이면 local 콜백 URL로 redirect한다")
        void should_redirectToLocalCallback_when_envCookieIsLocal() throws Exception {
            // given
            OAuth2LoginSuccessHandler handler =
                    newHandler(
                            true, Map.of("local", LOCAL_URL, "production", PROD_URL), "production");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("oauth_env", "local"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            stubTokens(response);

            // when
            handler.onAuthenticationSuccess(
                    request, response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(LOCAL_URL + "?access=" + encode(ACCESS_TOKEN));
            assertExpiredEnvCookie(response);
        }

        @Test
        @DisplayName("oauth_env 쿠키가 없으면 default env 콜백 URL로 redirect한다")
        void should_redirectToDefaultCallback_when_envCookieMissing() throws Exception {
            // given — defaultEnv=production
            OAuth2LoginSuccessHandler handler =
                    newHandler(
                            true, Map.of("local", LOCAL_URL, "production", PROD_URL), "production");
            MockHttpServletRequest request = new MockHttpServletRequest(); // 쿠키 없음
            MockHttpServletResponse response = new MockHttpServletResponse();
            stubTokens(response);

            // when
            handler.onAuthenticationSuccess(
                    request, response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(PROD_URL + "?access=" + encode(ACCESS_TOKEN));
            assertExpiredEnvCookie(response);
        }

        @Test
        @DisplayName("oauth_env 쿠키가 등록되지 않은 env이면 default env로 fallback한다")
        void should_fallbackToDefault_when_envCookieIsUnregistered() throws Exception {
            // given — env=staging은 매핑에 없음, defaultEnv=production
            OAuth2LoginSuccessHandler handler =
                    newHandler(
                            true, Map.of("local", LOCAL_URL, "production", PROD_URL), "production");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("oauth_env", "staging"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            stubTokens(response);

            // when
            handler.onAuthenticationSuccess(
                    request, response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(PROD_URL + "?access=" + encode(ACCESS_TOKEN));
            assertExpiredEnvCookie(response);
        }

        private void stubTokens(MockHttpServletResponse response) {
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, null));
        }

        private void assertExpiredEnvCookie(MockHttpServletResponse response) {
            assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                    .anyMatch(c -> c.startsWith("oauth_env=") && c.contains("Max-Age=0"));
        }
    }

    private static Authentication authFor(Long userId, SocialProvider provider) {
        PrincipalUser principal = new PrincipalUser(userId, provider, Map.of());
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(principal);
        return authentication;
    }

    private static String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
