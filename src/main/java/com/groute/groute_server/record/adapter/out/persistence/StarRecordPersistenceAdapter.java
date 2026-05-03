package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.StarRecordPort;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarRecordPort}의 JPA 구현체.
 *
 * <p>서비스는 이 클래스의 존재를 모른다. {@link StarRecordPort} 인터페이스만 바라보며, Spring이 실행 시점에 이 어댑터를 자동으로 주입한다.
 */
@Component
@RequiredArgsConstructor
public class StarRecordPersistenceAdapter implements StarRecordPort {

    private final StarRecordRepository starRecordRepository;

    @Override
    public Optional<StarRecord> findById(Long starRecordId) {
        return starRecordRepository.findByIdAndIsDeletedFalse(starRecordId);
    }
}
