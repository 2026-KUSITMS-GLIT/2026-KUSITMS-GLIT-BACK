package com.groute.groute_server.record.domain;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 데일리 스크럼 본문.
 *
 * <p>스크럼 제목(N:1)에 매달리는 데일리 기록. 날짜당 유저별 전체 스크럼 최대 5개 제약(REC002).
 * {@link #hasStar}=true이면 심화 STAR 기록이 존재하며, 스크럼 수정이 잠긴다(CAL002).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "scrums")
public class Scrum extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 비정규화 FK. scrum_titles를 거쳐서도 접근 가능하지만 단축 조회용. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private ScrumTitle title;

    /** 본문. 50자 이내(REC002). */
    @Column(name = "content", nullable = false, length = 50)
    private String content;

    /**
     * 사용자가 선택한 날짜.
     * 당일 기준 최근 14일 이내만 선택 가능하며 미래 불가(REC002).
     * 범위 검증은 CURRENT_DATE 의존이라 DB CHECK 대신 애플리케이션 레벨에서 수행.
     */
    @Column(name = "scrum_date", nullable = false)
    private LocalDate scrumDate;

    /**
     * 심화 STAR 기록 연결 여부.
     * true 시 스크럼 수정 잠금. 스크럼 삭제 시 심화기록도 함께 삭제(CAL002).
     */
    @Column(name = "has_star", nullable = false)
    private boolean hasStar = false;

    /**
     * 사용자가 선택한 5대 역량(REC004).
     * STAR 심화 기록 시작 전 선택. NULL 허용(미선택 상태).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_competency")
    private CompetencyCategory selectedCompetency;
}