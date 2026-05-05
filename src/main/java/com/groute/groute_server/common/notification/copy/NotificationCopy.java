package com.groute.groute_server.common.notification.copy;

import java.util.List;

/**
 * 부팅 시 1회 파싱되는 카피 풀(MYP-004).
 *
 * <p>구조: {@code {"copies":[{"title":"...","body":"..."}, ...]}}. 스케줄러는 user별 인덱스로 풀 안에서 1개를 골라
 * 라운드로빈 발송한다(기획 A). {@code title}/{@code body}에 포함된 {@code &#123;닉네임&#125;} 토큰은 발송 시점에
 * user.nickname으로 치환된다(기획 B).
 *
 * <p>풀이 비어 있으면(로컬 fallback) 발송 측에서 스킵 — 빈 풀 자체가 오류는 아니다.
 *
 * @param copies 카피 항목 리스트. 빈 리스트 가능.
 */
public record NotificationCopy(List<Item> copies) {

    /**
     * 카피 항목.
     *
     * <p>딥링크는 모든 카피에 동일하므로 항목 단위로 두지 않고 외부 상수({@link NotificationCopyProperties#deepLinkPath()})로
     * 분리한다.
     *
     * @param title 알림 제목 ({@code &#123;닉네임&#125;} 치환 토큰 포함 가능)
     * @param body 알림 본문 ({@code &#123;닉네임&#125;} 치환 토큰 포함 가능)
     */
    public record Item(String title, String body) {}
}
