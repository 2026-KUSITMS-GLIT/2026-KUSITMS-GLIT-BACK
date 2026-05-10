package com.groute.groute_server.record.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import lombok.RequiredArgsConstructor;

/**
 * {@link ScrumQueryPort}와 {@link ScrumWritePort}의 JPA 어댑터.
 *
 * <p>같은 JPA 레포를 공유하므로 두 포트를 한 어댑터에서 함께 구현한다. 모든 동작은 soft-delete(is_deleted=false) 기준.
 */
@Component
@RequiredArgsConstructor
class ScrumPersistenceAdapter implements ScrumQueryPort, ScrumWritePort {

    private final ScrumJpaRepository jpaRepository;

    @Override
    public List<Scrum> findAllByUserAndDate(Long userId, LocalDate date) {
        return jpaRepository.findAllByUserIdAndScrumDate(userId, date);
    }

    @Override
    public List<Scrum> findAllByIdInAndUserId(Collection<Long> ids, Long userId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByIdInAndUserId(ids, userId);
    }

    @Override
    public List<Scrum> saveAll(Collection<Scrum> scrums) {
        if (scrums.isEmpty()) {
            return List.of();
        }
        return jpaRepository.saveAll(scrums);
    }

    @Override
    public void updateContent(Long scrumId, String content) {
        jpaRepository.updateContent(scrumId, content);
    }

    @Override
    public void softDeleteAllByIdIn(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        jpaRepository.softDeleteAllByIdIn(ids);
    }

    @Override
    public void clearHasStar(Long scrumId) {
        jpaRepository.clearHasStarById(scrumId);
    }

    @Override
    public boolean existsByUserAndDate(Long userId, LocalDate date) {
        return jpaRepository.existsByUserIdAndScrumDateAndIsDeletedFalse(userId, date);
    }

    @Override
    public void updateCompetency(Long scrumId, CompetencyCategory competency) {
        jpaRepository.updateCompetency(scrumId, competency);
    }
}
