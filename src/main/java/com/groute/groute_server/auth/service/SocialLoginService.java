package com.groute.groute_server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.entity.SocialAccount;
import com.groute.groute_server.auth.repository.SocialAccountRepository;
import com.groute.groute_server.auth.service.oauth.OAuthAttributes;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소셜 계정 upsert 전담 서비스.
 *
 * <p>provider user-info 호출(네트워크 I/O)과 분리해 DB 트랜잭션만 짧게 유지한다. {@link
 * com.groute.groute_server.auth.service.oauth.CustomOAuth2UserService}가 외부 호출·정규화까지 맡고, upsert는
 * 이쪽으로 위임해 HTTP 지연이 DB 커넥션 점유로 번지지 않도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public User upsertUser(OAuthAttributes attributes) {
        return socialAccountRepository
                .findByProviderAndProviderUid(attributes.provider(), attributes.providerUid())
                .map(existing -> updateReturning(existing, attributes))
                .orElseGet(() -> createNew(attributes));
    }

    private User updateReturning(SocialAccount existing, OAuthAttributes attributes) {
        existing.updateEmail(attributes.email());
        User user = existing.getUser();
        user.recordLogin();
        return user;
    }

    private User createNew(OAuthAttributes attributes) {
        User user = userRepository.save(User.createForSocialLogin());
        user.recordLogin();
        SocialAccount account =
                SocialAccount.create(
                        user, attributes.provider(), attributes.providerUid(), attributes.email());
        socialAccountRepository.save(account);
        log.info("신규 소셜 유저 생성: userId={}, provider={}", user.getId(), attributes.provider());
        return user;
    }
}
