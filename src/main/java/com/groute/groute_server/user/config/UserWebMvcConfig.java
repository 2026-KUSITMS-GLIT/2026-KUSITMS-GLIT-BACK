package com.groute.groute_server.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.groute.groute_server.user.interceptor.OnboardingCheckInterceptor;

import lombok.RequiredArgsConstructor;

/** 온보딩 인터셉터 등록. */
@Configuration
@RequiredArgsConstructor
public class UserWebMvcConfig implements WebMvcConfigurer {

    private final OnboardingCheckInterceptor onboardingCheckInterceptor;

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
