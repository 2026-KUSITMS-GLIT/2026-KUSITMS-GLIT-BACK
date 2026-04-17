package com.groute.groute_server.record.domain;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "scrums")
public class Scrum extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private ScrumTitle title;

    @Column(name = "content", nullable = false, length = 50)
    private String content;

    @Column(name = "scrum_date", nullable = false)
    private LocalDate scrumDate;

    @Column(name = "has_star", nullable = false)
    private boolean hasStar = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_competency")
    private CompetencyCategory selectedCompetency;
}