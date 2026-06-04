package com.groute.groute_server.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.groute.groute_server.support.E2ETestBase;

import okhttp3.mockwebserver.MockResponse;

/**
 * 캘린더 플로우 E2E 테스트.
 *
 * <p>스크럼 1개 + STAR 완료(COMMITTED) → 월별·날짜별 조회 → STAR 삭제 → 스크럼 Sync → 스크럼·타이틀 삭제 순서로 검증한다.
 *
 * <p>캘린더 조회는 COMMITTED ScrumTitle만 노출하므로, 테스트 전 R 단계까지 저장해 COMMITTED 상태로 만든다.
 */
class CalendarFlowE2ETest extends E2ETestBase {

    private static final String TEST_DATE = "2026-06-03";

    @Test
    void 캘린더_조회_STAR삭제_스크럼Sync_삭제() throws Exception {
        // Setup: 신규 유저 로그인 + 온보딩
        String token = loginAsNewKakaoUser(55555L);
        restTemplate.exchange(
                url("/api/onboarding/complete"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"nickname\":\"캘린더유저\",\"jobRole\":\"개발자\",\"userStatus\":\"취업 준비 중\"}",
                        jsonAuthHeaders(token)),
                String.class);

        // 프로젝트 생성
        var projectResp =
                restTemplate.exchange(
                        url("/api/projects"),
                        HttpMethod.POST,
                        new HttpEntity<>("{\"name\":\"캘린더프로젝트\"}", jsonAuthHeaders(token)),
                        String.class);
        Long projectId = jsonRead(projectResp.getBody(), "$.data.projectId", Long.class);

        // 스크럼 1개 저장
        var writeResp =
                restTemplate.exchange(
                        url("/api/scrums/write"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"date\":\""
                                        + TEST_DATE
                                        + "\",\"scrumsByTitle\":[{\"projectId\":"
                                        + projectId
                                        + ",\"freeText\":\"캘린더작업\",\"scrums\":[{\"content\":\"scrum1\"}]}]}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(writeResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long scrumId =
                ((Number)
                                ((Map<?, ?>)
                                                ((List<?>)
                                                                jsonRead(
                                                                        writeResp.getBody(),
                                                                        "$.data[0].scrums",
                                                                        List.class))
                                                        .get(0))
                                        .get("scrumId"))
                        .longValue();

        // StarRecord 생성 → 역량 선택 → S·T/A/R 단계 저장 → COMMITTED
        var bulkResp =
                restTemplate.exchange(
                        url("/api/star-records/bulk"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"items\":[{\"scrumId\":" + scrumId + "}]}",
                                jsonAuthHeaders(token)),
                        String.class);
        Long starId = jsonRead(bulkResp.getBody(), "$.data.items[0].starRecordId", Long.class);

        restTemplate.exchange(
                url("/api/scrums/competencies"),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        "{\"items\":[{\"scrumId\":"
                                + scrumId
                                + ",\"competency\":\"DISCOVERY_ANALYSIS\"}]}",
                        jsonAuthHeaders(token)),
                String.class);
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
        // ScrumTitle → COMMITTED (R 단계 저장 시 자동 전환)

        // AI 태깅 — primaryCategory 없으면 CalendarHomeService.getDailyPreview NPE 발생
        AI_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                "{\"primaryCategory\":\"DISCOVERY_ANALYSIS\","
                                        + "\"detailTags\":[\"분석\"]}"));
        restTemplate.exchange(
                url("/api/star-records/" + starId + "/ai-tagging"),
                HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(token)),
                String.class);
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

        // 1. 월별 캘린더 조회 — 06-03에 데이터 존재 확인
        var monthlyResp =
                restTemplate.exchange(
                        url("/api/calendar/monthly?month=2026-06"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(monthlyResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<?> days = jsonRead(monthlyResp.getBody(), "$.data.days", List.class);
        assertThat(days).isNotEmpty();

        // 2. 날짜별 프리뷰 — 06-03에 STAR 완료 카드 존재
        var previewResp =
                restTemplate.exchange(
                        url("/api/calendar/daily-preview?date=" + TEST_DATE),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(previewResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<?> previewTitles = jsonRead(previewResp.getBody(), "$.data.titles", List.class);
        assertThat(previewTitles).isNotEmpty();

        // 3. 일자별 상세 조회 — titleId / scrumId / starRecordId 추출
        var dailyResp =
                restTemplate.exchange(
                        url("/api/calendar/daily?date=" + TEST_DATE),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(dailyResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<?> groups = jsonRead(dailyResp.getBody(), "$.data.groups", List.class);
        assertThat(groups).isNotEmpty();
        Long titleId = ((Number) ((Map<?, ?>) groups.get(0)).get("titleId")).longValue();
        List<?> items = (List<?>) ((Map<?, ?>) groups.get(0)).get("items");
        Long dailyScrumId = ((Number) ((Map<?, ?>) items.get(0)).get("scrumId")).longValue();
        Long dailyStarId = ((Number) ((Map<?, ?>) items.get(0)).get("starRecordId")).longValue();

        // 4. 심화기록 단독 삭제 (CAL-003)
        var deleteStarResp =
                restTemplate.exchange(
                        url("/api/star-records/" + dailyStarId),
                        HttpMethod.DELETE,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(deleteStarResp.getStatusCode().is2xxSuccessful()).isTrue();

        // STAR 삭제 후 프리뷰에서 hasStar 변화 확인 (groups 여전히 존재, STAR 없음)
        var previewAfterDelete =
                restTemplate.exchange(
                        url("/api/calendar/daily-preview?date=" + TEST_DATE),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(previewAfterDelete.getStatusCode().is2xxSuccessful()).isTrue();
        // ScrumTitle은 COMMITTED로 유지 — titles 배열은 여전히 존재
        assertThat(jsonRead(previewAfterDelete.getBody(), "$.data.titles", List.class))
                .isNotEmpty();

        // 5. 스크럼 Sync — STAR 삭제 후 hasStar=false 이므로 scrum 수정 가능
        var syncResp =
                restTemplate.exchange(
                        url("/api/scrums/daily?date=" + TEST_DATE),
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                "{\"groups\":[{\"titleId\":"
                                        + titleId
                                        + ",\"items\":[{\"scrumId\":"
                                        + dailyScrumId
                                        + ",\"content\":\"synced content\"}]}]}",
                                jsonAuthHeaders(token)),
                        String.class);
        assertThat(syncResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 6. 스크럼 단독 삭제
        var deleteScrumResp =
                restTemplate.exchange(
                        url("/api/scrums/" + dailyScrumId),
                        HttpMethod.DELETE,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(deleteScrumResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 7. 스크럼 타이틀 단위 삭제 (빈 ScrumTitle)
        var deleteTitleResp =
                restTemplate.exchange(
                        url("/api/scrums/titles/" + titleId),
                        HttpMethod.DELETE,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(deleteTitleResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 삭제 후 일자별 조회 — 빈 상태 확인
        var emptyDailyResp =
                restTemplate.exchange(
                        url("/api/calendar/daily?date=" + TEST_DATE),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(token)),
                        String.class);
        assertThat(emptyDailyResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(jsonRead(emptyDailyResp.getBody(), "$.data.groups", List.class)).isEmpty();
    }
}
