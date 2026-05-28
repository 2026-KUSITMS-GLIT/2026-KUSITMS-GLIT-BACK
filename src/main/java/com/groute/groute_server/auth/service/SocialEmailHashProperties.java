package com.groute.groute_server.auth.service;

import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SocialAccount.email HMAC 해시 시 사용하는 pepper 설정.
 *
 * <p>환경별 값은 SSM Parameter Store의 {@code /groute/{env}/SOCIAL_EMAIL_HASH_PEPPER}에서 주입된다.
 *
 * <p>HMAC-SHA-256의 키 강도를 위해 {@code pepper}는 32바이트 이상이어야 하며, 미충족 시 부팅 단계에서 실패한다. 한 번 정해진 pepper는 같은
 * 이메일을 항상 같은 해시값으로 매핑하므로 운영 중 변경하지 않는다.
 */
@Validated
@ConfigurationProperties(prefix = "social.email-hash")
public record SocialEmailHashProperties(
        @Size(min = 32, message = "social.email-hash.pepper는 HMAC-SHA-256 키로 32바이트 이상이어야 한다")
                String pepper) {}
