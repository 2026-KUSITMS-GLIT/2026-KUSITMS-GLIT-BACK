package com.groute.groute_server.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@link org.springframework.scheduling.annotation.Async @Async} 활성화 설정.
 *
 * <p>Spring Boot가 자동 구성한 {@code applicationTaskExecutor}를 기본 풀로 사용한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
