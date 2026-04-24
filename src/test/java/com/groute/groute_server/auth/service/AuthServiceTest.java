package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("logout을 호출했을 때 Redis의 refresh 토큰을 userId로 삭제한다")
        void should_deleteRefreshTokenByUserId_when_logoutCalled() {
            // given
            Long userId = 42L;

            // when
            authService.logout(userId);

            // then
            verify(refreshTokenRepository).deleteByUserId(userId);
        }

        @Test
        @DisplayName("logout을 두 번 호출해도 예외 없이 매 호출마다 deleteByUserId를 수행한다")
        void should_beIdempotent_when_logoutCalledTwice() {
            // given
            Long userId = 42L;

            // when & then
            assertThatCode(
                            () -> {
                                authService.logout(userId);
                                authService.logout(userId);
                            })
                    .doesNotThrowAnyException();
            verify(refreshTokenRepository, times(2)).deleteByUserId(userId);
        }
    }
}
