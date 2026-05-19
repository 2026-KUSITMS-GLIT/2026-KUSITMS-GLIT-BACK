package com.groute.groute_server.report.application.port.out;

import java.time.OffsetDateTime;

/**
 * 리포트 도메인에서 심화기록 수를 조회하기 위한 포트.
 *
 * <p>report 도메인이 record 도메인 내부에 직접 의존하지 않도록 경계를 분리한다. 구현체는 adapter/out/client에 위치한다.
 */
public interface StarRecordCountQueryPort {

    /**
     * 특정 시점 이후 완료된 심화기록 수를 조회한다.
     *
     * <p>게이지 계산 시 마지막 리포트 생성 이후 완료된 심화기록 수를 구하는 데 사용한다(RPT-001).
     *
     * @param userId 유저 ID
     * @param after 이 시점 이후 완료된 것만 카운트. null이면 전체 카운트
     * @return 완료된 심화기록 수
     */
    int countCompletedAfter(Long userId, OffsetDateTime after);
}
