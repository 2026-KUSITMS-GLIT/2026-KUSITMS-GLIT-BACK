package com.groute.groute_server.record.application.port.out.scrum;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.groute.groute_server.record.domain.Scrum;

/** Scrum 조회 포트. */
public interface ScrumQueryPort {

    /** 일자별 사용자 스크럼 전체. ScrumTitle·Project fetch join 보장. */
    List<Scrum> findAllByUserAndDate(Long userId, LocalDate date);

    /** 요청 scrumId 중 본인 소유인 것만 반환. 결과 크기로 미존재/타인 소유 판별. */
    List<Scrum> findAllByIdInAndUserId(Collection<Long> ids, Long userId);
}
