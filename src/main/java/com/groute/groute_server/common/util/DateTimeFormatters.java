package com.groute.groute_server.common.util;

import java.time.ZoneId;

/** KST 타임존 단일 출처. UTC로 저장된 시간을 한국 표시 기준으로 변환할 때 공통으로 참조한다. */
public final class DateTimeFormatters {

    public static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    private DateTimeFormatters() {}
}
