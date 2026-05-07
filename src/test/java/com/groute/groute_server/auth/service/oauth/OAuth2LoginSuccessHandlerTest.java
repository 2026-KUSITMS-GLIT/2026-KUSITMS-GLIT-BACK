package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private static final Long USER_ID = 42L;
    private static final String ACCESS_TOKEN = "acc.tok.en";
    private static final String REFRESH_TOKEN = "ref.tok.en";

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TokenDeliveryService tokenDeliveryService;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties =
                new AuthProperties(new AuthProperties.RefreshToken(false), CALLBACK_URL);
        handler =
                new OAuth2LoginSuccessHandler(
                        jwtTokenProvider,
                        refreshTokenRepository,
                        tokenDeliveryService,
                        authProperties);
    }

    @Nested
    @DisplayName("onAuthenticationSuccess - HappyPath")
    class HappyPath {

        @Test
        @DisplayName("쿠키 모드(refresh=null)일 때 access만 query에 담아 콜백 URL로 redirect한다")
        void should_redirectWithAccessOnly_when_cookieMode() throws Exception {
            // given
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
        @DisplayName("본문 모드(refresh!=null)일 때 access·refresh 모두 query에 담아 redirect한다")
        void should_redirectWithBothTokens_when_bodyMode() throws Exception {
            // given
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
            // given
            String callbackWithQuery = "http://localhost:3000/auth/callback?from=signup";
            AuthProperties authProperties =
                    new AuthProperties(new AuthProperties.RefreshToken(false), callbackWithQuery);
            OAuth2LoginSuccessHandler localHandler =
                    new OAuth2LoginSuccessHandler(
                            jwtTokenProvider,
                            refreshTokenRepository,
                            tokenDeliveryService,
                            authProperties);

            MockHttpServletResponse response = new MockHttpServletResponse();
            given(jwtTokenProvider.createAccessToken(USER_ID)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);
            given(tokenDeliveryService.deliver(response, ACCESS_TOKEN, REFRESH_TOKEN))
                    .willReturn(new TokenResponse(ACCESS_TOKEN, null));

            // when
            localHandler.onAuthenticationSuccess(
                    new MockHttpServletRequest(), response, authFor(USER_ID, SocialProvider.KAKAO));

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(callbackWithQuery + "&access=" + encode(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("redirect 호출 전에 활성 세션을 invalidate한다")
        void should_invalidateSession_before_redirect() throws Exception {
            // given
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
