package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.record.adapter.out.persistence.ScrumJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * 일자별 스크럼 작성 여부 조회(MYP-004 알림 발송 작성자 제외 처리용 outbound).
 *
 * <p>record 모듈은 정통 hexagonal(in/out port + adapter)이지만 본 서비스는 단일 use case 외부 노출용으로 port 없이 평범한
 * {@code @Service}로 둔다. 호출자(notification 스케줄러)는 record 내부 구조를 모르고 본 빈만 의존하면 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrumDailyQueryService {

    private final ScrumJpaRepository scrumJpaRepository;

    /**
     * 후보 user 중 KST 기준 해당 일자에 스크럼을 1개 이상 작성한 user_id 집합 반환.
     *
     * <p>스케줄러는 매칭 user 모음에서 본 결과를 빼고 발송한다(기획 D-1 — 이미 작성한 유저 스킵).
     *
     * @param kstDate 비교 기준 날짜(KST). DB의 {@code scrum_date}는 사용자가 선택한 KST 날짜라 변환 없이 직접 비교.
     * @param candidates 발송 후보 user_id 집합. 비어 있으면 빈 Set 반환(쿼리 스킵).
     * @return 작성자 user_id 집합. 호출자는 candidates에서 빼서 사용한다.
     */
    public Set<Long> findUserIdsWithScrumOn(LocalDate kstDate, Collection<Long> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                scrumJpaRepository.findDistinctUserIdsByScrumDate(candidates, kstDate));
    }
}
