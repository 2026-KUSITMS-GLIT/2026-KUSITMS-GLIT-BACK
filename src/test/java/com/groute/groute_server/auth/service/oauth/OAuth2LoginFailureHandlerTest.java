package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

class OAuth2LoginFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(objectMapper);

    @Nested
    @DisplayName("onAuthenticationFailure")
    class OnFailure {

        @Test
        @DisplayName("원인 체인에 BusinessException이 있을 때 해당 ErrorCode로 응답하고 상세 메시지는 마스킹한다")
        void should_respondWithMaskedErrorResponse_when_businessExceptionInCauseChain()
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

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            assertThat(response.getStatus())
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE.getHttpStatus().value());
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getContentAsString())
                    .contains("\"code\":\"" + ErrorCode.INVALID_OAUTH_RESPONSE.getCode() + "\"")
                    .contains(
                            "\"message\":\"" + ErrorCode.INVALID_OAUTH_RESPONSE.getMessage() + "\"")
                    .doesNotContain("providerUid");
        }

        @Test
        @DisplayName("원인 체인에 BusinessException이 없을 때 UNAUTHORIZED 기본 응답을 반환한다")
        void should_respondWithUnauthorized_when_noBusinessExceptionCause() throws Exception {
            // given
            BadCredentialsException exception = new BadCredentialsException("자격 증명 실패");
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            assertThat(response.getStatus())
                    .isEqualTo(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
            assertThat(response.getContentAsString())
                    .contains("\"code\":\"" + ErrorCode.UNAUTHORIZED.getCode() + "\"")
                    .contains("\"message\":\"" + ErrorCode.UNAUTHORIZED.getMessage() + "\"");
        }
    }
}
