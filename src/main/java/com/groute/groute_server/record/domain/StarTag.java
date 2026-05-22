package com.groute.groute_server.record.domain;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI가 추출한 역량 태그(REC006).
 *
 * <p>STAR 1개당 primary_category 1개 + detail_tag 1~3개 row가 생성된다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "star_tags")
public class StarTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star_record_id", nullable = false)
    private StarRecord starRecord;

    /** 대표 역량 1개. 홈 잔디 색상 결정(HOM002). REC007 완료 화면에 노출. */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_category", nullable = false)
    private CompetencyCategory primaryCategory;

    /** 세부 태그. AI가 자유롭게 생성(예: "이해관계자 조율"). REC007 완료 화면에 최대 3개 노출. */
    @Column(name = "detail_tag", nullable = false, length = 50)
    private String detailTag;

    /**
     * StarTag 생성 팩토리 메서드.
     *
     * @param starRecord 연결된 STAR 기록
     * @param primaryCategory 대표 역량
     * @param detailTag 세부 태그
     */
    public static StarTag of(
            StarRecord starRecord, CompetencyCategory primaryCategory, String detailTag) {
        StarTag tag = new StarTag();
        tag.starRecord = starRecord;
        tag.primaryCategory = primaryCategory;
        tag.detailTag = detailTag;
        return tag;
    }
}
