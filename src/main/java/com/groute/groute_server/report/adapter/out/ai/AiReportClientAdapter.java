package com.groute.groute_server.report.adapter.out.ai;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.adapter.out.ai.dto.AiCareerBrandingResponse;
import com.groute.groute_server.report.adapter.out.ai.dto.AiCareerHighlightsResponse;
import com.groute.groute_server.report.adapter.out.ai.dto.AiCareerNarrativeResponse;
import com.groute.groute_server.report.adapter.out.ai.dto.AiCareerStrengthsAndInterviewResponse;
import com.groute.groute_server.report.adapter.out.ai.dto.AiMiniReportResponse;
import com.groute.groute_server.report.adapter.out.ai.dto.AiReportRequest;
import com.groute.groute_server.report.adapter.out.ai.dto.AiReportRequest.RecordPeriod;
import com.groute.groute_server.report.adapter.out.ai.dto.AiReportRequest.ScrumsByDate;
import com.groute.groute_server.report.adapter.out.ai.dto.AiReportRequest.ScrumsByDate.ScrumItem;
import com.groute.groute_server.report.adapter.out.ai.dto.AiReportRequest.StarRecordItem;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;
import com.groute.groute_server.report.application.port.out.RequestAiReportPort;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link RequestAiReportPort}의 FastAPI 실 구현체.
 *
 * <p>MINI 타입은 {@code /api/reports/mini} 1회 호출, CAREER 타입은 branding / narrative / highlights /
 * strengths-and-interview 4개 순차 호출 후 결과를 합쳐 반환한다. 모든 요청에 {@code X-Internal-Token} 헤더를 포함한다.
 */
@Slf4j
@Component
public class AiReportClientAdapter implements RequestAiReportPort {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestClient restClient;
    private final LoadStarRecordPort loadStarRecordPort;

