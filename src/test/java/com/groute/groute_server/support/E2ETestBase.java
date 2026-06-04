package com.groute.groute_server.support;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
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
 * <p>PostgreSQL·Redis·LocalStack S3 컨테이너와 AI·OAuth2용 MockWebServer를 static 초기화 블록에서 직접 기동한다.
 * {@code @Container}를 사용하지 않으므로 Testcontainers가 클래스 종료 시 컨테이너를 중지하지 않는다 — 여러 테스트 클래스가 동일 컨테이너를
 * 재사용해도 포트가 바뀌지 않는다.
 *
 * <p>각 테스트 메서드 실행 전 {@code TRUNCATE TABLE users CASCADE}와 Redis {@code FLUSHALL}로 상태를 초기화한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.placeholders.social_email_hash_pepper="
                    + "test-pepper-must-be-at-least-32-bytes-for-hmac-sha256",
            "aws.s3.bucket=test-bucket",
            "aws.s3.region=us-east-1",
            "aws.s3.presigned-url-expiration-minutes=5",
            "aws.s3.cdn-base-url=http://localhost",
            "spring.main.allow-bean-definition-overriding=true",
        })
@Import(E2ETestBase.S3TestConfig.class)
public abstract class E2ETestBase {

    // ── 컨테이너 — @Container 없이 수동 기동 (클래스 종료 시 자동 중지 방지) ──────────────────

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final LocalStackContainer LOCALSTACK;
    protected static final MockWebServer AI_MOCK;
    protected static final MockWebServer OAUTH_MOCK;

    static {
        try {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();

            REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
            REDIS.start();

            LOCALSTACK =
                    new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"));
            LOCALSTACK.start();
            LOCALSTACK.execInContainer("awslocal", "s3", "mb", "s3://test-bucket");

            AI_MOCK = new MockWebServer();
            AI_MOCK.start();
            OAUTH_MOCK = new MockWebServer();
            OAUTH_MOCK.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── DynamicPropertySource ────────────────────────────────────────────────────

    @DynamicPropertySource
    static void overrideExternalServices(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("aws.s3.endpoint", () -> "http://localhost:" + LOCALSTACK.getMappedPort(4566));
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

    // ── 인스턴스 헬퍼 ────────────────────────────────────────────────────────────

    @LocalServerPort protected int port;

    @Autowired protected JwtTokenProvider jwtTokenProvider;

    @Autowired protected TestRestTemplate restTemplate;

    @Autowired protected JdbcTemplate jdbcTemplate;

    @Autowired protected RedisConnectionFactory redisConnectionFactory;

    /** OAuth2 플로우용 — 쿠키(세션) 유지, redirect 비활성. */
    protected final TestRestTemplate cookieRest =
            new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);

    /**
     * OAuth2 state 캡처용 — HttpURLConnection의 기본 redirect 동작을 비활성화.
     *
     * <p>@Autowired TestRestTemplate과 TestRestTemplate(ENABLE_COOKIES) 모두
     * SimpleClientHttpRequestFactory 또는 Apache HttpClient가 redirect를 따라가므로, HttpURLConnection 레벨에서
     * 직접 비활성화한다.
     */
    protected final RestTemplate noRedirectRest = buildNoRedirectRest();

    private static RestTemplate buildNoRedirectRest() {
        var rest =
                new RestTemplate(
                        new SimpleClientHttpRequestFactory() {
                            @Override
                            protected void prepareConnection(
                                    HttpURLConnection connection, String httpMethod)
                                    throws IOException {
                                super.prepareConnection(connection, httpMethod);
                                connection.setInstanceFollowRedirects(false);
                            }
                        });
        rest.setErrorHandler(
                new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(
                            org.springframework.http.client.ClientHttpResponse response) {
                        return false;
                    }
                });
        return rest;
    }

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

    // ── 공통 HTTP 헬퍼 ────────────────────────────────────────────────────────────

    protected HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    protected HttpHeaders jsonAuthHeaders(String accessToken) {
        HttpHeaders headers = authHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected static String queryParam(String url, String name) {
        int q = url.indexOf('?');
        if (q < 0) return null;
        for (String kv : url.substring(q + 1).split("&")) {
            String[] pair = kv.split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    protected static String sessionCookie(List<String> setCookieHeaders) {
        if (setCookieHeaders == null) return null;
        return setCookieHeaders.stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .map(c -> c.split(";")[0].trim())
                .findFirst()
                .orElse(null);
    }

    protected static <T> T jsonRead(String json, String path, Class<T> type) {
        return com.jayway.jsonpath.JsonPath.parse(json).read(path, type);
    }

    // ── OAuth2 로그인 헬퍼 ────────────────────────────────────────────────────────

    /**
     * 카카오 OAuth2 플로우로 신규 유저를 생성하고 access token을 반환한다.
     *
     * <p>OAUTH_MOCK에 token 교환·user-info 응답을 enqueue하고 noRedirectRest로 콜백까지 수행한다.
     */
    protected String loginAsNewKakaoUser(long kakaoId) {
        var initResp =
                noRedirectRest.getForEntity(url("/oauth2/authorization/kakao"), String.class);
        String state = queryParam(initResp.getHeaders().getLocation().toString(), "state");
        String session = sessionCookie(initResp.getHeaders().get("Set-Cookie"));

        OAUTH_MOCK.enqueue(
                new okhttp3.mockwebserver.MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"access_token\":\"kakao-mock\",\"token_type\":\"Bearer\","
                                        + "\"expires_in\":3600}"));
        OAUTH_MOCK.enqueue(
                new okhttp3.mockwebserver.MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"id\":"
                                        + kakaoId
                                        + ",\"kakao_account\":{\"email\":\"test"
                                        + kakaoId
                                        + "@kakao.com\"}}"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", session);
        var callbackResp =
                noRedirectRest.exchange(
                        url("/login/oauth2/code/kakao?code=fake&state=" + state),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        return queryParam(callbackResp.getHeaders().getLocation().toString(), "access");
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
