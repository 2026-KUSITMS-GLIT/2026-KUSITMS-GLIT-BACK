package com.groute.groute_server.record.domain.enums;

/** ScrumTitle 저장 상태. PENDING: 스크럼 작성 완료 전(STAR 완료 시 COMMITTED 전환). COMMITTED: 영구 저장 확정. */
public enum ScrumTitleStatus {
    PENDING,
    COMMITTED
}