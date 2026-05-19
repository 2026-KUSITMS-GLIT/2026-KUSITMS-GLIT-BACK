package com.groute.groute_server.report.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;

import lombok.RequiredArgsConstructor;

/**
 * {@link LoadStarRecordPort}의 JPA 어댑터.
 *
 * <p>리포트 생성에 필요한 심화기록 및 스크럼 조회를 담당한다.
 */
@Component
@RequiredArgsConstructor
class StarRecordForReportPersistenceAdapter implements LoadStarRecordPort {

    private final StarRecordForReportJpaRepository starRecordJpaRepository;
    private final ScrumForReportJpaRepository scrumJpaRepository;
    private final StarTagForReportJpaRepository starTagJpaRepository;

    @Override
    public int countCompletedByUserId(Long userId) {
        return starRecordJpaRepository.countCompletedByUserId(userId);
    }

    @Override
    public List<StarRecord> findCompletedByUserIdOrderByLatest(Long userId, int limit) {
        return starRecordJpaRepository.findCompletedByUserIdOrderByLatest(userId, limit);
    }

    @Override
    public List<LocalDate> findCompletedStarDatesByUserId(Long userId) {
        return starRecordJpaRepository.findCompletedStarDatesByUserId(userId);
    }

    @Override
    public List<StarRecord> findAllByIds(Long userId, List<Long> starRecordIds) {
        return starRecordJpaRepository.findAllByIds(userId, starRecordIds);
    }

    @Override
    public List<Scrum> findScrumsByStarRecordIds(Long userId, List<Long> starRecordIds) {
        return scrumJpaRepository.findScrumsByStarRecordIds(userId, starRecordIds);
    }

    @Override
    public List<StarRecord> findCompletedByUserIdAndDate(Long userId, LocalDate date) {
        return starRecordJpaRepository.findCompletedByUserIdAndDate(userId, date);
    }

    @Override
    public Map<Long, List<String>> findDetailTagsByStarRecordIds(List<Long> starRecordIds) {
        if (starRecordIds == null || starRecordIds.isEmpty()) {
            return Map.of();
        }
        List<StarTag> tags = starTagJpaRepository.findAllByStarRecordIds(starRecordIds);
        return tags.stream()
                .collect(
                        Collectors.groupingBy(
                                t -> t.getStarRecord().getId(),
                                Collectors.mapping(StarTag::getDetailTag, Collectors.toList())));
    }
}
