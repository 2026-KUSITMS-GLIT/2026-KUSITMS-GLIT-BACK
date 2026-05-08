package com.groute.groute_server.record.domain;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.user.entity.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 태그(사용자별 태그 원본).
 *
 * <p>스크럼 제목 구성 시 드롭다운으로 노출되며, 사용자별로 고유한 태그명을 가진다(REC002). {@link #titleCount}=0일 때만 태그 삭제가
 * 허용된다(심화기록 포함 카운팅 기준).
 */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Entity
@Table(name = "projects")
public class Project extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 태그명. 최대 15자(REC002). 사용자별 unique (is_deleted=false 기준). */
    @Column(name = "name", nullable = false, length = 15)
    private String name;

    /** 비정규화 카운터: 이 프로젝트에 연결된 scrum_titles 수. 0이면 태그 삭제 가능(REC002 태그 삭제 조건). */
    @Builder.Default
    @Column(name = "title_count", nullable = false)
    private Short titleCount = 0;

    /** 태그명 수정. */
    public void rename(String name) {
        this.name = name;
    }
}
