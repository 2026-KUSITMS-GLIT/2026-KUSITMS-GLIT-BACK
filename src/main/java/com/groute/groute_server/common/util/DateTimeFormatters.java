package com.groute.groute_server.common.util;

import java.time.ZoneId;

/**
 * 사람이 읽는 시간 문자열 포맷을 한 곳에서 관리하는 유틸리티.
 *
 * <h2>포지셔닝</h2>
 * <ul>
 *   <li>엔티티/DB에 저장되는 시간은 {@link java.time.OffsetDateTime} UTC를 기준으로 한다.</li>
 *   <li>응답 DTO에 <b>raw {@code OffsetDateTime}</b> 필드를 노출하는 경우,
 *       직렬화는 {@link com.groute.groute_server.common.config.JacksonConfig}가 ISO-8601 UTC로 처리한다.
 *       (예: 클라이언트가 직접 상대시간("2시간 전")을 계산하는 케이스)</li>
 *   <li>응답 DTO에 <b>사람이 읽는 문자열</b>(예: "2026.04.18", "04.18 15:45")을 내려야 하는 경우,
 *       서비스/어셈블러 레이어에서 본 클래스의 포맷터/헬퍼를 사용해 {@code String}으로 채운다.</li>
 * </ul>
 *
 * <h2>사용 위치</h2>
 * 서비스 레이어 또는 DTO 어셈블러에서만 호출한다.
 * 엔티티와 컨트롤러는 시간 포맷팅에 관여하지 않는다.
 *
 * <h2>타임존 변환 책임</h2>
 * UTC로 저장된 시간을 KST 표현으로 변환하는 로직은 본 클래스 내부에서 처리한다.
 * 호출부는 UTC/KST 변환을 신경 쓰지 않고 "어떤 포맷으로 보여줄지"만 결정한다.
 *
 * <h2>현재 상태</h2>
 * 응답 DTO 작업 전이므로 실제 포맷터 상수/메서드는 비어 있다.
 * 클라이언트 요구 패턴이 확정되는 대로 이곳에 상수와 헬퍼 메서드를 추가한다.
 * (예: {@code KST_DATE_DOT}, {@code KST_DATE_TIME_DOT}, {@code toKstDate(OffsetDateTime)} 등)
 */
public final class DateTimeFormatters {

    /**
     * 한국 서비스 기준 표시용 타임존.
     * UTC로 저장된 시간을 KST로 환산할 때 모든 포맷터/헬퍼가 공통으로 사용한다.
     */
    public static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    private DateTimeFormatters() {
    }
}
