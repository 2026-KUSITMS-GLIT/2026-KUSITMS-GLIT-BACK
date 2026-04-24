package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.auth.service.SocialLoginService;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock SocialLoginService socialLoginService;
    @Mock DefaultOAuth2UserService delegate;

    @InjectMocks CustomOAuth2UserService service;

    @BeforeEach
    void replaceDelegate() {
        // delegate는 필드 초기화로 new 되므로, 테스트에서는 HTTP I/O를 피하기 위해 mock을 주입한다.
        ReflectionTestUtils.setField(service, "delegate", delegate);
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {

        @Test
        @DisplayName("정상 응답일 때 OAuthAttributes를 upsert에 위임하고 PrincipalUser를 반환한다")
        void should_delegateUpsertAndReturnPrincipal_when_responseIsValid() {
            // given
            Map<String, Object> rawAttributes =
                    Map.of("id", 9999L, "kakao_account", Map.of("email", "new@kakao.com"));
            OAuth2User delegated = mock(OAuth2User.class);
            given(delegated.getAttributes()).willReturn(rawAttributes);
            OAuth2UserRequest request = userRequestFor("kakao");
            given(delegate.loadUser(request)).willReturn(delegated);

            User user = User.createForSocialLogin();
            ReflectionTestUtils.setField(user, "id", 42L);
            given(socialLoginService.upsertUser(any(OAuthAttributes.class))).willReturn(user);

            // when
            OAuth2User result = service.loadUser(request);

            // then
            assertThat(result).isInstanceOf(PrincipalUser.class);
            PrincipalUser principal = (PrincipalUser) result;
            assertThat(principal.getUserId()).isEqualTo(42L);
            assertThat(principal.getProvider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(principal.getAttributes()).isEqualTo(rawAttributes);

            ArgumentCaptor<OAuthAttributes> captor = ArgumentCaptor.forClass(OAuthAttributes.class);
            verify(socialLoginService).upsertUser(captor.capture());
            OAuthAttributes captured = captor.getValue();
            assertThat(captured.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(captured.providerUid()).isEqualTo("9999");
            assertThat(captured.email()).isEqualTo("new@kakao.com");
        }

        @Test
        @DisplayName("정규화 중 BusinessException이 발생했을 때 OAuth2AuthenticationException으로 래핑한다")
        void should_wrapAsOAuth2AuthenticationException_when_normalizationFails() {
            // given
            OAuth2User delegated = mock(OAuth2User.class);
            given(delegated.getAttributes()).willReturn(Map.of());
            OAuth2UserRequest request = userRequestFor("apple");
            given(delegate.loadUser(request)).willReturn(delegated);

            // when & then
            assertThatThrownBy(() -> service.loadUser(request))
                    .isInstanceOfSatisfying(
                            OAuth2AuthenticationException.class,
                            ex -> {
                                assertThat(ex.getError().getErrorCode())
                                        .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER.getCode());
                                assertThat(ex.getCause()).isInstanceOf(BusinessException.class);
                            });
        }
    }

    private OAuth2UserRequest userRequestFor(String registrationId) {
        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        ClientRegistration registration = mock(ClientRegistration.class);
        given(request.getClientRegistration()).willReturn(registration);
        given(registration.getRegistrationId()).willReturn(registrationId);
        return request;
    }
}
