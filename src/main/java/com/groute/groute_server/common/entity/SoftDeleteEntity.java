package com.groute.groute_server.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 논리 삭제(Soft Delete) 지원 엔티티 부모 클래스
 */
@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseTimeEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}