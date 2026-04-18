package com.groute.groute_server.auth.enums;

/**
 * 약관 종류.
 * 회원가입 시 동의가 필요한 법적 약관 구분.
 */
public enum TermType {
    /** 개인정보 처리방침 (필수). */
    PRIVACY_POLICY,
    /** 서비스 이용약관 (필수). */
    TERMS_OF_SERVICE,
    /** 마케팅 정보 수신 동의 (선택, 자유롭게 철회 가능). */
    MARKETING,
    /** 만 14세 이상 확인 (필수, 개인정보보호법 §22조의2). */
    AGE_OVER_14
}
