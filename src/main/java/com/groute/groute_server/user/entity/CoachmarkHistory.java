package com.groute.groute_server.user.entity;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "coachmark_key", nullable = false, length = 50)
    private String coachmarkKey;

    @Column(name = "shown_at", nullable = false)
    private LocalDateTime shownAt;
}