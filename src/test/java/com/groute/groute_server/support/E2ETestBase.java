package com.groute.groute_server.support;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.groute.groute_server.common.jwt.JwtTokenProvider;

import okhttp3.mockwebserver.MockWebServer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * E2E 플로우 테스트 공통 베이스.
 *
 * <p>PostgreSQL·Redis·LocalStack S3 컨테이너와 AI·OAuth2용 MockWebServer를 static으로 공유한다. 모든 외부 의존(S3, AI,
 * OAuth2)은 프로덕션 코드 변경 없이 @DynamicPropertySource + @TestConfiguration @Primary 빈으로 대체한다.
 *
 * <p>각 테스트 메서드 실행 전 {@code TRUNCATE TABLE users CASCADE}와 Redis {@code FLUSHALL}로 상태를 초기화한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.placeholders.social_email_hash_pepper="
                    + "test-pepper-must-be-at-least-32-bytes-for-hmac-sha256",
            "aws.s3.bucket=test-bucket",
            "aws.s3.region=us-east-1",
            "aws.s3.presigned-url-expiration-minutes=5",
            "aws.s3.cdn-base-url=http://localhost",
        })
@Import(E2ETestBase.S3TestConfig.class)
public abstract class E2ETestBase {

    // ── Testcontainers (static = 전 테스트 클래스 공유) ──────────────────────────────

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack"))
                    .withServices(Service.S3);

    // ── MockWebServer (static 초기화 블록으로 기동) ───────────────────────────────────

    /** AI FastAPI 모킹 — POST /v1/tagging, /v1/reports/mini 등 */
    static final MockWebServer AI_MOCK;

    /** OAuth2 프로바이더 모킹 — token-uri, user-info-uri */
    static final MockWebServer OAUTH_MOCK;

    static {
        try {
            AI_MOCK = new MockWebServer();
            AI_MOCK.start();
            OAUTH_MOCK = new MockWebServer();
            OAUTH_MOCK.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── DynamicPropertySource ────────────────────────────────────────────────────

    @DynamicPropertySource
    static void overrideExternalServices(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add(
                "aws.s3.endpoint", () -> LOCALSTACK.getEndpointOverride(Service.S3).toString());
        registry.add("ai.base-url", () -> "http://localhost:" + AI_MOCK.getPort());
        String oauthBase = "http://localhost:" + OAUTH_MOCK.getPort();
        registry.add(
                "spring.security.oauth2.client.provider.kakao.token-uri",
                () -> oauthBase + "/oauth/token");
        registry.add(
                "spring.security.oauth2.client.provider.kakao.user-info-uri",
                () -> oauthBase + "/v2/user/me");
        registry.add(
                "spring.security.oauth2.client.provider.google.token-uri",
                () -> oauthBase + "/oauth/token");
        registry.add(
                "spring.security.oauth2.client.provider.google.user-info-uri",
                () -> oauthBase + "/userinfo");
    }

    // ── S3 버킷 초기화 ────────────────────────────────────────────────────────────

    @BeforeAll
    static void createS3Bucket() throws IOException, InterruptedException {
        // 이미 존재해도 무시 — execInContainer는 non-zero exit에 예외를 던지지 않음
        LOCALSTACK.execInContainer("awslocal", "s3", "mb", "s3://test-bucket");
    }

    // ── 인스턴스 헬퍼 ────────────────────────────────────────────────────────────

    @LocalServerPort protected int port;

    @Autowired protected JwtTokenProvider jwtTokenProvider;

    /** redirect를 따라가지 않는 기본 클라이언트 (TestRestTemplate 기본값). */
    @Autowired protected TestRestTemplate restTemplate;

    @Autowired protected JdbcTemplate jdbcTemplate;

    @Autowired protected RedisConnectionFactory redisConnectionFactory;

    /** OAuth2 플로우용 — 쿠키(세션) 유지, redirect 비활성. */
    protected final TestRestTemplate cookieRest =
            new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);

    @BeforeEach
    void cleanState() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        redisConnectionFactory.getConnection().serverCommands().flushAll();
    }

    protected String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    protected String rawRefreshToken(Long userId) {
        return jwtTokenProvider.createRefreshToken(userId);
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ── S3 빈 교체 (LocalStack 엔드포인트) ───────────────────────────────────────

    /**
     * S3Config의 DefaultCredentialsProvider 빈을 LocalStack용으로 교체한다.
     *
     * <p>프로덕션 S3Config 빈도 생성되지만 @Primary 우선순위로 이 빈이 사용된다. 프로덕션 코드는 변경하지 않는다.
     */
    @TestConfiguration
    static class S3TestConfig {

        @Value("${aws.s3.endpoint}")
        private String endpoint;

        @Value("${aws.s3.region}")
        private String region;

        @Bean
        @Primary
        S3Presigner s3Presigner() {
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")))
                    .build();
        }

        @Bean
        @Primary
        S3Client s3Client() {
            return S3Client.builder()
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")))
                    .build();
        }
    }
}
