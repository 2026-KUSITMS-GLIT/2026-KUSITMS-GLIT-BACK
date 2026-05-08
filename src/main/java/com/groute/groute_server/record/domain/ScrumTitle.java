package com.groute.groute_server.record.domain;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.ScrumTitleStatus;
import com.groute.groute_server.user.entity.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스크럼 제목.
 *
 * <p>프로젝트 태그(FK) + 자유작성 텍스트(최대 20자)로 구성된다(REC002). 날짜와 무관하게 재사용 가능하며, 제목 선택 드롭다운 UI에 노출된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "scrum_titles")
public class ScrumTitle extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 소속 프로젝트 태그. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** 자유작성 영역. 최대 20자(REC002). */
    @Column(name = "free_text", nullable = false, length = 20)
    private String freeText;

    /** 비정규화 카운터: 이 제목에 연결된 scrums 수. UI의 "N회 사용" 뱃지 렌더링(REC002, is_deleted=false 기준). */
    @Builder.Default
    @Column(name = "scrum_count", nullable = false)
    private Short scrumCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScrumTitleStatus status = ScrumTitleStatus.PENDING;

    /** STAR 완료 시 호출. 해당 날짜 전체 ScrumTitle을 영구 저장 상태로 전환. */
    public void commit() {
        this.status = ScrumTitleStatus.COMMITTED;
    }
}