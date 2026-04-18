package com.groute.groute_server.user.enums;

/**
 * 사용자 현재 상태.
 * 온보딩 시 선택(ONB005), 마이페이지에서 자유롭게 변경 가능(MYP002).
 */
public enum UserStatus {
    /** 재학 중. */
    STUDENT,
    /** 취업 준비 중. */
    JOB_SEEKER,
    /** 재직 중. */
    EMPLOYED
}
