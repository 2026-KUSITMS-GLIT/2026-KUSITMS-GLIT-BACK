package com.groute.groute_server.record.application.port.out.star;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.groute.groute_server.record.domain.StarImage;

/** StarImage 조회 포트. */
public interface StarImageQueryPort {

    List<StarImage> findAllByStarRecordIdOrderBySortOrder(Long starRecordId);

    Optional<StarImage> findById(Long imageId);

    /** Scrum cascade 삭제 시 해당 scrumId에 연결된 모든 이미지 조회. */
    List<StarImage> findAllByScrumIdIn(Collection<Long> scrumIds);
}
