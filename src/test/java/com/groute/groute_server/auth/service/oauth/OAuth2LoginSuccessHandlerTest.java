package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.common.jwt.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TokenDeliveryService tokenDeliveryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler =
                new OAuth2LoginSuccessHandler(
                        jwtTokenProvider,
                        refreshTokenRepository,
                        tokenDeliveryService,
                        objectMapper);
    }

    @Nested
    @DisplayName("onAuthenticationSuccess")
    class OnSuccess {

        @Test
        @DisplayName("인증 성공일 때 토큰 쌍을 발급·저장하고 deliver 결과를 JSON 본문으로 기록한다")
        void should_issueTokensAndWriteJson_when_authenticated() throws Exception {
            // given
            Long userId = 42L;
            PrincipalUser principal = new PrincipalUser(userId, SocialProvider.KAKAO, Map.of());
            Authentication authentication = mock(Authentication.class);
            given(authentication.getPrincipal()).willReturn(principal);

            String access = "access-token";
            String refresh = "refresh-token";
            given(jwtTokenProvider.createAccessToken(userId)).willReturn(access);
            given(jwtTokenProvider.createRefreshToken(userId)).willReturn(refresh);

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(tokenDeliveryService.deliver(response, access, refresh))
                    .willReturn(new TokenResponse(access, null));

            // when
            handler.onAuthenticationSuccess(request, response, authentication);

            // then
            verify(refreshTokenRepository).save(userId, refresh);
            verify(tokenDeliveryService).deliver(response, access, refresh);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
            assertThat(response.getContentAsString())
                    .contains("\"success\":true")
                    .contains("\"accessToken\":\"" + access + "\"")
                    .doesNotContain("\"refreshToken\"");
        }
    }
}
