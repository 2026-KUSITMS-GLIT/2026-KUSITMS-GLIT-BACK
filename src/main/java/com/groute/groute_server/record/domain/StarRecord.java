package com.groute.groute_server.record.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;
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

    /** 현재 작성 단계. 재진입 시 이 단계부터 복원된다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private StarStep currentStep = StarStep.ST;

    /** 작성·태깅 진행 상태. WRITING → WRITTEN(R 완료) → TAGGED(AI 태깅 완료). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StarRecordStatus status = StarRecordStatus.WRITING;

    /** R 단계 완료 시각. 리포트 임계치(10회 단위) 카운트 기준(RPT001). */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** AI 태깅 완료 여부. status=TAGGED 전환 시 true로 설정. JPQL 조건절 호환용. */
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    /** 신규 StarRecord 팩토리. currentStep=ST, status=WRITING으로 초기화된다. */
    public static StarRecord create(User user, Scrum scrum) {
        StarRecord record = new StarRecord();
        record.user = Objects.requireNonNull(user, "user");
        record.scrum = Objects.requireNonNull(scrum, "scrum");
        return record;
    }

    /**
     * 단계별 내용을 저장하고 currentStep을 다음 단계로 진행한다.
     *
     * <p>ST 저장 시 A 단계로, A 저장 시 R 단계로 전진. R 저장 후 완료 처리는 {@link #complete}로 별도 호출.
     */
    public void saveStep(StarStep step, String answer) {
        Objects.requireNonNull(answer, "answer");
        switch (step) {
            case ST -> {
                this.situationTask = answer;
                this.currentStep = StarStep.A;
            }
            case A -> {
                this.action = answer;
                this.currentStep = StarStep.R;
            }
            case R -> this.result = answer;
            default -> throw new IllegalArgumentException("저장할 수 없는 단계: " + step);
        }
    }

    /** R 단계 완료 처리. status=WRITTEN, currentStep=DONE, completedAt 설정. */
    public void complete(OffsetDateTime completedAt) {
        this.status = StarRecordStatus.WRITTEN;
        this.currentStep = StarStep.DONE;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
    }

    /** AI 태깅 완료 처리. status=TAGGED로 전환하고 isCompleted=true로 설정. */
    public void tag() {
        this.status = StarRecordStatus.TAGGED;
        this.isCompleted = true;
    }

    /** 작성이 잠긴 상태인지 확인한다. WRITTEN 또는 TAGGED이면 재저장 불가. */
    public boolean isWriteLocked() {
        return this.status != StarRecordStatus.WRITING;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    /** AI 태깅 요청 가능 여부. status가 WRITTEN이어야 한다. */
    public boolean isReadyForTagging() {
        return this.status == StarRecordStatus.WRITTEN;
    }
}
