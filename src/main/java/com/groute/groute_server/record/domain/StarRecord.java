package com.groute.groute_server.record.domain;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "star_records")
public class StarRecord extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrum_id", nullable = false)
    private Scrum scrum;

    @Column(name = "situation_task", columnDefinition = "TEXT")
    private String situationTask;

    @Column(name = "action", columnDefinition = "TEXT")
    private String action;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private StarStep currentStep = StarStep.ST;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}