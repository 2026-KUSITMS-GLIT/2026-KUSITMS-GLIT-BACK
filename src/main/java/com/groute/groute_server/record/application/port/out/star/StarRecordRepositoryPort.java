package com.groute.groute_server.record.application.port.out.star;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;

/**
 * StarRecord 단건 조회·삭제 포트 (CAL-003).
 *
 * <p>소유권 검증은 호출자(서비스)가 로드된 엔티티의 user를 비교해 처리한다. 미존재(404)와 타인 소유(403)를 구분하기 위해 ownership 필터를 포트에 두지
 * 않는다.
 */
public interface StarRecordRepositoryPort {

    /** 응답 카테고리·부제목 매핑을 위해 Scrum·Title·Project까지 fetch join 한 단건 조회. */
    Optional<StarRecord> findByIdWithScrum(Long id);

    /** 논리 삭제된 레코드를 제외한 단건 조회. */
    Optional<StarRecord> findById(Long starRecordId);

    /** 이미지 업로드 시 2장 제한 Race Condition 방지용 비관적 락 조회. */
    Optional<StarRecord> findByIdWithLock(Long starRecordId);

    /** 단건 soft-delete. cascade(Scrum.hasStar=false 등)는 호출자가 별도 처리. */
    void softDeleteById(Long id);

    /** PENDING 세션 취소 시 해당 스크럼들에 연결된 StarRecord 일괄 soft-delete. */
    int softDeleteByScrumIds(List<Long> scrumIds);

    /** 해당 날짜에 TAGGED 미완료(WRITING·WRITTEN) StarRecord가 남아있는지 확인. */
    boolean existsUntaggedByUserAndDate(Long userId, LocalDate date);

    /** 사용자의 TAGGED 심화기록 총 개수. */
    long countTaggedByUserId(Long userId);

    /** TAGGED 완료된 STAR 기록의 역량 카테고리별 건수. selectedCompetency가 NULL인 스크럼은 제외. */
    List<CompetencyCount> countCompletedByCompetency(Long userId, StarRecordStatus status);
}
