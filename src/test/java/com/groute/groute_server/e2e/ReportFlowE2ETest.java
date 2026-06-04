package com.groute.groute_server.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.groute.groute_server.support.E2ETestBase;

import okhttp3.mockwebserver.MockResponse;

/**
 * 리포트 플로우 E2E 테스트.
 *
 * <p>10개 심화기록 완료(MINI 임계치) → 게이지·선택정보 조회 → MINI 리포트 생성(AI 모킹) → 폴링 → 상세·목록 조회 순서로 검증한다.
 *
 * <p>BulkCreateStarRecordService 제약: 한 번의 bulk 선택에서 스크럼은 동일 날짜여야 한다. 10개를 채우려면 날짜별로 나누어 5개씩 2회
 * 호출한다.
 */
class ReportFlowE2ETest extends E2ETestBase {

    @Test
    void 리포트_생성_폴링_상세_조회() throws Exception {
        // Setup: 신규 유저 로그인 + 온보딩
        String token = loginAsNewKakaoUser(33333L);
        restTemplate.exchange(
                url("/api/onboarding/complete"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"nickname\":\"리포터\",\"jobRole\":\"개발자\",\"userStatus\":\"취업 준비 중\"}",
                        jsonAuthHeaders(token)),
                String.class);

        // 프로젝트 생성
        var projectResp =
                restTemplate.exchange(
                        url("/api/projects"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"name\":\"리포트프로젝트\"}", jsonAuthHeaders(token)),
                        String.class);
        Long projectId = jsonRead(projectResp.getBody(), "$.data.projectId", Long.class);

        // 10개 심화기록 완료 — 날짜별 5개씩 2회 bulk (같은 날짜 제약)
        List<Long> starIds = createTenCompletedStars(token, projectId);
        assertThat(starIds).hasSize(10);

        // 1. 리포트 게이지
        var gaugeResp =
                restTemplate.exchange(
                        url("/api/reports/gauge"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(gaugeResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long currentCount = jsonRead(gaugeResp.getBody(), "$.data.currentCount", Long.class);
        assertThat(currentCount).isGreaterThanOrEqualTo(10L);

        // 2. 리포트 생성용 사전 정보 조회
        var selectableResp =
                restTemplate.exchange(
                        url("/api/reports/selectable-info"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(selectableResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 3. MINI 리포트 생성 (AI mock 먼저 등록)
        AI_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"activitySummary\":\"개발 작업 수행\","
                                        + "\"nextFocusPoint\":\"협업 역량 강화\","
                                        + "\"competencyFrequency\":[{\"competency\":\"DISCOVERY_ANALYSIS\",\"count\":10}],"
                                        + "\"topDetailTags\":[\"분석\",\"문제 해결\"]}"));

        StringBuilder starIdsJson = new StringBuilder("[");
        for (int i = 0; i < starIds.size(); i++) {
            if (i > 0) starIdsJson.append(",");
            starIdsJson.append(starIds.get(i));
        }
        starIdsJson.append("]");
        var createResp =
                restTemplate.exchange(
                        url("/api/reports"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"reportType\":\"MINI\",\"starRecordIds\":" + starIdsJson + "}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long reportId = jsonRead(createResp.getBody(), "$.data.reportId", Long.class);

        // 4. 리포트 생성 상태 폴링 (최대 10초)
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(
                        () -> {
                            String body =
                                    restTemplate
                                            .exchange(
                                                    url("/api/reports/" + reportId + "/status"),
                                                    HttpMethod.GET,
                                                    new HttpEntity<>(authHeaders(token)),
                                                    String.class)
                                            .getBody();
                            return "SUCCESS".equals(jsonRead(body, "$.data.status", String.class));
                        });

        // 5. 리포트 상세 조회 — MINI 타입 확인
        var detailResp =
                restTemplate.exchange(
                        url("/api/reports/" + reportId),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(jsonRead(detailResp.getBody(), "$.data.reportType", String.class))
                .isEqualTo("MINI");

        // 6. 리포트 목록 조회
        var listResp =
                restTemplate.exchange(
                        url("/api/reports"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(listResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<?> reports = jsonRead(listResp.getBody(), "$.data.reports", List.class);
        assertThat(reports).isNotEmpty();
    }

    @Test
    void 리포트_AI_실패_재시도_성공() throws Exception {
        // Setup
        String token = loginAsNewKakaoUser(44444L);
        restTemplate.exchange(
                url("/api/onboarding/complete"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"nickname\":\"재시도유저\",\"jobRole\":\"개발자\",\"userStatus\":\"취업 준비 중\"}",
                        jsonAuthHeaders(token)),
                String.class);
        var projectResp =
                restTemplate.exchange(
                        url("/api/projects"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"name\":\"재시도프로젝트\"}", jsonAuthHeaders(token)),
                        String.class);
        Long projectId = jsonRead(projectResp.getBody(), "$.data.projectId", Long.class);
        List<Long> starIds = createTenCompletedStars(token, projectId);

        // 1차 AI 호출 → 500 실패 등록
        AI_MOCK.enqueue(new MockResponse().setResponseCode(500));

        StringBuilder starIdsJson = new StringBuilder("[");
        for (int i = 0; i < starIds.size(); i++) {
            if (i > 0) starIdsJson.append(",");
            starIdsJson.append(starIds.get(i));
        }
        starIdsJson.append("]");
        var createResp =
                restTemplate.exchange(
                        url("/api/reports"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"reportType\":\"MINI\",\"starRecordIds\":" + starIdsJson + "}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long reportId = jsonRead(createResp.getBody(), "$.data.reportId", Long.class);

        // FAILED 상태 대기
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(
                        () -> {
                            String body =
                                    restTemplate
                                            .exchange(
                                                    url("/api/reports/" + reportId + "/status"),
                                                    HttpMethod.GET,
                                                    new HttpEntity<>(authHeaders(token)),
                                                    String.class)
                                            .getBody();
                            return "FAILED".equals(jsonRead(body, "$.data.status", String.class));
                        });

        // retryAvailable 확인
        var failedStatusResp =
                restTemplate.exchange(
                        url("/api/reports/" + reportId + "/status"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(jsonRead(failedStatusResp.getBody(), "$.data.retryAvailable", Boolean.class))
                .isTrue();

        // 재시도용 AI mock 등록 후 retry
        AI_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"activitySummary\":\"재시도 성공\","
                                        + "\"nextFocusPoint\":\"계속 성장\","
                                        + "\"competencyFrequency\":[{\"competency\":\"DISCOVERY_ANALYSIS\",\"count\":10}],"
                                        + "\"topDetailTags\":[\"분석\"]}"));
        var retryResp =
                restTemplate.exchange(
                        url("/api/reports/" + reportId + "/retry"),
                        HttpMethod.POST,
                        new HttpEntity<>(null, authHeaders(token)),
                        String.class);
        assertThat(retryResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 재시도 후 SUCCESS 대기
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(
                        () -> {
                            String body =
                                    restTemplate
                                            .exchange(
                                                    url("/api/reports/" + reportId + "/status"),
                                                    HttpMethod.GET,
                                                    new HttpEntity<>(authHeaders(token)),
                                                    String.class)
                                            .getBody();
                            return "SUCCESS".equals(jsonRead(body, "$.data.status", String.class));
                        });
    }

    /**
     * 10개 완료된 심화기록을 생성한다.
     *
     * <p>BulkCreateStarRecordService는 한 번의 bulk 선택에서 동일 날짜 스크럼만 허용하므로 날짜별 5개씩 2회 나누어 호출한다.
     */
    private List<Long> createTenCompletedStars(String token, Long projectId) {
        List<Long> allStarIds = new ArrayList<>();

        for (String date : new String[] {"2026-06-03", "2026-06-04"}) {
            // 스크럼 5개 저장
            StringBuilder scrums = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i > 0) scrums.append(",");
                scrums.append("{\"content\":\"scrum").append(i + 1).append("\"}");
            }
            var writeResp =
                    restTemplate.exchange(
                            url("/api/scrums/write"),
                            HttpMethod.POST,
                            new HttpEntity<>(
                                    "{\"date\":\""
                                            + date
                                            + "\",\"scrumsByTitle\":[{\"projectId\":"
                                            + projectId
                                            + ",\"freeText\":\"group\",\"scrums\":["
                                            + scrums
                                            + "]}]}",
                                    jsonAuthHeaders(token)),
                            String.class);
            List<?> scrumList = jsonRead(writeResp.getBody(), "$.data[0].scrums", List.class);
            List<Long> dateScrumIds = new ArrayList<>();
            for (Object s : scrumList) {
                dateScrumIds.add(((Number) ((Map<?, ?>) s).get("scrumId")).longValue());
            }

            // 같은 날짜 스크럼 5개로 StarRecord 생성 (동일 날짜 제약 충족)
            StringBuilder items = new StringBuilder("[");
            for (int i = 0; i < dateScrumIds.size(); i++) {
                if (i > 0) items.append(",");
                items.append("{\"scrumId\":").append(dateScrumIds.get(i)).append("}");
            }
            items.append("]");
            var bulkResp =
                    restTemplate.exchange(
                            url("/api/star-records/bulk"),
                            HttpMethod.POST,
                            new HttpEntity<>("{\"items\":" + items + "}", jsonAuthHeaders(token)),
                            String.class);
            List<?> starItems = jsonRead(bulkResp.getBody(), "$.data.items", List.class);
            List<Long> dateStarIds = new ArrayList<>();
            for (Object item : starItems) {
                dateStarIds.add(((Number) ((Map<?, ?>) item).get("starRecordId")).longValue());
            }

            // 역량 선택
            StringBuilder competencies = new StringBuilder("[");
            for (int i = 0; i < dateScrumIds.size(); i++) {
                if (i > 0) competencies.append(",");
                competencies
                        .append("{\"scrumId\":")
                        .append(dateScrumIds.get(i))
                        .append(",\"competency\":\"DISCOVERY_ANALYSIS\"}");
            }
            competencies.append("]");
            restTemplate.exchange(
                    url("/api/scrums/competencies"),
                    HttpMethod.PATCH,
                    new HttpEntity<>("{\"items\":" + competencies + "}", jsonAuthHeaders(token)),
                    String.class);

            // S·T / A / R 단계 저장 → 자동 완료
            for (Long starId : dateStarIds) {
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/steps/situation-task"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"userAnswer\":\"situation\"}", jsonAuthHeaders(token)),
                        String.class);
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/steps/action"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"userAnswer\":\"action\"}", jsonAuthHeaders(token)),
                        String.class);
                restTemplate.exchange(
                        url("/api/star-records/" + starId + "/steps/result"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"userAnswer\":\"result\"}", jsonAuthHeaders(token)),
                        String.class);
            }

            allStarIds.addAll(dateStarIds);
        }

        return allStarIds;
    }
}
