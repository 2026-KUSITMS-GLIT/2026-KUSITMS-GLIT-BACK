package com.groute.groute_server.auth.service.oauth;

import java.util.Map;

import com.groute.groute_server.auth.enums.SocialProvider;

/**
 * provider별 OAuth2 사용자 응답을 공통 포맷으로 정규화.
 *
 * <p>응답 구조가 provider마다 다르다.
 *
 * <ul>
 *   <li>카카오: 루트에 {@code id} (Long), {@code kakao_account.email}
 *   <li>구글: 루트에 {@code sub} (String), {@code email}
 *   <li>네이버: 루트 {@code response} 맵 안에 {@code id} (String), {@code email}
 * </ul>
 *
 * <p>{@link #from(String, Map)}이 registrationId에 따라 값을 발라 {@code (provider, providerUid, email)} 세
 * 값으로 통일한다. 이후 upsert는 세 값만 참조한다.
 */
public record OAuthAttributes(
        SocialProvider provider, String providerUid, String email, Map<String, Object> attributes) {

    public OAuthAttributes {
        if (providerUid == null || providerUid.isBlank()) {
            throw new IllegalArgumentException("providerUid가 비어 있습니다: provider=" + provider);
        }
    }

    public static OAuthAttributes from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> ofKakao(attributes);
            case "google" -> ofGoogle(attributes);
            case "naver" -> ofNaver(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 프로바이더: " + registrationId);
        };
    }

    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        String providerUid = String.valueOf(attributes.get("id"));
        String email = null;
        if (attributes.get("kakao_account") instanceof Map<?, ?> account
                && account.get("email") instanceof String kakaoEmail) {
            email = kakaoEmail;
        }
        return new OAuthAttributes(SocialProvider.KAKAO, providerUid, email, attributes);
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        String providerUid = String.valueOf(attributes.get("sub"));
        String email = (String) attributes.get("email");
        return new OAuthAttributes(SocialProvider.GOOGLE, providerUid, email, attributes);
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        if (!(attributes.get("response") instanceof Map<?, ?> response)) {
            throw new IllegalArgumentException("네이버 응답 형식이 올바르지 않습니다: response 누락");
        }
        String providerUid = String.valueOf(response.get("id"));
        String email = response.get("email") instanceof String e ? e : null;
        return new OAuthAttributes(SocialProvider.NAVER, providerUid, email, attributes);
    }
}
