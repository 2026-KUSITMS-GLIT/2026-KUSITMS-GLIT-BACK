package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.StarTagPort;
import com.groute.groute_server.record.domain.StarTag;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarTagPort}의 JPA 구현체.
 *
 * <p>서비스는 이 클래스의 존재를 모른다. {@link StarTagPort} 인터페이스만 바라보며, Spring이 실행 시점에 이 어댑터를 자동으로 주입한다.
 */
@Component
@RequiredArgsConstructor
public class StarTagPersistenceAdapter implements StarTagPort {

    private final StarTagRepository starTagRepository;

    @Override
    public List<StarTag> findAllByStarRecordId(Long starRecordId) {
        return starTagRepository.findAllByStarRecordId(starRecordId);
    }
}
