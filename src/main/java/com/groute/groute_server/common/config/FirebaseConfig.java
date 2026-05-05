package com.groute.groute_server.common.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Admin SDK 부팅 초기화.
 *
 * <p>{@link FirebaseProperties#credentialsJson()}로 받은 service account JSON을 {@link
 * GoogleCredentials}로 변환해 {@link FirebaseApp}을 등록한다. {@code project_id}는 자격증명 내부 필드에서 SDK가 자동 추출한다.
 *
 * <p>{@code firebase.credentials-json}이 빈 문자열이면 본 빈은 등록되지 않으며(로컬 환경 부팅 허용), FCM 발송 측에서 {@link
 * FirebaseApp} 빈 부재를 감지해 발송을 건너뛴다. stg/prod는 SSM 미주입 시 application.yaml의 placeholder 해석 실패로 부팅이
 * 막힌다(fail-fast).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnExpression("!'${firebase.credentials-json:}'.isBlank()")
public class FirebaseConfig {

    private final FirebaseProperties properties;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (ByteArrayInputStream stream =
                new ByteArrayInputStream(
                        properties.credentialsJson().getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp 초기화 완료 (project={})", app.getOptions().getProjectId());
            return app;
        }
    }
}
