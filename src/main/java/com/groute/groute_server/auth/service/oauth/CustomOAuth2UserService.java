package com.groute.groute_server.auth.service.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.groute.groute_server.auth.service.SocialLoginService;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 인가 코드 교환 후 provider에서 받은 사용자 정보를 정규화하고 DB upsert를 {@link SocialLoginService}에 위임한다.
 *
 * <p>흐름:
 *
 * <ol>
 *   <li>{@link DefaultOAuth2UserService}에 위임해 실제 user-info 호출 수행 (HTTP I/O, 트랜잭션 밖)
 *   <li>{@link OAuthAttributes#from(String, java.util.Map)}으로 provider 응답 정규화
 *   <li>{@link SocialLoginService#upsertUser(OAuthAttributes)}로 DB 반영 (짧은 트랜잭션)
 *   <li>{@link PrincipalUser}로 감싸 반환 (userId 탑재)
 * </ol>
 *
 * <p>트랜잭션을 이 메서드 전체가 아니라 upsert 단계에만 좁힌 이유: provider 응답 지연이 DB 커넥션 점유 시간을 늘려 풀 고갈로 전파되지 않게 하기 위함.
 *
 * <p>신규 User는 닉네임·직군·상태를 모두 NULL로 둔 채 생성된다. 온보딩 이슈에서 이 세 필드를 채워 넣고 완료 판정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final SocialLoginService socialLoginService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        try {
            OAuthAttributes attributes =
                    OAuthAttributes.from(registrationId, oauth2User.getAttributes());
            User user = socialLoginService.upsertUser(attributes);
            return new PrincipalUser(user.getId(), attributes.provider(), attributes.attributes());
        } catch (BusinessException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorCode().getCode()), e.getMessage(), e);
        }
    }
}
