package com.groute.groute_server.auth.enums;

/** 디바이스 플랫폼. FCM/APNs 푸시 토큰 등록 시 클라이언트 유형 구분 (MYP003). */
public enum DevicePlatform {
    /** iOS 디바이스. APNs 토큰 사용. */
    IOS,
    /** Android 디바이스. FCM 토큰 사용. */
    ANDROID,
    /** 웹 브라우저. */
    WEB
}
