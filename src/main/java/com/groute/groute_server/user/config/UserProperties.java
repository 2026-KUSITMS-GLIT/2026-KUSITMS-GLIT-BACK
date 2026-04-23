package com.groute.groute_server.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사용자 도메인 런타임 설정.
 *
 * <p>{@code defaultProfileImageUrl}은 모든 유저에게 공통으로 노출되는 기본 캐릭터 프로필 이미지 URL이다. 환경별 값은 SSM Parameter
 * Store의 {@code /groute/{env}/USER_DEFAULT_PROFILE_IMAGE_URL}에서 주입되며, 로컬은 env 미설정 시 빈 문자열로
 * fallback된다 (프론트가 빈값 대체 이미지 처리 책임).
 */
@ConfigurationProperties(prefix = "app.user")
public record UserProperties(String defaultProfileImageUrl) {}
