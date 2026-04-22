package com.groute.groute_server.common.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

/**
 * {@link CurrentUser} 파라미터에 인증된 사용자 id를 주입.
 *
 * <p>{@code JwtAuthenticationFilter}가 {@link SecurityContextHolder}의 principal로 {@link Long
 * userId}를 세팅하므로, 이 resolver는 그 값을 그대로 꺼낸다. 인증이 없거나 익명이면 {@link ErrorCode#UNAUTHORIZED}.
 *
 * <p>파라미터 타입이 {@link Long}인 경우에만 동작 — 다른 타입에 {@code @CurrentUser}가 달리면 resolver가 건너뛰어 Spring이 다른
 * 방식으로 해결하려다 실패하므로, 잘못된 사용이 컴파일 이후 단계에서 드러난다.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
