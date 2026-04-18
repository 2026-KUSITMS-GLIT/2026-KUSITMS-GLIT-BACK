package com.groute.groute_server.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 시간 타입의 JSON 직렬화/역직렬화 계약을 정의한다.
 *
 * <h2>정책</h2>
 * <ul>
 *   <li>{@link java.time.OffsetDateTime} 등 JSR-310 타입을 ISO-8601 문자열로 직렬화한다.</li>
 *   <li>직렬화 타임존은 UTC로 고정한다. 배포 환경(OS/JVM)의 기본 타임존에 의존하지 않는다.</li>
 *   <li>타임스탬프(숫자) 직렬화는 비활성화한다. 가독성과 클라이언트 파싱 안정성을 우선한다.</li>
 * </ul>
 *
 * <h2>적용 범위</h2>
 * <ul>
 *   <li>응답 DTO에 노출되는 <b>raw {@code OffsetDateTime}</b> 필드는 여기서 정의한 포맷으로 내려간다.</li>
 *   <li>사람이 읽는 화면용 문자열(예: "2026.04.18", "04.18 15:45")은 본 설정이 아니라
 *       {@link com.groute.groute_server.common.util.DateTimeFormatters}에서 처리한다.</li>
 *   <li>요청 바디에 {@code OffsetDateTime}이 포함되면 동일 규약으로 역직렬화된다.
 *       쿼리 파라미터의 날짜는 {@link java.time.LocalDate}로 받으며 본 설정과 무관하다.</li>
 * </ul>
 *
 * <h2>참고</h2>
 * 본 클래스만으로는 JVM 기본 타임존을 UTC로 바꾸지 않는다.
 * {@code application.yaml}의 {@code spring.jackson.time-zone: UTC}가 함께 적용되어야 일관성이 보장된다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .timeZone(TimeZone.getTimeZone("UTC"));
    }
}
