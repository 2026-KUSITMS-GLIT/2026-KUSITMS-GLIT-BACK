package com.groute.groute_server.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.groute.groute_server.support.E2ETestBase;

import okhttp3.mockwebserver.MockResponse;

/**
 * 심화기록 플로우 E2E 테스트.
 *
 * <p>프로젝트 생성 → 스크럼 저장 → 심화기록 선택 → 역량 선택 → S·T/A/이미지/R 작성 → AI 태깅 → 상세 조회 순서로 검증한다.
 */
class RecordFlowE2ETest extends E2ETestBase {

    @Test
    void 프로젝트_스크럼_STAR_이미지_AI태깅_플로우() throws Exception {
        // Setup: 신규 유저 로그인 + 온보딩
        String token = loginAsNewKakaoUser(22222L);
        restTemplate.exchange(
                url("/api/onboarding/complete"),
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                        {"nickname":"기록자","jobRole":"개발자","userStatus":"취업 준비 중"}
                        """,
                        jsonAuthHeaders(token)),
                String.class);

        // 1. 프로젝트 생성
        var projectResp =
                restTemplate.exchange(
                        url("/api/projects"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"name\":\"테스트프로젝트\"}", jsonAuthHeaders(token)),
                        String.class);
        assertThat(projectResp.getStatusCode().value()).isIn(200, 201);
        Long projectId = jsonRead(projectResp.getBody(), "$.data.projectId", Long.class);

        // 2. 스크럼 일괄 저장 (PENDING 상태)
        String scrumWriteBody =
                """
                {"date":"2026-06-05","scrumsByTitle":[
                  {"projectId":%d,"freeText":"기획작업","scrums":[{"content":"API 설계 작업"}]}
                ]}
                """
                        .formatted(projectId);
        var scrumWriteResp =
                restTemplate.exchange(
                        url("/api/scrums/write"),
                        HttpMethod.POST,
                        new HttpEntity<>(scrumWriteBody, jsonAuthHeaders(token)),
                        String.class);
        assertThat(scrumWriteResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long scrumId =
                jsonRead(scrumWriteResp.getBody(), "$.data[0].scrums[0].scrumId", Long.class);

        // 3. 심화기록할 스크럼 선택 → StarRecord 생성
        var bulkCreateResp =
                restTemplate.exchange(
                        url("/api/star-records/bulk"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"items\":[{\"scrumId\":" + scrumId + "}]}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(bulkCreateResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long starId =
                jsonRead(bulkCreateResp.getBody(), "$.data.items[0].starRecordId", Long.class);

        // 4. 역량 선택
        var competencyResp =
                restTemplate.exchange(
                        url("/api/scrums/competencies"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(
                                "{\"items\":[{\"scrumId\":"
                                        + scrumId
                                        + ",\"competency\":\"DISCOVERY_ANALYSIS\"}]}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(competencyResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 5. S·T 단계 저장
        restTemplate.exchange(
                url("/api/star-records/" + starId + "/steps/situation-task"),
                HttpMethod.POST,
                new HttpEntity<>("{\"userAnswer\":\"상황과 과업 내용\"}", jsonAuthHeaders(token)),
                String.class);

        // 6. A 단계 저장
        restTemplate.exchange(
                url("/api/star-records/" + starId + "/steps/action"),
                HttpMethod.POST,
                new HttpEntity<>("{\"userAnswer\":\"액션 내용\"}", jsonAuthHeaders(token)),
                String.class);

        // 7. 이미지 업로드 (R 단계 전) — presigned URL 발급 → LocalStack S3 PUT → confirm
        var presignedResp =
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/images/presigned-url"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"mimeTypes\":[\"image/jpeg\"]}", jsonAuthHeaders(token)),
                        String.class);
        assertThat(presignedResp.getStatusCode().is2xxSuccessful()).isTrue();
        String presignedUrl =
                jsonRead(presignedResp.getBody(), "$.data[0].presignedUrl", String.class);
        String imageKey = jsonRead(presignedResp.getBody(), "$.data[0].imageKey", String.class);

        // virtual-hosted presignedUrl(test-bucket.localhost:PORT)은 DNS 해석 불가 —
        // LocalStack이 지원하는 path-style(localhost:PORT/test-bucket/key)로 직접 PUT
        String localstackPutUrl =
                "http://localhost:" + LOCALSTACK.getMappedPort(4566) + "/test-bucket/" + imageKey;
        HttpHeaders s3Headers = new HttpHeaders();
        s3Headers.setContentType(MediaType.IMAGE_JPEG);
        noRedirectRest.exchange(
                localstackPutUrl,
                HttpMethod.PUT,
                new HttpEntity<>(new byte[] {1, 2, 3}, s3Headers),
                String.class);

        restTemplate.exchange(
                url("/api/star-records/" + starId + "/images/confirm"),
                HttpMethod.POST,
                new HttpEntity<>("{\"imageKeys\":[\"" + imageKey + "\"]}", jsonAuthHeaders(token)),
                String.class);

        // 8. R 단계 저장 → StarRecord 자동 완료, ScrumTitle COMMITTED 전환
        var rResp =
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/steps/result"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"userAnswer\":\"결과 내용\"}", jsonAuthHeaders(token)),
                        String.class);
        assertThat(rResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 9. AI 태깅 트리거 (mock 응답 먼저 등록)
        AI_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"primaryCategory\":\"DISCOVERY_ANALYSIS\","
                                        + "\"detailTags\":[\"데이터 분석\",\"문제 정의\"]}"));
        var triggerResp =
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/ai-tagging"),
                        HttpMethod.POST,
                        new HttpEntity<>(null, authHeaders(token)),
                        String.class);
        assertThat(triggerResp.getStatusCode().value()).isEqualTo(201);

        // 10. AI 태깅 완료 대기 (비동기 — 최대 10초 폴링)
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(
                        () -> {
                            String body =
                                    restTemplate
                                            .exchange(
                                                    url(
                                                            "/api/star-records/"
                                                                    + starId
                                                                    + "/ai-tagging/status"),
                                                    HttpMethod.GET,
                                                    new HttpEntity<>(authHeaders(token)),
                                                    String.class)
                                            .getBody();
                            return "SUCCESS".equals(jsonRead(body, "$.data.status", String.class));
                        });

        // 11. AI 태깅 결과 검증
        var resultResp =
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/ai-tagging/result"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(jsonRead(resultResp.getBody(), "$.data.primaryCategory", String.class))
                .isEqualTo("DISCOVERY_ANALYSIS");
        assertThat(jsonRead(resultResp.getBody(), "$.data.detailTags[0]", String.class))
                .isEqualTo("데이터 분석");

        // 12. 심화기록 상세 조회 — 내용·이미지 확인
        var detailResp =
                restTemplate.exchange(
                        url("/api/star-records/" + starId),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(jsonRead(detailResp.getBody(), "$.data.situationTask", String.class))
                .isEqualTo("상황과 과업 내용");
        List<?> images = jsonRead(detailResp.getBody(), "$.data.images", List.class);
        assertThat(images).hasSize(1);
    }
}
