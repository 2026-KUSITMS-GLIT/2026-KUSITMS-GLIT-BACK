package com.groute.groute_server.common.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

/**
 * JPA Auditing에서 {@link OffsetDateTime} 필드를 사용할 때 필요한 {@link DateTimeProvider} 구현.
 *
 * <p>Spring Data 기본 provider는 {@link java.time.LocalDateTime}을 반환하는데, 우리 {@code BaseTimeEntity}의
 * {@code @CreatedDate}/{@code @LastModifiedDate}는 {@link OffsetDateTime}이라 타입 불일치로 저장이 실패한다. 이
 * provider를 {@code @EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")}에 지정해 해소.
 *
 * <p>값은 UTC 기준으로 고정 — hibernate의 {@code jdbc.time_zone: UTC} 설정과 정렬되며, DB의 {@code TIMESTAMPTZ}에 일관된
 * UTC 타임스탬프가 기록된다.
 */
@Component("offsetDateTimeProvider")
public class OffsetDateTimeProvider implements DateTimeProvider {

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
