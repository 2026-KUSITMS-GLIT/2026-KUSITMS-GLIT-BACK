package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginFailureHandlerTest {

    private static final String CALLBACK_URL = "http://localhost:3000/auth/callback";

    @Mock OAuthCallbackUrlResolver oAuthCallbackUrlResolver;

    @InjectMocks OAuth2LoginFailureHandler handler;

    @Nested
    @DisplayName("onAuthenticationFailure - HappyPath")
    class HappyPath {

        @Test
        @DisplayName("원인 체인에 BusinessException이 있을 때 해당 ErrorCode를 error query로 redirect한다")
        void should_redirectWithBusinessErrorCode_when_businessExceptionInCauseChain()
                throws Exception {
            // given
            BusinessException businessException =
                    new BusinessException(
                            ErrorCode.INVALID_OAUTH_RESPONSE,
                            "providerUid가 비어 있습니다: provider=KAKAO");
            OAuth2AuthenticationException exception =
                    new OAuth2AuthenticationException(
                            new OAuth2Error(ErrorCode.INVALID_OAUTH_RESPONSE.getCode()),
                            "providerUid=1234 정규화 실패",
                            businessException);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(oAuthCallbackUrlResolver.resolveAndExpire(request, response))
                    .willReturn(CALLBACK_URL);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(
                            CALLBACK_URL
                                    + "?error="
                                    + encode(ErrorCode.INVALID_OAUTH_RESPONSE.getCode()));
        }

        @Test
        @DisplayName("원인 체인에 BusinessException이 없을 때 UNAUTHORIZED를 error query로 redirect한다")
        void should_redirectWithUnauthorized_when_noBusinessExceptionCause() throws Exception {
            // given
            BadCredentialsException exception = new BadCredentialsException("자격 증명 실패");
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(oAuthCallbackUrlResolver.resolveAndExpire(request, response))
                    .willReturn(CALLBACK_URL);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(CALLBACK_URL + "?error=" + encode(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("콜백 URL이 이미 query를 포함하면 `&error=...`로 이어 붙인다")
        void should_appendWithAmpersand_when_callbackHasExistingQuery() throws Exception {
            // given
            String callbackWithQuery = "http://localhost:3000/auth/callback?from=signup";
            BadCredentialsException exception = new BadCredentialsException("자격 증명 실패");
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(oAuthCallbackUrlResolver.resolveAndExpire(request, response))
                    .willReturn(callbackWithQuery);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            assertThat(response.getRedirectedUrl())
                    .isEqualTo(
                            callbackWithQuery
                                    + "&error="
                                    + encode(ErrorCode.UNAUTHORIZED.getCode()));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
