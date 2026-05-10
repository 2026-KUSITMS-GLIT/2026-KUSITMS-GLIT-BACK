package com.groute.groute_server.report.application.service;

import org.springframework.stereotype.Service;

import com.groute.groute_server.report.application.port.in.ReportAccountHardDeleteUseCase;
import com.groute.groute_server.report.application.port.out.ReportHardDeletePort;

import lombok.RequiredArgsConstructor;

/**
 * {@link ReportAccountHardDeleteUseCase} 구현(MYP-005).
 *
 * <p>외부 도메인이 report 내부 port/out에 직접 결합하지 않도록 use case로 한 단계 추상화한다. 본 service는 자체 비즈니스 로직 없이
 * port/out 호출을 1:1로 위임한다.
 */
@Service
@RequiredArgsConstructor
public class ReportAccountHardDeleteService implements ReportAccountHardDeleteUseCase {

    private final ReportHardDeletePort reportHardDeletePort;

    @Override
    public void purgeDb(Long userId) {
        reportHardDeletePort.hardDeleteAllByUserId(userId);
    }
}
