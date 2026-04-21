package com.groute.groute_server.user.enums;

/** 사용자 직군. 온보딩 시 1회 선택하며(ONB004), 마이페이지에서 변경 가능(MYP002). AI 태깅 시 컨텍스트로 활용된다. */
public enum JobRole {
    /** 기획자. */
    PLANNER,
    /** 개발자. */
    DEVELOPER,
    /** 디자이너. */
    DESIGNER
}
