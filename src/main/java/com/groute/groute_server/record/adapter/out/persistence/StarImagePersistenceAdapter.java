package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.domain.StarImage;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarImageQueryPort}·{@link StarImageWritePort}의 JPA 어댑터.
 *
 * <p>심화기록 상세 응답의 이미지 목록을 sortOrder 오름차순으로 제공하고, 이미지 저장·삭제를 처리한다.
 */
@Component
@RequiredArgsConstructor
class StarImagePersistenceAdapter implements StarImageQueryPort, StarImageWritePort {

    private final StarImageJpaRepository jpaRepository;

    @Override
    public List<StarImage> findAllByStarRecordIdOrderBySortOrder(Long starRecordId) {
        return jpaRepository.findAllByStarRecordIdOrderBySortOrderAsc(starRecordId);
    }

    @Override
    public Optional<StarImage> findById(Long imageId) {
        return jpaRepository.findById(imageId);
    }

    @Override
    public List<StarImage> findAllByScrumIdIn(Collection<Long> scrumIds) {
        if (scrumIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByStarRecordScrumIdIn(scrumIds);
    }

    @Override
    public StarImage save(StarImage starImage) {
        return jpaRepository.save(starImage);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteAll(Collection<StarImage> images) {
        jpaRepository.deleteAll(images);
    }
}
