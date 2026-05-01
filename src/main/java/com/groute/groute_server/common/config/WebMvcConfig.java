package com.groute.groute_server.common.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.groute.groute_server.common.interceptor.OnboardingCheckInterceptor;
import com.groute.groute_server.common.resolver.CurrentUserArgumentResolver;

import lombok.RequiredArgsConstructor;

/**
 * Spring MVC 커스텀 argument resolver 등록.
 *
 * <p>{@link CurrentUserArgumentResolver}를 매핑해 {@code @CurrentUser Long userId} 파라미터가 컨트롤러 시그니처에서
 * 자연스럽게 동작하도록 한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final OnboardingCheckInterceptor onboardingCheckInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(onboardingCheckInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/onboarding/complete",
                        "/api/auth/**",
                        "/oauth2/**",
                        "/login/oauth2/code/**");
    }
}
