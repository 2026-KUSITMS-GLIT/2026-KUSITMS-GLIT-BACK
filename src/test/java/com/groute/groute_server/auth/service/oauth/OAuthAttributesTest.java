package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

class OAuthAttributesTest {

    @Nested
    @DisplayName("카카오")
    class Kakao {

        @Test
        @DisplayName("정상 응답일 때 (KAKAO, providerUid, email)로 정규화한다")
        void should_normalize_when_kakaoResponseIsValid() {
            // given
            Map<String, Object> attributes =
                    Map.of("id", 1234567890L, "kakao_account", Map.of("email", "user@kakao.com"));

            // when
            OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(result.providerUid()).isEqualTo("1234567890");
            assertThat(result.email()).isEqualTo("user@kakao.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("kakao_account가 없을 때 email을 null로 정규화한다")
        void should_normalizeWithNullEmail_when_kakaoAccountMissing() {
            // given
            Map<String, Object> attributes = Map.of("id", 42L);

            // when
            OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(result.providerUid()).isEqualTo("42");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("id가 누락됐을 때 INVALID_OAUTH_RESPONSE를 던진다")
        void should_throwInvalidOAuthResponse_when_kakaoIdMissing() {
            // given
            Map<String, Object> attributes =
                    Map.of("kakao_account", Map.of("email", "user@kakao.com"));

            // when & then
            assertThatThrownBy(() -> OAuthAttributes.from("kakao", attributes))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
        }
    }

    @Nested
    @DisplayName("구글")
    class Google {

        @Test
        @DisplayName("정상 응답일 때 (GOOGLE, providerUid, email)로 정규화한다")
        void should_normalize_when_googleResponseIsValid() {
            // given
            Map<String, Object> attributes =
                    Map.of("sub", "google-sub-001", "email", "user@gmail.com");

            // when
            OAuthAttributes result = OAuthAttributes.from("google", attributes);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(result.providerUid()).isEqualTo("google-sub-001");
            assertThat(result.email()).isEqualTo("user@gmail.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("sub가 누락됐을 때 INVALID_OAUTH_RESPONSE를 던진다")
        void should_throwInvalidOAuthResponse_when_googleSubMissing() {
            // given
            Map<String, Object> attributes = Map.of("email", "user@gmail.com");

            // when & then
            assertThatThrownBy(() -> OAuthAttributes.from("google", attributes))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
        }
    }

    @Nested
    @DisplayName("네이버")
    class Naver {

        @Test
        @DisplayName("정상 응답일 때 response 하위 값을 (NAVER, providerUid, email)로 정규화한다")
        void should_normalize_when_naverResponseIsValid() {
            // given
            Map<String, Object> attributes =
                    Map.of("response", Map.of("id", "naver-id-001", "email", "user@naver.com"));

            // when
            OAuthAttributes result = OAuthAttributes.from("naver", attributes);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.NAVER);
            assertThat(result.providerUid()).isEqualTo("naver-id-001");
            assertThat(result.email()).isEqualTo("user@naver.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("response 루트가 없을 때 INVALID_OAUTH_RESPONSE를 던진다")
        void should_throwInvalidOAuthResponse_when_naverResponseMissing() {
            // given
            Map<String, Object> attributes = Map.of("id", "naver-id-001");

            // when & then
            assertThatThrownBy(() -> OAuthAttributes.from("naver", attributes))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
        }

        @Test
        @DisplayName("response.id가 누락됐을 때 INVALID_OAUTH_RESPONSE를 던진다")
        void should_throwInvalidOAuthResponse_when_naverIdMissing() {
            // given
            Map<String, Object> attributes = Map.of("response", Map.of("email", "user@naver.com"));

            // when & then
            assertThatThrownBy(() -> OAuthAttributes.from("naver", attributes))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
        }
    }

    @Nested
    @DisplayName("registrationId 분기")
    class Registration {

        @Test
        @DisplayName("registrationId 대소문자가 섞여 있을 때 동일 provider로 매칭한다")
        void should_matchProvider_when_registrationIdCaseMixed() {
            // given
            Map<String, Object> attributes =
                    Map.of("sub", "google-sub-001", "email", "user@gmail.com");

            // when
            OAuthAttributes result = OAuthAttributes.from("GOOGLE", attributes);

            // then
            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("미지원 registrationId일 때 UNSUPPORTED_OAUTH_PROVIDER를 던진다")
        void should_throwUnsupportedProvider_when_registrationIdNotSupported() {
            // when & then
            assertThatThrownBy(() -> OAuthAttributes.from("apple", Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
