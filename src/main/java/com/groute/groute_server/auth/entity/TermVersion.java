package com.groute.groute_server.auth.entity;

import com.groute.groute_server.auth.enums.TermType;
import com.groute.groute_server.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 약관/개인정보처리방침 버전 관리.
 *
 * <p>약관 개정 시마다 새 row를 생성한다. 본문은 외부 호스팅(노션/CMS) URL 또는 S3에 두고 URL만 저장.
 * 클라이언트는 웹뷰로 본문을 노출한다(MYP006).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "term_versions")
public class TermVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 약관 종류(개인정보/이용약관/마케팅/만14세). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TermType type;

    /** 시맨틱 버전 문자열(예: "v1.0", "v1.1"). */
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    /** 노출용 제목(예: "개인정보 처리방침 v1.1"). */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 본문 호스팅 URL(웹뷰로 노출되는 페이지). */
    @Column(name = "content_url", nullable = false, length = 500)
    private String contentUrl;

    /** 시행일. 이 시각 이후 신규 가입자에게 적용. */
    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    /** 필수 동의 여부. false면 거부해도 가입/이용 가능(마케팅 등). */
    @Column(name = "is_required", nullable = false)
    private boolean isRequired;
}
