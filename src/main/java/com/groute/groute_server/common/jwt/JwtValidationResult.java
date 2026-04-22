package com.groute.groute_server.common.jwt;

/**
 * JWT 검증 결과.
 *
 * <p>인증 필터/재발급 API가 만료와 위조를 구분해 응답 코드를 분기(401 vs 401+재발급 유도)할 수 있도록 예외 대신 enum으로 반환한다.
 */
public enum JwtValidationResult {
    /** 서명·형식·만료 모두 유효. */
    VALID,
    /** 서명·형식은 정상이나 만료됨. 클라이언트가 재발급 플로우를 트리거해야 한다. */
    EXPIRED,
    /** 서명 불일치, 포맷 손상, 지원되지 않는 알고리즘 등 그 외 모든 실패. */
    INVALID
}
