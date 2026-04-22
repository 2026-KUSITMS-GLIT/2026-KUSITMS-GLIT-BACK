package com.groute.groute_server.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * 인증된 사용자 정보를 컨트롤러 파라미터로 주입받는 마커 어노테이션.
 *
 * <p>{@link io.swagger.v3.oas.annotations.Parameter @Parameter(hidden = true)}를 메타 어노테이션으로 달아,
 * springdoc이 해당 파라미터를 요청 스펙으로 문서화하지 않는다. 클라이언트 입장에서는 {@code Authorization: Bearer <token>} 헤더만 보내면
 * 되고, Swagger UI의 Try it out에도 유령 필드가 나타나지 않는다.
 *
 * <p>실제 주입은 {@code CurrentUserArgumentResolver}가 {@link
 * org.springframework.security.core.context.SecurityContextHolder}에 저장된 principal(userId)을 꺼내 수행한다.
 * 파라미터 타입은 {@link Long}이어야 한다.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * public UserResponse me(@CurrentUser Long userId) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface CurrentUser {}
