package com.groute.groute_server.auth.entity;

import jakarta.persistence.*;

import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.entity.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM/APNs 푸시 알림 토큰.
 *
 * <p>알림 발송 시 user_id로 활성 토큰을 조회한다(MYP003).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /** 신규 디바이스 토큰 생성용 정적 팩토리. 활성 상태로 시작. */
    public static DeviceToken register(User user, DevicePlatform platform, String pushToken) {
        DeviceToken token = new DeviceToken();
        token.user = user;
        token.platform = platform;
        token.pushToken = pushToken;
        token.isActive = true;
        return token;
    }

    /**
     * 기존 토큰의 소유자 갱신. push_token unique 제약상 같은 토큰이 다른 유저로 재사용되는 케이스(공용 PC 등)를 위해 노출. 호출 후 {@link
     * #activate()}/{@link #updatePlatform(DevicePlatform)}와 함께 묶어 사용한다.
     */
    public void changeOwner(User user) {
        this.user = user;
    }

    /** 디바이스 플랫폼 정정. 같은 토큰이 환경 갱신으로 플랫폼이 바뀌는 케이스 대응. */
    public void updatePlatform(DevicePlatform platform) {
        this.platform = platform;
    }

    /** 발송 가능 상태로 전환. */
    public void activate() {
        this.isActive = true;
    }

    /** 발송 실패 토큰 비활성화. 스케줄러에서 FCM이 UNREGISTERED/INVALID_ARGUMENT 응답한 토큰에 호출. */
    public void deactivate() {
        this.isActive = false;
    }
}
