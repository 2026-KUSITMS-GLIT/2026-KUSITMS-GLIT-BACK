package com.groute.groute_server.record.domain.enums;

/**
 * 5대 역량 카테고리.
 * 사용자가 STAR 작성 시 선택하거나(REC004) AI 태깅이 자동 분류하며,
 * 홈 잔디/레이더 차트의 색상과 집계 기준이 된다.
 */
public enum CompetencyCategory {
    /** 발견/분석. */
    DISCOVERY_ANALYSIS,
    /** 기획/실행. */
    PLANNING_EXECUTION,
    /** 협업/조율. */
    COLLABORATION,
    /** 문제해결/개선. */
    PROBLEM_SOLVING,
    /** 성찰/성장. */
    REFLECTION_GROWTH
}
