package com.groute.groute_server.common.notification.copy;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 카피 풀 부팅 초기화(MYP-004).
 *
 * <p>{@link NotificationCopyProperties#copyJson()} 문자열을 Jackson으로 파싱해 {@link NotificationCopy} 빈을
 * 1회 등록한다. 파싱 실패는 부팅을 막아 운영 환경에서 잘못된 SSM 값으로 무한 발송 실패가 누적되는 상황을 차단한다.
 *
 * <p>로컬 환경에서 {@code copyJson}이 비어 있으면 빈 풀로 부팅한다 — 발송 측이 빈 풀을 만나면 스킵하므로 정상 동작 (FCM 발송 비활성).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationCopyConfig {

    private final NotificationCopyProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 카피 풀 빈 등록.
     *
     * <p>1) JSON 비어 있으면 빈 풀 fallback. 2) JSON 있으면 파싱. 파싱 실패는 {@link IllegalStateException}으로 부팅
     * 차단(fail-fast).
     */
    @Bean
    public NotificationCopy notificationCopy() {
        // 1. 빈 문자열 fallback (로컬 부팅 허용)
        if (properties.copyJson() == null || properties.copyJson().isBlank()) {
            log.warn("notification.copy-json 미설정 — 빈 카피 풀로 부팅 (FCM 발송 비활성)");
            return new NotificationCopy(List.of());
        }

        // 2. JSON 파싱 (실패 시 부팅 차단)
        try {
            NotificationCopy copy =
                    objectMapper.readValue(properties.copyJson(), NotificationCopy.class);
            log.info("알림 카피 풀 로드 완료 (size={})", copy.copies().size());
            return copy;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "notification.copy-json 파싱 실패. SSM /groute/{env}/NOTIFICATION_COPY_JSON 형식 확인 필요"
                            + " — {\"copies\":[{\"title\":\"...\",\"body\":\"...\"},...]}",
                    e);
        }
    }
}
