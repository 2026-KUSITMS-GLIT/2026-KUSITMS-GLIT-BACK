package com.groute.groute_server.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 사용자 현재 상태. 온보딩 시 선택(ONB005), 마이페이지에서 자유롭게 변경 가능(MYP002). */
@Getter
@RequiredArgsConstructor
public enum UserStatus {
    /** 재학 중. */
    STUDENT("재학 중"),
    /** 취업 준비 중. */
    JOB_SEEKER("취업 준비 중"),
    /** 재직 중. */
    EMPLOYED("재직 중");

    private final String label;

    /**
     * 한글 라벨을 enum 상수로 변환.
     *
     * <p>API 요청 바디에 담긴 사용자 친화 라벨("재학 중" 등)을 내부 enum으로 매핑하기 위한 정적 팩토리. 매칭되는 라벨이 없으면 {@link
     * IllegalArgumentException}을 던지며, 호출부 계층(Service)에서 {@code BusinessException}으로 래핑해 일관된 400
     * 응답으로 변환한다.
     */
    public static UserStatus fromLabel(String label) {
        for (UserStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 상태 라벨입니다: " + label);
    }
}
