package com.groute.groute_server.report.adapter.out.client;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.adapter.out.persistence.StarRecordJpaRepository;
import com.groute.groute_server.report.application.port.out.StarRecordCountQueryPort;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarRecordCountQueryPort}의 구현체.
 *
 * <p>report 도메인이 record 도메인 내부에 직접 의존하지 않도록 경계를 분리한다. StarRecordJpaRepository를 주입받아 게이지 계산에 필요한
 * 심화기록 수만 조회한다(RPT-001).
 */
@Component
@RequiredArgsConstructor
public class StarRecordClientAdapter implements StarRecordCountQueryPort {

    private final StarRecordJpaRepository starRecordJpaRepository;

    @Override
    public int countCompletedAfter(Long userId, OffsetDateTime after) {
        return starRecordJpaRepository.countCompletedAfter(userId, after);
    }
}
