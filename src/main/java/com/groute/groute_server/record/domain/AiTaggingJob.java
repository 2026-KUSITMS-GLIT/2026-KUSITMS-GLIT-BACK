package com.groute.groute_server.record.domain;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.record.domain.enums.JobStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 태깅 비동기 잡 큐(REC006).
 *
 * <p>워커가 {@code SELECT FOR UPDATE SKIP LOCKED}로 QUEUED 잡을 폴링해 처리한다. 최대 1회 재시도 허용(1차 실패 → 재시도 → 2차
 * 실패 시 FAILED 확정).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "ai_tagging_jobs")
public class AiTaggingJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star_record_id", nullable = false)
    private StarRecord starRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.QUEUED;

    /** 재시도 횟수. 0=초기, 1=1차 실패 후 재시도 중. 최대 1회 재시도(REC006). */
    @Column(name = "retry_count", nullable = false)
    private Short retryCount = 0;

    /** 요청 페이로드 JSON: 직군 + 선택 역량 + S-T-A-R 텍스트(REC006). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    /** AI 응답 원문. {primary_category, detail_tags[]} JSON. 디버깅 및 모델 교체 대비용 원본 보관. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;

    /** 실패 사유(상세 메시지). */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 워커가 RUNNING으로 전환한 시각. */
    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    /** SUCCESS/FAILED 확정 시각. */
    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    /**
     * 새 AI 태깅 잡을 생성한다.
     *
     * <p>REC-005 트리거 시 어댑터에서 호출한다. status는 QUEUED, retryCount는 0으로 초기화된다.
     *
     * @param starRecord 태깅 대상 STAR 기록
     */
    public AiTaggingJob(StarRecord starRecord) {
        this.starRecord = starRecord;
        this.status = JobStatus.QUEUED;
        this.retryCount = 0;
    }
}
