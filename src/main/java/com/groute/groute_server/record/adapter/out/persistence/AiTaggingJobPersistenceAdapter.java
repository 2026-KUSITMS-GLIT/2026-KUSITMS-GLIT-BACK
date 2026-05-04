package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link AiTaggingJobPort}의 JPA 구현체.
 *
 * <p>서비스는 이 클래스의 존재를 모른다. {@link AiTaggingJobPort} 인터페이스만 바라보며, Spring이 실행 시점에 이 어댑터를 자동으로 주입한다.
 */
@Component
@RequiredArgsConstructor
public class AiTaggingJobPersistenceAdapter implements AiTaggingJobPort {

    private final AiTaggingJobRepository aiTaggingJobRepository;

    @Override
    public Optional<AiTaggingJob> findLatestByStarRecordId(Long starRecordId) {
        return aiTaggingJobRepository.findTopByStarRecordIdOrderByCreatedAtDesc(starRecordId);
    }

    @Override
    public AiTaggingJob save(StarRecord starRecord) {
        return aiTaggingJobRepository.save(new AiTaggingJob(starRecord));
    }
}
