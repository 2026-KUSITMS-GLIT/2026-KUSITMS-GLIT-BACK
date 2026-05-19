package com.groute.groute_server.home.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.home.repository.CompetencyStatsQueryRepository;
import com.groute.groute_server.home.repository.CompetencyStatsQueryRepository.DateCountRow;

import lombok.RequiredArgsConstructor;

/**
 * 홈 역량 잔디 조회 서비스(HOM-002).
 *
 * <p>요청 월의 일자별 STAR 완료 건수를 집계한다. {@code YearMonth → [월 첫날, 다음달 첫날)} 반열림 구간으로 변환해 repository에 위임하며,
 * KST 기준 {@code Scrum.scrumDate}를 그대로 사용해 DB UTC 변환과 무관하다.
 *
 * <p>서비스 시그니처는 도메인 원시값(Map)만 노출한다 — DTO ↔ 도메인 변환은 컨트롤러 레이어 책임(코드래빗 Layered 룰: service 리턴에 Response
 * DTO 노출 금지).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCompetencyStatsService {

    private final CompetencyStatsQueryRepository competencyStatsQueryRepository;

    /**
     * 사용자의 월별 STAR 완료 건수를 일자별로 집계한다.
     *
     * <p>결과는 STAR 완료 1건 이상인 일자만 키로 갖는 sparse map. 0건 일자는 키 자체가 누락되며 컨트롤러 단에서 응답 변환 시 NO_DATA로 채운다.
     * 동일 일자 중복 키는 repository GROUP BY로 이미 제거되어 발생하지 않으나, 향후 쿼리 변경에 대비해 첫 항목을 유지하는 merge 함수를 두어
     * {@code IllegalStateException} 회귀를 방어한다.
     *
     * @param userId 조회 대상 사용자 ID
     * @param month 조회 월(KST)
     * @return 일자(KST) → STAR 완료 건수 맵. 0건 일자는 미포함.
     */
    public Map<LocalDate, Long> getCompletedStarCountsByMonth(Long userId, YearMonth month) {
        LocalDate startInclusive = month.atDay(1);
        LocalDate endExclusive = month.plusMonths(1).atDay(1);
        return competencyStatsQueryRepository
                .findCompletedStarCountsByUserAndDateRange(userId, startInclusive, endExclusive)
                .stream()
                .collect(
                        Collectors.toMap(
                                DateCountRow::getDate,
                                DateCountRow::getCount,
                                (existing, ignored) -> existing));
    }
}