    public AiReportClientAdapter(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.internal-token}") String internalToken,
            LoadStarRecordPort loadStarRecordPort) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader("X-Internal-Token", internalToken)
                        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .build();
        this.loadStarRecordPort = loadStarRecordPort;
    }

    @Override
    public AiReportResult requestReportGeneration(
            Long reportId, List<StarRecord> starRecords, List<Scrum> scrums) {

        if (starRecords.isEmpty()) {
            log.warn("[AI Report] starRecords가 비어있음 — reportId={}", reportId);
            return new AiReportResult(null, Map.of());
        }

        User user = starRecords.get(0).getUser();
        ReportType reportType = resolveReportType(starRecords.size());

        // detailTags 조회
        List<Long> starRecordIds = starRecords.stream().map(StarRecord::getId).toList();
        Map<Long, List<String>> detailTagsMap =
                loadStarRecordPort.findDetailTagsByStarRecordIds(starRecordIds);

        AiReportRequest request = buildRequest(user, starRecords, scrums, detailTagsMap);

        if (reportType == ReportType.MINI) {
            return callMini(request);
        } else {
            return callCareer(request);
        }
    }

    // =========================================================
    // MINI
    // =========================================================

    private AiReportResult callMini(AiReportRequest request) {
        AiMiniReportResponse response =
                restClient
                        .post()
                        .uri("/api/reports/mini")
                        .body(request)
                        .retrieve()
                        .body(AiMiniReportResponse.class);

        Map<String, Object> contentJson = new HashMap<>();
        contentJson.put("activity_summary", response.activitySummary());
        contentJson.put("next_focus_point", response.nextFocusPoint());
        contentJson.put("competency_frequency", response.competencyFrequency());
        contentJson.put("top_detail_tags", response.topDetailTags());

        return new AiReportResult(null, contentJson);
    }

    // =========================================================
    // CAREER
    // =========================================================

    private AiReportResult callCareer(AiReportRequest request) {
        AiCareerBrandingResponse branding =
                restClient
                        .post()
                        .uri("/api/reports/career/branding")
                        .body(request)
                        .retrieve()
                        .body(AiCareerBrandingResponse.class);

        AiCareerNarrativeResponse narrative =
                restClient
                        .post()
                        .uri("/api/reports/career/narrative")
                        .body(request)
                        .retrieve()
                        .body(AiCareerNarrativeResponse.class);

        AiCareerHighlightsResponse highlights =
                restClient
                        .post()
                        .uri("/api/reports/career/highlights")
                        .body(request)
                        .retrieve()
                        .body(AiCareerHighlightsResponse.class);

        AiCareerStrengthsAndInterviewResponse strengthsAndInterview =
                restClient
                        .post()
                        .uri("/api/reports/career/strengths-and-interview")
                        .body(request)
                        .retrieve()
                        .body(AiCareerStrengthsAndInterviewResponse.class);

        Map<String, Object> contentJson = new HashMap<>();
        contentJson.put("branding_statement", branding.brandingStatement());
        contentJson.put("branding_pattern", branding.brandingPattern());
        contentJson.put("top_detail_tags", branding.topDetailTags());
        contentJson.put("narrative_summary", narrative.narrativeSummary());
        contentJson.put("experience_highlights", highlights.experienceHighlights());
        contentJson.put("strengths", strengthsAndInterview.strengths());
        contentJson.put("interview_questions", strengthsAndInterview.interviewQuestions());

        return new AiReportResult(branding.brandingStatement(), contentJson);
    }

    // =========================================================
    // Request 빌더
    // =========================================================

    private AiReportRequest buildRequest(
            User user,
            List<StarRecord> starRecords,
            List<Scrum> scrums,
            Map<Long, List<String>> detailTagsMap) {

        List<StarRecordItem> records =
                starRecords.stream().map(sr -> toStarRecordItem(sr, detailTagsMap)).toList();

        List<ScrumsByDate> scrumsByDate = groupScrumsByDate(scrums, starRecords);

        RecordPeriod recordPeriod = buildRecordPeriod(starRecords);

        return new AiReportRequest(
                user.getNickname(),
                user.getJobRole().name(),
                user.getUserStatus().name(),
                records,
                scrumsByDate,
                recordPeriod,
                starRecords.size());
    }

    private StarRecordItem toStarRecordItem(StarRecord sr, Map<Long, List<String>> detailTagsMap) {
        String completedAt =
                sr.getCompletedAt() != null
                        ? sr.getCompletedAt()
                                .atZoneSameInstant(KST)
                                .toLocalDate()
                                .format(DATE_FORMATTER)
                        : "";

        String competency =
                sr.getScrum().getSelectedCompetency() != null
                        ? sr.getScrum().getSelectedCompetency().name()
                        : "";

        List<String> detailTags = detailTagsMap.getOrDefault(sr.getId(), List.of());

        return new StarRecordItem(
                sr.getId(),
                sr.getSituationTask(),
                sr.getAction(),
                sr.getResult(),
                completedAt,
                competency,
                detailTags);
    }

    private List<ScrumsByDate> groupScrumsByDate(List<Scrum> scrums, List<StarRecord> starRecords) {
        // starRecordId → scrumId 맵 (스크럼에 연결된 STAR 기록 ID 역조회용)
        Map<Long, Long> scrumIdToStarRecordId =
                starRecords.stream()
                        .collect(Collectors.toMap(sr -> sr.getScrum().getId(), StarRecord::getId));

        Map<String, List<ScrumItem>> byDate = new HashMap<>();
        for (Scrum scrum : scrums) {
            String date = scrum.getScrumDate().format(DATE_FORMATTER);
            Long starRecordId = scrumIdToStarRecordId.get(scrum.getId());
            ScrumItem item =
                    new ScrumItem(
                            scrum.getTitle().getProject().getName(),
                            scrum.getTitle().getFreeText(),
                            scrum.getContent(),
                            starRecordId);
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(item);
        }

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ScrumsByDate(e.getKey(), e.getValue()))
                .toList();
    }

    private RecordPeriod buildRecordPeriod(List<StarRecord> starRecords) {
        String from =
                starRecords.stream()
                        .map(sr -> sr.getScrum().getScrumDate())
                        .min(Comparator.naturalOrder())
                        .map(d -> d.format(DATE_FORMATTER))
                        .orElse("");
        String to =
                starRecords.stream()
                        .map(sr -> sr.getScrum().getScrumDate())
                        .max(Comparator.naturalOrder())
                        .map(d -> d.format(DATE_FORMATTER))
                        .orElse("");
        return new RecordPeriod(from, to);
    }

    private ReportType resolveReportType(int starRecordCount) {
        return starRecordCount == 10 ? ReportType.MINI : ReportType.CAREER;
    }
}
