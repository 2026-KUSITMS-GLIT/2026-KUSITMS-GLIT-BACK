package com.groute.groute_server.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 Presigner 빈 설정.
 *
 * <p>{@code aws.s3.bucket}이 비어 있으면 빈이 등록되지 않는다(로컬 환경 부팅 허용). stg/prod는 SSM 미주입 시
 * application.yaml placeholder 해석 실패로 부팅이 막힌다(fail-fast).
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnExpression("!'${aws.s3.bucket:}'.isBlank()")
public class S3Config {

    private final S3Properties properties;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}