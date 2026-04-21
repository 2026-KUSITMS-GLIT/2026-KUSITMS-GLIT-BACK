package com.groute.groute_server.auth.entity;

import jakarta.persistence.*;

import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.entity.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저와 소셜 프로바이더 간 매핑.
 *
 * <p>한 유저가 여러 소셜 계정을 연결할 수 있다(ONB002). 로그인 시 (provider, provider_uid) 조합으로 기존 유저를 찾는다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "social_accounts")
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 소셜 프로바이더(카카오/구글/네이버). */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    /** 소셜 측 고유 ID. 로그인 시 (provider, provider_uid)로 유저를 조회한다. */
    @Column(name = "provider_uid", nullable = false)
    private String providerUid;

    /** 소셜에서 전달받은 이메일. 일부 프로바이더는 미제공이라 NULL 가능. */
    @Column(name = "email")
    private String email;
}
