package com.groute.groute_server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase Cloud Messaging 부팅 설정.
 *
 * <p>{@code credentialsJson}이 비어 있으면 {@link com.google.firebase.FirebaseApp} 빈이 등록되지 않아 FCM 발송이
 * 비활성화된다(MYP-004 알림 발송 파이프라인). service account JSON은 SSM Parameter Store의 {@code
 * /groute/{env}/FIREBASE_CREDENTIALS_JSON}에서 주입되며, 배포 파이프라인이 컨테이너 환경변수 {@code
 * FIREBASE_CREDENTIALS_JSON}으로 변환한다. stg/prod는 누락 시 부팅 실패(fail-fast)로 설정 오류 즉시 노출, 로컬은 빈 문자열
 * fallback 허용.
 *
 * <p>{@code project_id} 등 식별자는 service account JSON 내부 필드에서 SDK가 자동 추출하므로 별도 설정값으로 두지 않는다.
 */
@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(String credentialsJson) {}
