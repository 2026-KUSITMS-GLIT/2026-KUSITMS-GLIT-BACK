package com.groute.groute_server.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 비동기·스케줄링 인프라 활성화 설정.
 *
 * <p>{@link org.springframework.scheduling.annotation.Async @Async}와 {@link
 * org.springframework.scheduling.annotation.Scheduled @Scheduled}를 동시에 활성화한다. Spring Boot가 자동 구성한
 * {@code applicationTaskExecutor}를 기본 풀로 사용한다.
 *
 * <p>{@link Clock} 빈은 시간 의존 로직(스케줄러의 KST 매칭 등)이 단위 테스트에서 시각을 주입할 수 있도록 노출한다. 운영에서는 UTC 시스템 시계를
 * 사용한다.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 시간 의존 로직 단위 테스트 용이성을 위한 {@link Clock} 빈.
     *
     * <p>운영 서버는 UTC로 운용하므로 zone을 명시적으로 UTC로 박아 환경 추정에 의존하지 않는다 ({@link Clock#systemDefaultZone()}은
     * JVM {@code user.timezone}에 따라 zone이 달라져 코드 추론을 어렵게 한다). 도메인별 시각 변환은 호출 측에서 {@code
     * clock.withZone(...)}으로 명시 처리한다 (예: 스케줄러는 {@code Asia/Seoul}로 변환 후 매칭).
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
