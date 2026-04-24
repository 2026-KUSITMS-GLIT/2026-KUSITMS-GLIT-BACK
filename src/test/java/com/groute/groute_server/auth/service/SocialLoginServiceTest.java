package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.auth.entity.SocialAccount;
import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.auth.repository.SocialAccountRepository;
import com.groute.groute_server.auth.service.oauth.OAuthAttributes;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock UserRepository userRepository;
    @Mock SocialAccountRepository socialAccountRepository;

    @InjectMocks SocialLoginService socialLoginService;

    @Nested
    @DisplayName("upsertUser")
    class UpsertUser {

        @Test
        @DisplayName("기존 소셜 계정이 없을 때 User를 먼저 저장하고 SocialAccount를 이어서 저장한 뒤 User를 반환한다")
        void should_createUserAndSocialAccount_when_socialAccountNotFound() {
            // given
            OAuthAttributes attributes =
                    OAuthAttributes.from(
                            "kakao",
                            Map.of("id", 9999L, "kakao_account", Map.of("email", "new@kakao.com")));
            given(
                            socialAccountRepository.findByProviderAndProviderUid(
                                    SocialProvider.KAKAO, "9999"))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class)))
                    .willAnswer(
                            invocation -> {
                                User saved = invocation.getArgument(0);
                                ReflectionTestUtils.setField(saved, "id", 42L);
                                return saved;
                            });

            // when
            User result = socialLoginService.upsertUser(attributes);

            // then
            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getLastLoginAt()).isNotNull();

            InOrder inOrder = inOrder(userRepository, socialAccountRepository);
            inOrder.verify(userRepository).save(any(User.class));

            ArgumentCaptor<SocialAccount> captor = ArgumentCaptor.forClass(SocialAccount.class);
            inOrder.verify(socialAccountRepository).save(captor.capture());
            SocialAccount saved = captor.getValue();
            assertThat(saved.getProvider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(saved.getProviderUid()).isEqualTo("9999");
            assertThat(saved.getEmail()).isEqualTo("new@kakao.com");
            assertThat(saved.getUser()).isSameAs(result);
        }

        @Test
        @DisplayName("기존 소셜 계정이 있을 때 email을 갱신하고 recordLogin만 호출하며 새로 저장하지 않는다")
        void should_updateEmailAndRecordLoginOnly_when_socialAccountExists() {
            // given
            User existingUser = User.createForSocialLogin();
            ReflectionTestUtils.setField(existingUser, "id", 42L);
            SocialAccount existingAccount =
                    SocialAccount.create(
                            existingUser, SocialProvider.KAKAO, "9999", "old@kakao.com");
            given(
                            socialAccountRepository.findByProviderAndProviderUid(
                                    SocialProvider.KAKAO, "9999"))
                    .willReturn(Optional.of(existingAccount));
            OAuthAttributes attributes =
                    OAuthAttributes.from(
                            "kakao",
                            Map.of("id", 9999L, "kakao_account", Map.of("email", "new@kakao.com")));

            // when
            User result = socialLoginService.upsertUser(attributes);

            // then
            assertThat(result).isSameAs(existingUser);
            assertThat(result.getLastLoginAt()).isNotNull();
            assertThat(existingAccount.getEmail()).isEqualTo("new@kakao.com");
            verify(userRepository, never()).save(any(User.class));
            verify(socialAccountRepository, never()).save(any(SocialAccount.class));
        }
    }
}
