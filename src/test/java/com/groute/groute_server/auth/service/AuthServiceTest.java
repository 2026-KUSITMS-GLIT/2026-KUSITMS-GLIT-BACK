package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.jwt.JwtTokenProvider;
import com.groute.groute_server.common.jwt.JwtValidationResult;
import com.groute.groute_server.common.jwt.TokenType;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    @Nested
    @DisplayName("재발급")
    class Reissue {

        @Test
        @DisplayName("토큰이 null일 때 REFRESH_TOKEN_REQUIRED를 던진다")
        void should_throwRefreshTokenRequired_when_tokenIsNull() {
            // when & then
            assertThatThrownBy(() -> authService.reissue(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        @Test
        @DisplayName("토큰이 공백 문자열일 때 REFRESH_TOKEN_REQUIRED를 던진다")
        void should_throwRefreshTokenRequired_when_tokenIsBlank() {
            // when & then
            assertThatThrownBy(() -> authService.reissue("   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        @Test
        @DisplayName("validate 결과가 VALID가 아닐 때 INVALID_REFRESH_TOKEN을 던진다")
        void should_throwInvalidRefreshToken_when_validateFails() {
            // given
            String expired = "expired-token";
            given(jwtTokenProvider.validate(expired)).willReturn(JwtValidationResult.EXPIRED);

            // when & then
            assertThatThrownBy(() -> authService.reissue(expired))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("토큰 타입이 REFRESH가 아닐 때 INVALID_REFRESH_TOKEN을 던진다")
        void should_throwInvalidRefreshToken_when_tokenTypeIsNotRefresh() {
            // given
            String access = "access-token";
            given(jwtTokenProvider.validate(access)).willReturn(JwtValidationResult.VALID);
            given(jwtTokenProvider.getTokenType(access)).willReturn(TokenType.ACCESS);

            // when & then
            assertThatThrownBy(() -> authService.reissue(access))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("rotate에 성공했을 때 새 토큰 쌍을 반환하고 저장값을 삭제하지 않는다")
        void should_returnNewTokens_when_rotateSucceeds() {
            // given
            String oldRefresh = "old-refresh";
            String newAccess = "new-access";
            String newRefresh = "new-refresh";
            Long userId = 42L;
            given(jwtTokenProvider.validate(oldRefresh)).willReturn(JwtValidationResult.VALID);
            given(jwtTokenProvider.getTokenType(oldRefresh)).willReturn(TokenType.REFRESH);
            given(jwtTokenProvider.getUserId(oldRefresh)).willReturn(userId);
            given(jwtTokenProvider.createAccessToken(userId)).willReturn(newAccess);
            given(jwtTokenProvider.createRefreshToken(userId)).willReturn(newRefresh);
            given(refreshTokenRepository.rotate(userId, oldRefresh, newRefresh)).willReturn(true);

            // when
            TokenResponse response = authService.reissue(oldRefresh);

            // then
            assertThat(response.accessToken()).isEqualTo(newAccess);
            assertThat(response.refreshToken()).isEqualTo(newRefresh);
            verify(refreshTokenRepository, never()).deleteByUserId(anyLong());
        }

        @Test
        @DisplayName("rotate에 실패했을 때 저장값을 삭제하고 INVALID_REFRESH_TOKEN을 던진다")
        void should_deleteStoredAndThrowInvalidRefreshToken_when_rotateFails() {
            // given
            String oldRefresh = "old-refresh";
            String newRefresh = "new-refresh";
            Long userId = 42L;
            given(jwtTokenProvider.validate(oldRefresh)).willReturn(JwtValidationResult.VALID);
            given(jwtTokenProvider.getTokenType(oldRefresh)).willReturn(TokenType.REFRESH);
            given(jwtTokenProvider.getUserId(oldRefresh)).willReturn(userId);
            given(jwtTokenProvider.createAccessToken(userId)).willReturn("new-access");
            given(jwtTokenProvider.createRefreshToken(userId)).willReturn(newRefresh);
            given(refreshTokenRepository.rotate(userId, oldRefresh, newRefresh)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissue(oldRefresh))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
            verify(refreshTokenRepository).deleteByUserId(userId);
        }
    }

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
