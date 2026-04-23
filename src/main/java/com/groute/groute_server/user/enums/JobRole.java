package com.groute.groute_server.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 사용자 직군. 온보딩 시 1회 선택하며(ONB004), 마이페이지에서 변경 가능(MYP002). AI 태깅 시 컨텍스트로 활용된다. */
@Getter
@RequiredArgsConstructor
public enum JobRole {
    /** 기획자. */
    PLANNER("기획자"),
    /** 개발자. */
    DEVELOPER("개발자"),
    /** 디자이너. */
    DESIGNER("디자이너");

    private final String label;

    /**
     * 한글 라벨을 enum 상수로 변환.
     *
     * <p>API 요청 바디에 담긴 사용자 친화 라벨("개발자" 등)을 내부 enum으로 매핑하기 위한 정적 팩토리. 매칭되는 라벨이 없으면 {@link
     * IllegalArgumentException}을 던지며, 호출부 계층(Service)에서 {@code BusinessException}으로 래핑해 일관된 400
     * 응답으로 변환한다.
     */
    public static JobRole fromLabel(String label) {
        for (JobRole role : values()) {
            if (role.label.equals(label)) {
                return role;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 직군 라벨입니다: " + label);
    }
}
