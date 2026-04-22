package com.groute.groute_server.common.jwt;

/**
 * JWT 토큰 종류.
 *
 * <p>재발급(ONB001) 시 refresh 토큰만 허용하고, 인증 필터에서는 access 토큰만 허용하도록 구분하기 위해 claim으로 저장한다.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
