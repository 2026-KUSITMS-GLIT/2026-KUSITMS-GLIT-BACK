package com.groute.groute_server.auth.service.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.groute.groute_server.auth.enums.SocialProvider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 성공 후 {@code SecurityContext}에 주입되는 principal.
 *
 * <p>userId를 1급 필드로 보유해 성공 핸들러/필터가 JWT 발급·검증 시 바로 꺼낼 수 있도록 한다. {@link #getName()}은 JWT subject와 동일한
 * 의미를 가지도록 userId 문자열을 반환.
 */
@Getter
@RequiredArgsConstructor
public class PrincipalUser implements OAuth2User {

    private static final Collection<? extends GrantedAuthority> DEFAULT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final Long userId;
    private final SocialProvider provider;
    private final Map<String, Object> attributes;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return DEFAULT_AUTHORITIES;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
