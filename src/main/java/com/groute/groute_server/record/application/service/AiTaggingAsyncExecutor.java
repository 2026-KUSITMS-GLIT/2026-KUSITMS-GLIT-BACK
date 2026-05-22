package com.groute.groute_server.record.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.record.application.port.out.AiTaggingClient;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.domain.AiTaggingJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 태깅 비동기 실행기.
 *
 * <p>{@link AiTaggingService#trigger}에서 잡 생성 후 호출된다. {@code @Async}를 별도 빈으로 분리해 Spring 프록시를 통한 비동기
 * 실행을 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaggingAsyncExecutor {

    private final AiTaggingClient aiTaggingClient;
    private final AiTaggingJobPort aiTaggingJobPort;

    /**
     * FastAPI 태깅 요청을 비동기로 실행한다.
     *
     * <p>트리거 API 응답을 블로킹하지 않고 백그라운드에서 실행된다. jobId로 새 트랜잭션에서 잡을 재조회해 Lazy 로딩 세션 문제를 방지한다.
     *
     * @param jobId 처리할 AI 태깅 잡 ID
     */
    @Async
    @Transactional
    public void execute(Long jobId) {
        AiTaggingJob job =
                aiTaggingJobPort
                        .findById(jobId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "AI 태깅 잡을 찾을 수 없습니다: jobId=" + jobId));
        log.debug(
                "[AI Tagging] 비동기 실행 시작 — jobId={}, starRecordId={}",
                job.getId(),
                job.getStarRecord().getId());
        aiTaggingClient.requestTagging(job);
    }
}
