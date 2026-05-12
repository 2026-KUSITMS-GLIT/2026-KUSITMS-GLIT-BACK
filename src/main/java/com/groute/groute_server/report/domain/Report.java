package com.groute.groute_server.report.domain;

import java.util.Map;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트(RPT001).
 *
 * <p>미니(첫 10회 1회성) + 커리어(이후 10회 단위) 두 종류를 지원. MVP에서는 결제 플로우 없이 무료 생성만 제공한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "reports")
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** MINI / CAREER(RPT001). */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    /** AI 호출 완료 시 SUCCESS/FAILED로 전환(RPT003). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.GENERATING;

    /** 발행 시점 누적 STAR 수(10/20/30...). */
    @Column(name = "star_count_at", nullable = false)
    private Integer starCountAt;

    /** 커리어 브랜딩 문장. 리포트 목록 카드에 노출(RPT001). */
    @Column(name = "title", length = 200)
    private String title;

    /** 리포트 본문 JSON. 통합 서사 요약 / 핵심 강점 / 전략적 어필 포인트 / 예상 면접 질문 / branding_title 등(RPT003). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private Map<String, Object> contentJson;

    /** AI 실패 시 1회 재시도 제공(RPT003). */
    @Column(name = "retry_count", nullable = false)
    private Short retryCount = 0;

    // =========================================================
    // 팩토리 메서드
    // =========================================================

    /**
     * 리포트 생성 요청 시 호출. status는 GENERATING으로 초기화된다.
     *
     * @param user 요청 유저
     * @param reportType MINI / CAREER
     * @param starCountAt 발행 시점 누적 STAR 수
     */
    public static Report create(User user, ReportType reportType, int starCountAt) {
        Report report = new Report();
        report.user = user;
        report.reportType = reportType;
        report.status = ReportStatus.GENERATING;
        report.starCountAt = starCountAt;
        report.retryCount = 0;
        return report;
    }

    // =========================================================
    // 상태 전환
    // =========================================================

    /**
     * AI 실패 후 재시도 요청 시 호출. status를 GENERATING으로 되돌리고 retryCount를 1로 올린다.
     *
     * <p>재시도 가능 여부는 {@link #isRetryAvailable()}로 먼저 확인해야 한다.
     */
    public void startRetry() {
        if (!isRetryAvailable()) {
            throw new BusinessException(ErrorCode.REPORT_RETRY_NOT_AVAILABLE);
        }
        this.status = ReportStatus.GENERATING;
        this.retryCount = 1;
    }

    // =========================================================
    // 도메인 규칙
    // =========================================================

    /** 재시도 가능 여부. FAILED 상태이고 아직 재시도를 1회도 하지 않은 경우에만 true. */
    public boolean isRetryAvailable() {
        return this.status == ReportStatus.FAILED && this.retryCount < 1;
    }
}
