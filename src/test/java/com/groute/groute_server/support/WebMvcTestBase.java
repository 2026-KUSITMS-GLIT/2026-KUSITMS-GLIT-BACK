package com.groute.groute_server.support;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.common.config.OffsetDateTimeProvider;
import com.groute.groute_server.common.config.SecurityConfig;
import com.groute.groute_server.common.config.WebMvcConfig;
import com.groute.groute_server.common.filter.JwtAuthenticationFilter;
import com.groute.groute_server.common.handler.JwtAccessDeniedHandler;
import com.groute.groute_server.common.handler.JwtAuthenticationEntryPoint;
import com.groute.groute_server.common.jwt.JwtTokenProvider;
import com.groute.groute_server.common.resolver.CurrentUserArgumentResolver;
import com.groute.groute_server.common.webhook.ErrorWebhookNotifier;
import com.groute.groute_server.user.service.UserService;

/**
 * {@code @WebMvcTest} 슬라이스 공통 베이스.
 *
 * <p>실제 {@link SecurityConfig}·{@link JwtAuthenticationFilter}를 로드하되, {@link JwtTokenProvider}만 모킹해
 * 필터가 실행되더라도 SecurityContext를 비운 채로 둔다. 인증이 필요한 테스트는 {@link #auth(Long)}로 {@code Authentication}을
 * 직접 주입한다.
 *
 * <p>{@link OAuth2SecurityConfig}는 임포트하지 않는다 — OAuth2 체인은 {@code /oauth2/**}에만 적용되며 해당 의존 빈
 * (CustomOAuth2UserService 등)을 모킹하면 복잡도가 증가한다.
 */
@Import({
    OffsetDateTimeProvider.class,
    SecurityConfig.class,
    WebMvcConfig.class,
    CurrentUserArgumentResolver.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
public abstract class WebMvcTestBase {

    @MockitoBean protected JwtTokenProvider jwtTokenProvider;
    @MockitoBean protected JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean protected ErrorWebhookNotifier errorWebhookNotifier;
    @MockitoBean protected UserService userService;

    @Autowired protected MockMvc mockMvc;

    @BeforeEach
    void setUpOnboardingDefault() {
        given(userService.isOnboardingCompleted(anyLong())).willReturn(true);
    }

    @Autowired protected ObjectMapper objectMapper;

    protected static RequestPostProcessor auth(Long userId) {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
