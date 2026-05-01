package com.groute.groute_server.user.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 온보딩 미완료 유저의 API 접근 차단 인터셉터.
 *
 * <p>인증된 유저 중 {@code nickname}이 NULL인 경우(온보딩 미완료) {@link ErrorCode#ONBOARDING_NOT_COMPLETED}를 던진다.
 * 비인증 요청은 Security 레이어에서 처리하므로 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class OnboardingCheckInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof Long userId)) {
            return true;
        }

        userRepository
                .findById(userId)
                .filter(user -> user.getNickname() == null)
                .ifPresent(
                        user -> {
                            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
                        });

        return true;
    }
}
