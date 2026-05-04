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
        @DisplayName("정상 응답을 (KAKAO, providerUid, email)로 정규화한다")
        void parsesNormalResponse() {
            Map<String, Object> attributes =
                    Map.of("id", 1234567890L, "kakao_account", Map.of("email", "user@kakao.com"));

            OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

            assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(result.providerUid()).isEqualTo("1234567890");
            assertThat(result.email()).isEqualTo("user@kakao.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("kakao_account가 없어도 email을 null로 정상 파싱한다")
        void parsesWhenKakaoAccountMissing() {
            Map<String, Object> attributes = Map.of("id", 42L);

            OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

            assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(result.providerUid()).isEqualTo("42");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("id가 누락되면 INVALID_OAUTH_RESPONSE")
        void throwsWhenIdMissing() {
            Map<String, Object> attributes =
                    Map.of("kakao_account", Map.of("email", "user@kakao.com"));

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
        @DisplayName("정상 응답을 (GOOGLE, providerUid, email)로 정규화한다")
        void parsesNormalResponse() {
            Map<String, Object> attributes =
                    Map.of(
                            "sub", "google-sub-001",
                            "email", "user@gmail.com");

            OAuthAttributes result = OAuthAttributes.from("google", attributes);

            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(result.providerUid()).isEqualTo("google-sub-001");
            assertThat(result.email()).isEqualTo("user@gmail.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("sub가 누락되면 INVALID_OAUTH_RESPONSE")
        void throwsWhenSubMissing() {
            Map<String, Object> attributes = Map.of("email", "user@gmail.com");

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
        @DisplayName("response 하위 값을 (NAVER, providerUid, email)로 정규화한다")
        void parsesNormalResponse() {
            Map<String, Object> attributes =
                    Map.of(
                            "response",
                            Map.of(
                                    "id", "naver-id-001",
                                    "email", "user@naver.com"));

            OAuthAttributes result = OAuthAttributes.from("naver", attributes);

            assertThat(result.provider()).isEqualTo(SocialProvider.NAVER);
            assertThat(result.providerUid()).isEqualTo("naver-id-001");
            assertThat(result.email()).isEqualTo("user@naver.com");
            assertThat(result.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("response 루트가 없으면 INVALID_OAUTH_RESPONSE")
        void throwsWhenResponseMissing() {
            Map<String, Object> attributes = Map.of("id", "naver-id-001");

            assertThatThrownBy(() -> OAuthAttributes.from("naver", attributes))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
        }

        @Test
        @DisplayName("response.id가 누락되면 INVALID_OAUTH_RESPONSE")
        void throwsWhenIdMissing() {
            Map<String, Object> attributes = Map.of("response", Map.of("email", "user@naver.com"));

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
        @DisplayName("대소문자가 섞여 있어도 provider를 매칭한다")
        void matchesCaseInsensitively() {
            Map<String, Object> attributes =
                    Map.of(
                            "sub", "google-sub-001",
                            "email", "user@gmail.com");

            OAuthAttributes result = OAuthAttributes.from("GOOGLE", attributes);

            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("미지원 registrationId면 UNSUPPORTED_OAUTH_PROVIDER")
        void throwsOnUnsupportedProvider() {
            assertThatThrownBy(() -> OAuthAttributes.from("apple", Map.of()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
