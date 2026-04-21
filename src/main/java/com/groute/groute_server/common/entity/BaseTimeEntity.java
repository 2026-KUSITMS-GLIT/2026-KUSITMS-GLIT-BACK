package com.groute.groute_server.common.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;

/**
 * 생성/수정 시각을 자동으로 관리하는 엔티티 부모 클래스.
 *
 * <p>모든 도메인 엔티티는 본 클래스 또는 {@link SoftDeleteEntity}를 상속한다. 동작하려면 애플리케이션 클래스에
 * {@code @EnableJpaAuditing}이 활성화되어 있어야 한다.
 *
 * <p>타임존 정책: DB는 {@code TIMESTAMPTZ}, Java 측은 UTC {@link OffsetDateTime}.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /** 레코드 생성 시각. insert 시 1회 set, 이후 변경 불가. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 레코드 마지막 수정 시각. update 시마다 갱신. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
