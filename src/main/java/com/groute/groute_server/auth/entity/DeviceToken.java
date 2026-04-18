package com.groute.groute_server.auth.entity;

import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM/APNs 푸시 알림 토큰.
 *
 * <p>알림 발송 시 user_id로 활성 토큰을 조회한다(MYP003).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "device_tokens")
public class DeviceToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 디바이스 플랫폼(iOS/Android/Web). */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private DevicePlatform platform;

    /** FCM/APNs 디바이스 토큰. */
    @Column(name = "push_token", nullable = false)
    private String pushToken;

    /** 활성 여부. 발송 실패 시 false로 전환해 차기 발송 대상에서 제외. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
