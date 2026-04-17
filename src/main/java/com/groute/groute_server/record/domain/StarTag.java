package com.groute.groute_server.record.domain;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_category", nullable = false)
    private CompetencyCategory primaryCategory;

    @Column(name = "detail_tag", nullable = false, length = 50)
    private String detailTag;
}