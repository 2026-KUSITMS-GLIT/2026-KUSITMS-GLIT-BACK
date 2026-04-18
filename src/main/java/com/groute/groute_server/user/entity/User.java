package com.groute.groute_server.user.entity;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, length = 12)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", nullable = false)
    private JobRole jobRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    @Column(name = "branding_title", length = 100)
    private String brandingTitle;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "hard_delete_at")
    private OffsetDateTime hardDeleteAt;
}