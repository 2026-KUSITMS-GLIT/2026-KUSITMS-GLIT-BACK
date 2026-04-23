package com.groute.groute_server.auth.service.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

class OAuthAttributesTest {

    // ════════════════════════════════════════════════════════════
    // KAKAO
    // ════════════════════════════════════════════════════════════

    @Test
    void from_kakao_withEmail_parsesCorrectly() {
        Map<String, Object> kakaoAccount = Map.of("email", "user@kakao.com");
        Map<String, Object> attributes = Map.of("id", 12345L, "kakao_account", kakaoAccount);

        OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(result.providerUid()).isEqualTo("12345");
        assertThat(result.email()).isEqualTo("user@kakao.com");
        assertThat(result.attributes()).isEqualTo(attributes);
    }

    @Test
    void from_kakao_withoutEmail_emailIsNull() {
        Map<String, Object> attributes = Map.of("id", 99L);

        OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

        assertThat(result.email()).isNull();
    }

    @Test
    void from_kakao_withKakaoAccountButNoEmail_emailIsNull() {
        Map<String, Object> kakaoAccount = new HashMap<>();
        // email key가 없음
        Map<String, Object> attributes =
                Map.of("id", 77L, "kakao_account", kakaoAccount);

        OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

        assertThat(result.email()).isNull();
    }

    @Test
    void from_kakao_missingId_throwsInvalidOAuthResponse() {
        Map<String, Object> attributes = Map.of("kakao_account", Map.of("email", "test@test.com"));

        assertThatThrownBy(() -> OAuthAttributes.from("kakao", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
    }

    @Test
    void from_kakao_registrationIdCaseInsensitive() {
        Map<String, Object> attributes = Map.of("id", 1L);

        OAuthAttributes result = OAuthAttributes.from("KAKAO", attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    // ════════════════════════════════════════════════════════════
    // GOOGLE
    // ════════════════════════════════════════════════════════════

    @Test
    void from_google_parsesCorrectly() {
        Map<String, Object> attributes =
                Map.of("sub", "google-uid-123", "email", "user@gmail.com");

        OAuthAttributes result = OAuthAttributes.from("google", attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(result.providerUid()).isEqualTo("google-uid-123");
        assertThat(result.email()).isEqualTo("user@gmail.com");
    }

    @Test
    void from_google_withoutEmail_emailIsNull() {
        Map<String, Object> attributes = Map.of("sub", "google-uid-456");

        OAuthAttributes result = OAuthAttributes.from("google", attributes);

        assertThat(result.email()).isNull();
    }

    @Test
    void from_google_missingSub_throwsInvalidOAuthResponse() {
        Map<String, Object> attributes = Map.of("email", "user@gmail.com");

        assertThatThrownBy(() -> OAuthAttributes.from("google", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
    }

    // ════════════════════════════════════════════════════════════
    // NAVER
    // ════════════════════════════════════════════════════════════

    @Test
    void from_naver_parsesCorrectly() {
        Map<String, Object> naverResponse = Map.of("id", "naver-uid-789", "email", "user@naver.com");
        Map<String, Object> attributes = Map.of("response", naverResponse);

        OAuthAttributes result = OAuthAttributes.from("naver", attributes);

        assertThat(result.provider()).isEqualTo(SocialProvider.NAVER);
        assertThat(result.providerUid()).isEqualTo("naver-uid-789");
        assertThat(result.email()).isEqualTo("user@naver.com");
    }

    @Test
    void from_naver_withoutEmail_emailIsNull() {
        Map<String, Object> naverResponse = Map.of("id", "naver-uid-abc");
        Map<String, Object> attributes = Map.of("response", naverResponse);

        OAuthAttributes result = OAuthAttributes.from("naver", attributes);

        assertThat(result.email()).isNull();
    }

    @Test
    void from_naver_missingResponseMap_throwsInvalidOAuthResponse() {
        Map<String, Object> attributes = Map.of("id", "some-id");

        assertThatThrownBy(() -> OAuthAttributes.from("naver", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
    }

    @Test
    void from_naver_missingIdInResponse_throwsInvalidOAuthResponse() {
        Map<String, Object> naverResponse = Map.of("email", "user@naver.com");
        Map<String, Object> attributes = Map.of("response", naverResponse);

        assertThatThrownBy(() -> OAuthAttributes.from("naver", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
    }

    // ════════════════════════════════════════════════════════════
    // 지원하지 않는 프로바이더
    // ════════════════════════════════════════════════════════════

    @Test
    void from_unsupportedProvider_throwsUnsupportedOAuthProvider() {
        Map<String, Object> attributes = Map.of("id", "some-id");

        assertThatThrownBy(() -> OAuthAttributes.from("facebook", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    @Test
    void from_emptyRegistrationId_throwsUnsupportedOAuthProvider() {
        Map<String, Object> attributes = Map.of("sub", "some-id");

        assertThatThrownBy(() -> OAuthAttributes.from("", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    // ════════════════════════════════════════════════════════════
    // compact constructor 검증 (providerUid blank 방어)
    // ════════════════════════════════════════════════════════════

    @Test
    void constructor_blankProviderUid_throwsInvalidOAuthResponse() {
        // Google sub가 빈 문자열인 경우 requireProviderUid는 통과하지만 compact constructor가 차단
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", ""); // 빈 문자열

        assertThatThrownBy(() -> OAuthAttributes.from("google", attributes))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OAUTH_RESPONSE);
    }

    // ── 경계값: 카카오 id가 Long이 아닌 String이어도 toString() 처리 ─
    @Test
    void from_kakao_idAsString_convertsToString() {
        Map<String, Object> attributes = Map.of("id", "string-id-value");

        OAuthAttributes result = OAuthAttributes.from("kakao", attributes);

        assertThat(result.providerUid()).isEqualTo("string-id-value");
    }
}