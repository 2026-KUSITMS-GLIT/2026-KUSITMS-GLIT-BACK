package com.groute.groute_server.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.service.AuthService;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.support.WebMvcTestBase;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends WebMvcTestBase {

    @MockitoBean private AuthService authService;
    @MockitoBean private TokenDeliveryService tokenDeliveryService;

    private static final String REISSUE_URL = "/api/auth/reissue";
    private static final String LOGOUT_URL = "/api/auth/logout";

    @Nested
    @DisplayName("POST /api/auth/reissue")
    class Reissue {

        @Test
        @DisplayName("유효한 refreshToken 전달 시 200 반환")
        void should_return200_when_validRefreshToken() throws Exception {
            given(authService.reissue(anyString()))
                    .willReturn(new TokenResponse("access-token", "refresh-token"));
            given(
                            tokenDeliveryService.deliver(
                                    any(HttpServletResponse.class), anyString(), anyString()))
                    .willReturn(new TokenResponse("access-token", null));

            mockMvc.perform(
                            post(REISSUE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("빈 refreshToken 전달 시 400 반환")
        void should_return400_when_blankRefreshToken() throws Exception {
            mockMvc.perform(
                            post(REISSUE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"refreshToken\":\"   \"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("인증된 사용자 로그아웃 시 200 반환")
        void should_return200_when_authenticated() throws Exception {
            mockMvc.perform(post(LOGOUT_URL).with(auth(1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(authService).should().logout(1L);
        }

        @Test
        @DisplayName("미인증 요청 시 401 반환")
        void should_return401_when_unauthenticated() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)).andExpect(status().isUnauthorized());
        }
    }
}
