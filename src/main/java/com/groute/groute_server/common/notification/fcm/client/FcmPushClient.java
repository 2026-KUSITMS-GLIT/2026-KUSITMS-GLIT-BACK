package com.groute.groute_server.common.notification.fcm.client;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.groute.groute_server.common.notification.fcm.model.FcmPayload;
import com.groute.groute_server.common.notification.fcm.model.SendResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FCM Web Push 단일 발송 클라이언트(MYP-004).
 *
 * <p>{@link FirebaseMessaging} 빈이 등록돼 있지 않으면(로컬 개발 환경에서 자격증명 미설정) 발송을 건너뛰고 {@code success=false,
 * tokenInvalid=false}를 반환해 호출자가 토큰을 비활성화하지 않도록 한다.
 *
 * <p>토큰 무효 분류 규칙:
 *
 * <ul>
 *   <li>{@code UNREGISTERED} / {@code SENDER_ID_MISMATCH} — 항상 {@code tokenInvalid=true}.
 *   <li>{@code INVALID_ARGUMENT} — 토큰 필드 오류와 페이로드 필드 오류 모두 같은 코드로 떨어지므로, 응답 BadRequest의 {@code
 *       message.token} fieldViolation을 확인했을 때만 {@code tokenInvalid=true}로 분류. 페이로드 오류로 인한 잘못된 비활성화를
 *       방지.
 *   <li>그 외 일시 오류(쿼터/네트워크 등) — 토큰 보존 ({@code tokenInvalid=false}).
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushClient {

    private final Optional<FirebaseMessaging> firebaseMessaging;

    /**
     * 단일 토큰으로 푸시 메시지를 발송한다.
     *
     * <p>호출자는 결과의 {@code tokenInvalid} 플래그를 보고 device_tokens 비활성화 여부를 결정한다. 빈 부재 환경(로컬)에서는 발송을 시도하지
     * 않고 graceful 실패로 응답해 호출자가 토큰 상태를 임의로 변경하지 않게 한다.
     *
     * @param token 대상 디바이스 토큰
     * @param payload 알림 페이로드(제목/본문/딥링크)
     * @return 발송 결과 — {@code (success, tokenInvalid)}
     */
    public SendResult send(String token, FcmPayload payload) {
        // 1. FirebaseMessaging 빈 부재 → 발송 스킵 (로컬 fallback)
        if (firebaseMessaging.isEmpty()) {
            log.debug("FirebaseMessaging 빈 부재 — FCM 발송 스킵 (token={})", maskToken(token));
            return new SendResult(false, false);
        }

        // 2. 메시지 빌드 (link 유무에 따라 WebpushConfig 분기)
        Message message = buildMessage(token, payload);

        // 3. FCM 호출 + 응답 기반 에러 분류
        try {
            firebaseMessaging.get().send(message);
            return new SendResult(true, false);
        } catch (FirebaseMessagingException e) {
            boolean tokenInvalid = isTokenInvalid(e);
            log.warn(
                    "FCM 발송 실패 (errorCode={}, tokenInvalid={}, token={})",
                    e.getMessagingErrorCode(),
                    tokenInvalid,
                    maskToken(token));
            return new SendResult(false, tokenInvalid);
        }
    }

    /**
     * 페이로드를 FCM {@link Message} 객체로 변환한다.
     *
     * <p>{@code link}가 있을 때만 {@link WebpushConfig}에 {@link WebpushFcmOptions}를 끼워 클릭 시 PWA가 해당 URL로
     * 이동하도록 한다. 그 외 케이스는 클라 service worker의 기본 클릭 핸들러에 위임한다.
     */
    private Message buildMessage(String token, FcmPayload payload) {
        // 1. 기본 알림(title/body) 빌드
        Message.Builder builder =
                Message.builder()
                        .setToken(token)
                        .setNotification(
                                Notification.builder()
                                        .setTitle(payload.title())
                                        .setBody(payload.body())
                                        .build());

        // 2. link가 있으면 WebpushConfig에 fcmOptions 추가 (PWA 클릭 시 이동 경로)
        if (payload.link() != null) {
            builder.setWebpushConfig(
                    WebpushConfig.builder()
                            .setFcmOptions(WebpushFcmOptions.withLink(payload.link()))
                            .build());
        }

        // 3. 빌드된 메시지 반환
        return builder.build();
    }

    /**
     * FCM 예외가 토큰 영구 무효(앱 삭제/기기 초기화/잘못된 토큰 형식 등)에 해당하는지 판정한다.
     *
     * <p>{@code UNREGISTERED}와 {@code SENDER_ID_MISMATCH}는 토큰 자체가 더 이상 유효하지 않다는 명확한 신호다. {@code
     * INVALID_ARGUMENT}는 토큰 필드 오류와 페이로드 필드 오류 모두에서 발생하므로, 응답 본문의 BadRequest fieldViolation이 {@code
     * message.token}을 가리킬 때만 토큰 무효로 분류한다 — 페이로드 오류에 따른 잘못된 토큰 비활성화 방지.
     *
     * @param e FCM 발송 예외
     * @return 토큰을 비활성화 대상으로 분류해야 하면 {@code true}
     */
    private boolean isTokenInvalid(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == null) {
            return false;
        }
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return true;
        }
        if (code == MessagingErrorCode.INVALID_ARGUMENT) {
            return mentionsTokenField(e);
        }
        return false;
    }

    /**
     * {@code INVALID_ARGUMENT} 응답이 토큰 필드 오류인지 판정한다. HTTP 응답 본문의 BadRequest fieldViolation에 {@code
     * message.token}이 포함됐는지를 단순 문자열 매칭으로 확인한다 — 무거운 JSON 파싱 회피.
     */
    private boolean mentionsTokenField(FirebaseMessagingException e) {
        if (e.getHttpResponse() != null) {
            String content = e.getHttpResponse().getContent();
            if (content != null && content.contains("message.token")) {
                return true;
            }
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("registration token")
                || lower.contains("invalid token")
                || lower.contains("message.token");
    }

    /** 로그에 토큰 전체를 노출하지 않기 위해 앞 6 + 뒤 4만 남기고 가운데를 마스킹한다. */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
