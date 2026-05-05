package com.groute.groute_server.common.notification.copy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 알림 카피·딥링크 설정(MYP-004).
 *
 * <p>{@code copyJson}은 SSM Parameter Store {@code /groute/{env}/NOTIFICATION_COPY_JSON}에서 주입되는 JSON
 * 문자열로, 부팅 시 {@link NotificationCopy}로 파싱돼 카피 풀(라운드로빈 대상)을 구성한다. stg/prod는 누락 시 부팅 실패(fail-fast),
 * 로컬은 빈 문자열 fallback 허용 (FCM 발송 비활성).
 *
 * <p>{@code deepLinkUrl}은 알림 클릭 시 이동할 절대 URL이다(스킴+도메인+path 일체). SSM {@code
 * /groute/{env}/NOTIFICATION_DEEP_LINK_URL}에서 환경별 주입된다(dev/prod 따로). 기획 C — 모든 카피 동일하게 스크럼 작성
 * 화면(REC-002)으로 이동.
 */
@ConfigurationProperties(prefix = "notification")
public record NotificationCopyProperties(String copyJson, String deepLinkUrl) {}
