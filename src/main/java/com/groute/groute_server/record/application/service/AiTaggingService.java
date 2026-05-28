package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingResultResponse;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingStatusResponse;
import com.groute.groute_server.record.application.port.in.CompleteAiTaggingUseCase;
import com.groute.groute_server.record.application.port.in.GetAiTaggingResultUseCase;
import com.groute.groute_server.record.application.port.in.GetAiTaggingStatusUseCase;
import com.groute.groute_server.record.application.port.in.TriggerAiTaggingUseCase;
import com.groute.groute_server.record.application.port.out.AiTaggingJobPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.record.domain.enums.JobStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 태깅 비즈니스 로직.
 *
 * <p>REC-005(트리거), REC-006(상태 폴링), REC-007(결과 조회) 유스케이스를 구현한다. 외부 의존성(DB)은 포트 인터페이스를 통해서만 접근한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiTaggingService
        implements TriggerAiTaggingUseCase,
                GetAiTaggingStatusUseCase,
                GetAiTaggingResultUseCase,
                CompleteAiTaggingUseCase {

    private final StarRecordRepositoryPort starRecordPort;
    private final AiTaggingJobPort aiTaggingJobPort;
    private final StarTagQueryPort starTagPort;
    private final AiTaggingAsyncExecutor aiTaggingAsyncExecutor;
    private final StarTagPersister starTagPersister;
    private final ScrumTitleCommitter scrumTitleCommitter;
    private final UserMilestoneTracker userMilestoneTracker;

    /**
     * REC-005: AI 태깅 트리거.
     *
     * <p>기존 잡 상태에 따라 분기한다.
     *
     * <ul>
     *   <li>잡 없거나 FAILED {@code &&} retryCount=0 → 새 잡 생성
     *   <li>RUNNING → 409 Conflict
     *   <li>FAILED {@code &&} retryCount=1 → 400 Bad Request (최종 실패)
     * </ul>
     */
    @Override
    @Transactional
    public void trigger(Long starRecordId, Long userId) {
        // 1. StarRecord 조회
        StarRecord starRecord =
                starRecordPort
                        .findById(starRecordId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_RECORD_NOT_FOUND));

        // 2. 본인 소유 확인
        if (!starRecord.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. DONE 단계 확인 (R단계까지 작성 완료 여부)
        if (!starRecord.isReadyForTagging()) {
            throw new BusinessException(ErrorCode.STAR_RECORD_NOT_READY);
        }

        // 4. 기존 잡 상태 분기
        aiTaggingJobPort
                .findLatestByStarRecordId(starRecordId)
                .ifPresent(
                        job -> {
                            if (job.getStatus() == JobStatus.RUNNING) {
                                throw new BusinessException(ErrorCode.AI_TAGGING_ALREADY_RUNNING);
                            }
                            if (job.getStatus() == JobStatus.FAILED && job.getRetryCount() >= 1) {
                                throw new BusinessException(
                                        ErrorCode.AI_TAGGING_PERMANENTLY_FAILED);
                            }
                        });

        // 5. 새 잡 생성 (QUEUED)
        AiTaggingJob job = aiTaggingJobPort.save(starRecord);
        log.debug("AI 태깅 잡 생성: starRecordId={}, userId={}", starRecordId, userId);

        // 6. 트랜잭션 커밋 후 비동기 FastAPI 호출 (커밋 전 조회 실패 방지)
        Long jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        aiTaggingAsyncExecutor.execute(jobId);
                    }
                });
    }

    /**
     * REC-006: AI 태깅 상태 폴링.
     *
     * <p>가장 최근 잡의 상태와 재시도 횟수를 반환한다.
     */
    @Override
    public AiTaggingStatusResponse getStatus(Long starRecordId, Long userId) {
        // 1. StarRecord 조회
        StarRecord starRecord =
                starRecordPort
                        .findById(starRecordId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_RECORD_NOT_FOUND));

        // 2. 본인 소유 확인
        if (!starRecord.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 가장 최근 잡 조회
        AiTaggingJob job =
                aiTaggingJobPort
                        .findLatestByStarRecordId(starRecordId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.AI_TAGGING_JOB_NOT_FOUND));

        return AiTaggingStatusResponse.from(job);
    }

    /**
     * REC-007: AI 태깅 결과 조회.
     *
     * <p>잡 상태가 SUCCESS일 때만 결과를 반환한다.
     */
    @Override
    public AiTaggingResultResponse getResult(Long starRecordId, Long userId) {
        // 1. StarRecord 조회
        StarRecord starRecord =
                starRecordPort
                        .findById(starRecordId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_RECORD_NOT_FOUND));

        // 2. 본인 소유 확인
        if (!starRecord.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 가장 최근 잡 조회
        AiTaggingJob job =
                aiTaggingJobPort
                        .findLatestByStarRecordId(starRecordId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.AI_TAGGING_JOB_NOT_FOUND));

        // 4. SUCCESS 상태 확인
        if (job.getStatus() != JobStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.AI_TAGGING_NOT_COMPLETED);
        }

        // 5. star_tags 조회 후 응답 반환
        List<StarTag> tags = starTagPort.findAllByStarRecordId(starRecordId);
        return AiTaggingResultResponse.from(tags);
    }

    /**
     * FastAPI 태깅 완료 시 호출. StarRecord를 TAGGED로 전환하고, 세션 내 전체 완료 시 ScrumTitle을 COMMITTED로 전환한다.
     *
     * <p>FastAPI 연동 구현 후 {@code AiTaggingClientAdapter}에서 이 메서드를 호출하도록 연결한다.
     */
    @Override
    @Transactional
    public void completeTagging(
            Long starRecordId, String primaryCategory, List<String> detailTags) {
        StarRecord record =
                starRecordPort
                        .findByIdWithScrum(starRecordId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_RECORD_NOT_FOUND));

        record.tag();

        starTagPersister.persist(record, primaryCategory, detailTags);

        Long userId = record.getUser().getId();
        LocalDate date = record.getScrum().getScrumDate();

        scrumTitleCommitter.commitIfFullyTagged(userId, date);

        userMilestoneTracker.track(userId);
    }
}
