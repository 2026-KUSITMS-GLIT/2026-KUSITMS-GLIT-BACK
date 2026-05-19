package com.groute.groute_server.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.groute.groute_server.auth.service.oauth.CustomOAuth2UserService;
import com.groute.groute_server.auth.service.oauth.OAuth2EnvAwareAuthorizationRequestResolver;
import com.groute.groute_server.auth.service.oauth.OAuth2LoginFailureHandler;
import com.groute.groute_server.auth.service.oauth.OAuth2LoginSuccessHandler;

import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 플로우 전용 Security 필터 체인.
 *
 * <p>{@code /oauth2/**} 및 {@code /login/oauth2/code/**} 경로에만 적용(@Order(1))된다. OAuth2 인가 코드
 * 교환·user-info 정규화는 {@link CustomOAuth2UserService}가, JWT 발급·응답은 {@link OAuth2LoginSuccessHandler}가
 * 담당한다. JWT 필터는 이 체인에 등록하지 않는다 — OAuth2 플로우 경로는 JWT 인증이 필요 없다.
 *
 * <p>프론트 시작 origin(env)은 {@link OAuth2EnvAwareAuthorizationRequestResolver}가 OAuth2 시작 시점에 쿠키로
 * 보존한다. SuccessHandler가 그 쿠키를 사용해 어떤 콜백 URL로 redirect할지 결정한다.
 *
 * <p>공통 JWT 체인은 {@code common/config/SecurityConfig} (@Order(2))가 담당한다.
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final AuthProperties authProperties;

    @Bean
    public OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new OAuth2EnvAwareAuthorizationRequestResolver(
                clientRegistrationRepository, authProperties);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver)
            throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/code/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(
                        oauth2 ->
                                oauth2.authorizationEndpoint(
                                                endpoint ->
                                                        endpoint.authorizationRequestResolver(
                                                                oAuth2AuthorizationRequestResolver))
                                        .userInfoEndpoint(
                                                userInfo ->
                                                        userInfo.userService(
                                                                customOAuth2UserService))
                                        .successHandler(oAuth2LoginSuccessHandler)
                                        .failureHandler(oAuth2LoginFailureHandler));
        return http.build();
    }
}
