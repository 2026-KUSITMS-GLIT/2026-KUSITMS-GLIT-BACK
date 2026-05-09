package com.groute.groute_server.report.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.groute.groute_server.report.application.port.out.ReportHardDeletePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ReportHardDeletePort}의 JPA 어댑터(MYP-005 hard delete 배치).
 *
 * <p>Report는 leaf 테이블이라 한 번의 DELETE로 끝난다. JPA 캐시를 거치지 않고 DB에 바로 쏘는 방식이라 메모리 엔티티가 DB와 어긋날 수 있는데, 본
 * 배치는 사용자 한 명만 짧게 처리하고 끝나는 트랜잭션이라 같은 컨텍스트에서 그 엔티티를 다시 쓸 일이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ReportHardDeleteAdapter implements ReportHardDeletePort {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public void hardDeleteAllByUserId(Long userId) {
        int reports = reportJpaRepository.hardDeleteAllByUserId(userId);
        log.debug("report hard delete (userId={}): reports={}", userId, reports);
    }
}
