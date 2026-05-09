package com.groute.groute_server.record.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.RecordHardDeletePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link RecordHardDeletePort}의 JPA 어댑터(MYP-005 hard delete 배치).
 *
 * <p>외래키 관계(V1 마이그레이션 기준)를 따라 자식 테이블부터 차례로 DELETE를 날린다. JPA 캐시를 거치지 않고 DB에 바로 쏘는 방식이라 메모리에 남아있는
 * 엔티티는 DB와 어긋날 수 있는데, 본 배치는 사용자 한 명만 짧게 처리하고 끝나는 트랜잭션이라 같은 컨텍스트에서 그 엔티티를 다시 쓸 일이 없다.
 *
 * <p>삭제 순서:
 *
 * <ol>
 *   <li>StarTag — star_records FK
 *   <li>StarImage — star_records FK
 *   <li>AiTaggingJob — star_records FK
 *   <li>StarRecord — users FK + scrums FK
 *   <li>Scrum — users FK + scrum_titles FK
 *   <li>ScrumTitle — users FK + projects FK
 *   <li>Project — users FK
 *   <li>DailyCompetencyStat — users FK (다른 도메인 FK 없음, 마지막에 둬도 무방)
 * </ol>
 *
 * <p>각 단계의 삭제 row 수는 DEBUG 로깅한다. 프로덕션 INFO 로그는 호출자(service)가 사용자별 합산 결과를 출력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RecordHardDeleteAdapter implements RecordHardDeletePort {

    private final StarTagJpaRepository starTagJpaRepository;
    private final StarImageJpaRepository starImageJpaRepository;
    private final AiTaggingJobRepository aiTaggingJobRepository;
    private final StarRecordJpaRepository starRecordJpaRepository;
    private final ScrumJpaRepository scrumJpaRepository;
    private final ScrumTitleJpaRepository scrumTitleJpaRepository;
    private final ProjectJpaRepository projectJpaRepository;
    private final DailyCompetencyStatJpaRepository dailyCompetencyStatJpaRepository;

    @Override
    public void hardDeleteAllByUserId(Long userId) {
        int starTags = starTagJpaRepository.hardDeleteAllByUserId(userId);
        int starImages = starImageJpaRepository.hardDeleteAllByUserId(userId);
        int aiJobs = aiTaggingJobRepository.hardDeleteAllByUserId(userId);
        int starRecords = starRecordJpaRepository.hardDeleteAllByUserId(userId);
        int scrums = scrumJpaRepository.hardDeleteAllByUserId(userId);
        int scrumTitles = scrumTitleJpaRepository.hardDeleteAllByUserId(userId);
        int projects = projectJpaRepository.hardDeleteAllByUserId(userId);
        int dailyStats = dailyCompetencyStatJpaRepository.hardDeleteAllByUserId(userId);
        log.debug(
                "record hard delete (userId={}): starTags={}, starImages={}, aiJobs={},"
                        + " starRecords={}, scrums={}, scrumTitles={}, projects={}, dailyStats={}",
                userId,
                starTags,
                starImages,
                aiJobs,
                starRecords,
                scrums,
                scrumTitles,
                projects,
                dailyStats);
    }
}
