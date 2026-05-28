package com.groute.groute_server.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * SocialAccount.email을 HMAC-SHA-256으로 단방향 해시한다.
 *
 * <p>로그인 식별은 (provider, provider_uid)로 처리되므로 email은 양방향 복호화가 필요 없고, 평문 노출을 피하기 위해 저장 전 해시값으로 치환한다.
 * 같은 pepper·같은 입력은 항상 같은 hex 문자열로 매핑되며, 마이그레이션 SQL과 동일한 알고리즘으로 처리된다.
 */
@Component
@RequiredArgsConstructor
public class SocialEmailHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final HexFormat HEX = HexFormat.of();

    private final SocialEmailHashProperties properties;

    /** null·빈 문자열은 null로 반환해 컬럼 nullable 동작을 유지한다. */
    public String hash(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(
                    new SecretKeySpec(
                            properties.pepper().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(email.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 hashing failed", e);
        }
    }
}
