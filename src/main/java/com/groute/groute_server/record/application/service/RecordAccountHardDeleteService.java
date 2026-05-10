package com.groute.groute_server.record.application.service;

import org.springframework.stereotype.Service;

import com.groute.groute_server.record.application.port.in.RecordAccountHardDeleteUseCase;
import com.groute.groute_server.record.application.port.out.RecordHardDeletePort;
import com.groute.groute_server.record.application.port.out.star.StarImageStoragePort;

import lombok.RequiredArgsConstructor;

/**
 * {@link RecordAccountHardDeleteUseCase} 구현(MYP-005).
 *
 * <p>외부 도메인이 record 내부 port/out에 직접 결합하지 않도록 use case로 한 단계 추상화한다. 본 service는 자체 비즈니스 로직 없이 두
 * port/out 호출을 1:1로 위임한다.
 */
@Service
@RequiredArgsConstructor
public class RecordAccountHardDeleteService implements RecordAccountHardDeleteUseCase {

    private final StarImageStoragePort starImageStoragePort;
    private final RecordHardDeletePort recordHardDeletePort;

    @Override
    public void purgeExternalStorage(Long userId) {
        starImageStoragePort.deleteAllByUserId(userId);
    }

    @Override
    public void purgeDb(Long userId) {
        recordHardDeletePort.hardDeleteAllByUserId(userId);
    }
}
