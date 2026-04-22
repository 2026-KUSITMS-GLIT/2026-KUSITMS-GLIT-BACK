package com.groute.groute_server.auth.service.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.entity.SocialAccount;
import com.groute.groute_server.auth.repository.SocialAccountRepository;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 인가 코드 교환 후 provider에서 받은 사용자 정보를 정규화하고 {@link User}/{@link SocialAccount}를 upsert한다.
 *
 * <p>흐름:
 *
 * <ol>
 *   <li>{@link DefaultOAuth2UserService}에 위임해 실제 user-info 호출 수행
 *   <li>{@link OAuthAttributes#from(String, java.util.Map)}으로 provider 응답 정규화
 *   <li>{@code (provider, providerUid)}로 {@link SocialAccount} 조회 — 있으면 email/lastLoginAt 업데이트, 없으면
 *       User·SocialAccount 신규 생성
 *   <li>{@link PrincipalUser}로 감싸 반환 (userId 탑재)
 * </ol>
 *
 * <p>신규 User는 닉네임·직군·상태를 모두 NULL로 둔 채 생성된다. 온보딩 이슈에서 이 세 필드를 채워 넣고 완료 판정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthAttributes attributes =
                OAuthAttributes.from(registrationId, oauth2User.getAttributes());

        User user = upsert(attributes);
        return new PrincipalUser(user.getId(), attributes.provider(), attributes.attributes());
    }

    private User upsert(OAuthAttributes attributes) {
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
        log.info(
                "신규 소셜 유저 생성: userId={}, provider={}, providerUid={}",
                user.getId(),
                attributes.provider(),
                attributes.providerUid());
        return user;
    }
}
