package com.groute.groute_server.auth.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.entity.User;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저별 약관 동의/철회 이력.
 *
 * <p>append-only 테이블 — 기존 row는 절대 UPDATE/DELETE하지 않는다(감사 추적 및 법적 증빙). 마케팅 동의 ON/OFF를 토글해도 매번 새 row를
 * INSERT한다. 이 규약은 DB 트리거가 아닌 애플리케이션 레벨에서 보장한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_term_agreements")
public class UserTermAgreement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 동의한 약관의 정확한 버전. 이후 약관이 개정돼도 동의 시점 스냅샷으로 보존된다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_version_id", nullable = false)
    private TermVersion termVersion;

    /** true=동의, false=철회. */
    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    /** 동의/철회 발생 시각. 법적 증빙 기준. */
    @Column(name = "agreed_at", nullable = false)
    private OffsetDateTime agreedAt;

    /** 동의 시점 IP(IPv6 대응 45자). 감사 강화용(선택). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 동의 시점 디바이스/브라우저 정보(선택). */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
