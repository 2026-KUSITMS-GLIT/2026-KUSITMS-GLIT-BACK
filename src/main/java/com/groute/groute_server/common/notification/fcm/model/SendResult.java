package com.groute.groute_server.common.notification.fcm.model;

/**
 * FCM 발송 결과.
 *
 * <p>{@code tokenInvalid=true}는 토큰이 영구적으로 무효(앱 삭제·기기 초기화 등)임을 의미하므로 호출자는 해당 토큰을 비활성화해야 한다. 일시
 * 오류(네트워크/쿼터 등)는 {@code success=false, tokenInvalid=false}로 분류돼 토큰은 보존된다.
 *
 * @param success FCM이 메시지를 수락했는지 여부
 * @param tokenInvalid 토큰이 영구 무효라 비활성화 대상인지 여부
 */
public record SendResult(boolean success, boolean tokenInvalid) {}
