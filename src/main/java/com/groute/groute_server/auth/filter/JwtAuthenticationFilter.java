package com.groute.groute_server.auth.filter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.groute.groute_server.auth.jwt.JwtTokenProvider;
import com.groute.groute_server.auth.jwt.JwtValidationResult;
import com.groute.groute_server.auth.jwt.TokenType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매 요청 {@code Authorization: Bearer <token>} 헤더의 access 토큰을 파싱·검증해 {@link SecurityContextHolder}에
 * Authentication을 주입.
 *
 * <p>토큰이 없거나 유효하지 않으면 SecurityContext를 비워둔 채 필터 체인을 그대로 흘려보낸다. 이후 authorization 단계에서 보호 경로라면 {@link
 * org.springframework.security.web.AuthenticationEntryPoint}로 위임되어 401이 나가고, permitAll 경로라면 그대로
 * 통과한다. 즉 이 필터는 401을 직접 만들지 않는다.
 *
 * <p>refresh 토큰을 Authorization 헤더로 보내는 오용을 막기 위해 {@link TokenType#ACCESS}인 토큰만 수용한다. 재발급은 별도
 * 엔드포인트(쿠키·본문 경유)에서 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<SimpleGrantedAuthority> DEFAULT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                if (jwtTokenProvider.validate(token) == JwtValidationResult.VALID
                        && jwtTokenProvider.getTokenType(token) == TokenType.ACCESS) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, DEFAULT_AUTHORITIES);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException e) {
                log.debug("JWT 처리 중 예외 무시하고 미인증으로 진행: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
