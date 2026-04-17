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

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "scrum_count", nullable = false)
    private Short scrumCount = 0;

    @Column(name = "star_count", nullable = false)
    private Short starCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_category")
    private CompetencyCategory primaryCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_counts", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> categoryCounts = new HashMap<>();
}