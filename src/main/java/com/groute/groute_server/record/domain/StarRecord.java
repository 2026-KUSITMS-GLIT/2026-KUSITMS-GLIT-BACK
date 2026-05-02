package com.groute.groute_server.record.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 심화 STAR 기록.
 *
 * <p>스크럼과 1:1(REC008). 단계별로 작성이 진행되며, 3단계 완료 후 AI 태깅 호출로 완료된다. {@link #currentStep} + 각 단계 필드(S/T,
 * A, R)로 임시저장을 대체한다(REC010).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "star_records")
public class StarRecord extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 소속 스크럼(1:1 UNIQUE). 스크럼당 STAR 1개만 가능(REC008). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrum_id", nullable = false)
    private Scrum scrum;

    /** S·T 단계 입력. 최대 300자(REC005). NULL = 미작성. */
    @Column(name = "situation_task", columnDefinition = "TEXT")
    private String situationTask;

    /** A 단계 입력. 최대 300자(REC005). NULL = 미작성. */
    @Column(name = "action", columnDefinition = "TEXT")
    private String action;

    /** R 단계 입력. 최대 300자(REC005). NULL = 미작성. */
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /** 현재 작성 단계(REC005). 재진입 시 이 단계부터 복원되어 임시저장 역할을 한다(REC010). */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private StarStep currentStep = StarStep.ST;

    /** R 단계 완료 + AI 태깅 호출 후 true. */
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    /** 완료 시각. 리포트 임계치(10회 단위) 카운트 기준(RPT001). */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;


    /**
     * 이 StarRecord가 특정 유저의 소유인지 확인한다.
     *
     * <p>AI 태깅 트리거 시 본인 소유 검증에 사용한다(REC-005).
     *
     * @param userId 검증할 유저 ID
     * @return 소유자이면 true
     */
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    /**
     * AI 태깅 호출 가능한 상태인지 확인한다.
     *
     * <p>currentStep이 DONE이어야 AI 태깅 트리거 가능하다(REC-005).
     * DONE은 S·T, A, R 3단계가 모두 작성 완료된 상태를 의미한다.
     *
     * @return DONE 단계이면 true
     */
    public boolean isReadyForTagging() {
        return this.currentStep == StarStep.DONE;
    }
}
