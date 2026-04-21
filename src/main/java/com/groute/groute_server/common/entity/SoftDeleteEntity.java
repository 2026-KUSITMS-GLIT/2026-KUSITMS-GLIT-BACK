package com.groute.groute_server.common.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * 논리 삭제(Soft Delete)를 지원하는 엔티티 부모 클래스.
 *
 * <p>{@link BaseTimeEntity}를 상속하며, {@link #isDeleted}=true인 레코드는 모든 조회에서 제외해야 한다. 물리 삭제 대신 플래그 방식으로
 * 이력을 보존한다.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseTimeEntity {

    /** 논리 삭제 플래그. true 시 모든 조회에서 제외. */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /** 논리 삭제 시각. {@link #isDeleted}=true와 함께 set. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
