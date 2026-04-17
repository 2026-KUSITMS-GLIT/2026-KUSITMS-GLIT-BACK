package com.groute.groute_server.user.entity;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification_settings")
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "notify_time", nullable = false)
    private LocalTime notifyTime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}