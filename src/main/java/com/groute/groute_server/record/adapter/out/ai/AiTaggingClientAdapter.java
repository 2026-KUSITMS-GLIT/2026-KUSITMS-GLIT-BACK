package com.groute.groute_server.record.adapter.out.ai;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.AiTaggingClient;
import com.groute.groute_server.record.domain.AiTaggingJob;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link AiTaggingClient}의 FastAPI stub 구현체.
 *
 * <p>AI 서버 로직이 미구현 상태이므로 현재는 로그만 출력한다. FastAPI 구현 완료 후 이 클래스에 실제 HTTP 호출 코드를 추가한다.
 *
 * <p>교체 시 서비스 코드는 변경하지 않아도 된다. {@link AiTaggingClient} 인터페이스의 새 구현체로 교체하거나 이 클래스를 수정하면 된다.
 */
@Slf4j
@Component
public class AiTaggingClientAdapter implements AiTaggingClient {

    @Override
    public void requestTagging(AiTaggingJob job) {
        // TODO: FastAPI 구현 완료 후 HTTP 호출 추가. 완료 콜백 수신 시 CompleteAiTaggingUseCase.completeTagging()
        // 호출.
        log.info(
                "[AI Tagging Stub] jobId={}, starRecordId={} 태깅 요청 (미구현)",
                job.getId(),
                job.getStarRecord().getId());
    }
}
