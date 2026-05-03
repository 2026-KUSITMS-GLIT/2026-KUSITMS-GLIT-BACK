package com.groute.groute_server.record.application.port.out;

import com.groute.groute_server.record.domain.AiTaggingJob;

/**
 * AI 서버 호출 포트.
 *
 * <p>서비스가 FastAPI 서버를 직접 알지 않도록 인터페이스로 분리한다.
 * 실제 구현은 {@code AiTaggingClientAdapter}가 담당한다.
 *
 * <p>AI 모델 교체 시 이 인터페이스의 새 구현체만 추가하면 되며,
 * 서비스 코드는 변경하지 않아도 된다.
 */
public interface AiTaggingClient {

    /**
     * AI 서버에 역량 태깅을 요청한다.
     *
     * <p>워커가 QUEUED 잡을 꺼내 호출한다. 응답은 star_tags에 저장된다.
     *
     * @param job 처리할 AI 태깅 잡
     */
    void requestTagging(AiTaggingJob job);
}
