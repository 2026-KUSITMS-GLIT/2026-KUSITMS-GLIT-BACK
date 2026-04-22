package com.groute.groute_server.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.groute.groute_server.auth.service.oauth.CustomOAuth2UserService;
import com.groute.groute_server.auth.service.oauth.OAuth2LoginFailureHandler;
import com.groute.groute_server.auth.service.oauth.OAuth2LoginSuccessHandler;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 설정.
 *
 * <p>인증이 필요 없는 경로({@link #PUBLIC_ENDPOINTS})를 제외한 모든 요청은 인증을 요구한다. OAuth2 로그인 플로우는 {@link
 * CustomOAuth2UserService}가 user-info 정규화·upsert를, {@link OAuth2LoginSuccessHandler}가 JWT 발급·응답을
 * 담당한다. JwtAuthenticationFilter 결합은 후속 커밋.
 *
 * <p>세션: OAuth2 인가 코드 플로우의 state 유지 목적으로만 잠시 사용되며, SuccessHandler에서 즉시 invalidate. API 인증은
 * JWT-only.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/oauth2/**",
        "/login/oauth2/code/**",
        "/api/auth/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/docs/error-code",
        "/actuator/**"
    };

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(
                        oauth2 ->
                                oauth2.userInfoEndpoint(
                                                userInfo ->
                                                        userInfo.userService(
                                                                customOAuth2UserService))
                                        .successHandler(oAuth2LoginSuccessHandler)
                                        .failureHandler(oAuth2LoginFailureHandler));

        return http.build();
    }

    /**
     * CORS 전역 설정.
     *
     * <p>로컬 개발은 Vite(5173)/Next·CRA(3000), 운영은 {@code https://glit.today}에서 호출. {@code
     * allowCredentials(true)}는 refresh 쿠키 동작에 필수.
     */
    private static final List<String> ALLOWED_ORIGINS =
            List.of("http://localhost:3000", "http://localhost:5173", "https://glit.today");

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
