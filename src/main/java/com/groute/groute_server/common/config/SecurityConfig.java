package com.groute.groute_server.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.groute.groute_server.common.filter.JwtAuthenticationFilter;
import com.groute.groute_server.common.handler.JwtAccessDeniedHandler;
import com.groute.groute_server.common.handler.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 공통 JWT 필터 체인 설정.
 *
 * <p>OAuth2 로그인 플로우는 {@code auth/config/OAuth2SecurityConfig} (@Order(1))가 담당하며, 본 체인은 @Order(2)로 그
 * 외 모든 API 요청에 적용된다. 인증이 필요 없는 경로({@link #PUBLIC_ENDPOINTS})를 제외한 모든 요청은 인증을 요구한다. {@link
 * JwtAuthenticationFilter}가 Authorization 헤더의 access 토큰을 검증해 SecurityContext를 채운다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/oauth2/**",
        "/login/oauth2/code/**",
        "/api/auth/reissue",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/docs/error-code",
        "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
