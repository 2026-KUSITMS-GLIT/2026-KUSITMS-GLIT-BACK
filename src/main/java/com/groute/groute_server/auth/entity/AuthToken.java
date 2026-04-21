package com.groute.groute_server.auth.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.entity.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 저장(액세스 토큰은 stateless).
 *
 * <p>재발급 요청 시 {@link #refreshTokenHash}로 유효성 검증(ONB001). 디바이스 단위로 추적하며, 로그아웃(MYP004)/탈퇴(MYP005) 시
 * {@link #revokedAt}을 set한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "auth_tokens")
public class AuthToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 리프레시 토큰의 SHA-256 해시. 평문 저장 금지(DB 유출 시 토큰 도용 방지). */
    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    /** 디바이스 식별자. 멀티 디바이스 로그인 추적용. */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** 토큰 만료 시각(예: 30일). 만료 토큰은 배치 잡이 정리. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** 로그아웃/탈퇴 시 set. NULL이면 활성 토큰. */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
}
