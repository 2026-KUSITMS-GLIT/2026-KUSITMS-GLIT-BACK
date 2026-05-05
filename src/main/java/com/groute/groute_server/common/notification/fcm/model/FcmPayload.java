package com.groute.groute_server.common.notification.fcm.model;

/**
 * FCM 발송 페이로드(MYP-004).
 *
 * <p>{@code link}는 nullable — {@code null}이면 알림 클릭 동작은 클라이언트의 service worker 기본값에 위임한다. 카피·링크 분기
 * 정책은 호출자(스케줄러)가 결정하고 본 클라이언트는 주어진 값을 그대로 FCM에 실어 보낸다.
 *
 * @param title 알림 제목 (PWA 미리보기 헤드라인, 30자 이내 권장)
 * @param body 알림 본문 (60자 이내 권장)
 * @param link 클릭 시 이동할 절대 URL. {@code null}이면 미설정.
 */
public record FcmPayload(String title, String body, String link) {}
