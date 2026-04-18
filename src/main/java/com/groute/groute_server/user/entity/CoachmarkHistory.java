package com.groute.groute_server.user.entity;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 코치마크 노출 이력.
 *
 * <p>유저당 코치마크 종류별 1회만 노출. 최초 노출 시 row를 생성하고 이후 재노출하지 않는다.
 * 첫 스크럼/첫 심화기록 완료 후 캘린더 탭 코치마크가 대표 예시(REC002/REC007).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "coachmark_histories")
public class CoachmarkHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 코치마크 식별자(예: CALENDAR_AFTER_SCRUM, CALENDAR_AFTER_STAR). */
    @Column(name = "coachmark_key", nullable = false, length = 50)
    private String coachmarkKey;

    /** 최초 노출 시각. 이후 재노출 없음. */
    @Column(name = "shown_at", nullable = false)
    private OffsetDateTime shownAt;
}
