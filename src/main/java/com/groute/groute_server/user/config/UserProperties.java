package com.groute.groute_server.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사용자 도메인 런타임 설정.
 *
 * <p>{@code defaultProfileImageUrl}은 모든 유저에게 공통으로 노출되는 기본 캐릭터 프로필 이미지 URL이다. 환경별 값은 SSM Parameter
 * Store의 {@code /groute/{env}/USER_DEFAULT_PROFILE_IMAGE_URL}에서 주입되며, 로컬은 env 미설정 시 빈 문자열로
 * fallback된다 (프론트가 빈값 대체 이미지 처리 책임).
 *
 * <p>{@code hardDeleteGraceDays}는 회원 탈퇴 후 물리 삭제까지의 grace period(MYP-005). 기획상 30일 고정이지만 인시던트 대응·테스트
 * 단축 목적으로 yaml 한 줄로 조정 가능하도록 외부화한다. 0 이하는 부팅 단계에서 거부된다.
 */
@ConfigurationProperties(prefix = "app.user")
public record UserProperties(String defaultProfileImageUrl, int hardDeleteGraceDays) {

    public UserProperties {
        if (hardDeleteGraceDays <= 0) {
            throw new IllegalStateException(
                    "app.user.hard-delete-grace-days는 0보다 커야 합니다 (현재값: "
                            + hardDeleteGraceDays
                            + ")");
        }
    }
}
