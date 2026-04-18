package com.groute.groute_server.record.domain;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 일자별 역량 집계 테이블.
 *
 * <p>홈 잔디(HOM002)·레이더 차트(HOM003)의 성능 최적화용.
 * STAR 완료 시 upsert되며, (user_id, stat_date) UNIQUE 기준으로 ON CONFLICT 갱신.
 * 최근 3개월 구간만 조회 대상(HOM002).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "daily_competency_stats")
public class DailyCompetencyStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 집계 대상 날짜. */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 해당 날짜 스크럼 수. 잔디 기본색 결정(HOM002: 스크럼만 있는 날 = 기본색). */
    @Column(name = "scrum_count", nullable = false)
    private Short scrumCount = 0;

    /** STAR 완료 수. 잔디 농도 결정(HOM002: 1건=연한 / 2건=중간 / 3건+=진한). */
    @Column(name = "star_count", nullable = false)
    private Short starCount = 0;

    /**
     * 해당 날짜 대표 역량. 잔디 색상 결정.
     * STAR 0건이면 NULL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_category")
    private CompetencyCategory primaryCategory;

    /**
     * 레이더 차트용 카테고리별 카운트(HOM003).
     * 예: {@code {"DISCOVERY_ANALYSIS":2,"PLANNING_EXECUTION":1,...}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_counts", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> categoryCounts = new HashMap<>();
}
