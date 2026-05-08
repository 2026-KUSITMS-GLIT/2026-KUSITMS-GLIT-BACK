package com.groute.groute_server.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 에러 코드 정의.
 *
 * <p>모든 에러 코드는 단일 enum에 정의하며, 도메인별 섹션 주석으로 구분한다. 코드 값은 {@code {DOMAIN}_{NNN}} 형식을 따른다 (예: {@code
 * COMMON_001}, {@code USER_001}).
 *
 * <pre>
 * // 도메인 에러 코드 추가 예시
 * // User
 * USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON_004", "요청 본문을 파싱할 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_005", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_006", "접근 권한이 없습니다."),
    DUPLICATE_DATA(HttpStatus.CONFLICT, "COMMON_007", "이미 존재하는 데이터입니다."),

    // Auth
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_001", "지원하지 않는 소셜 프로바이더입니다."),
    INVALID_OAUTH_RESPONSE(HttpStatus.BAD_GATEWAY, "AUTH_002", "소셜 프로바이더 응답 형식이 올바르지 않습니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_003", "리프레시 토큰이 전달되지 않았습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 리프레시 토큰입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "USER_002", "지원하지 않는 사용자 직군입니다."),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, "USER_003", "지원하지 않는 사용자 상태입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER_004", "이미 온보딩이 완료된 사용자입니다."),
    ONBOARDING_NOT_COMPLETED(HttpStatus.FORBIDDEN, "USER_005", "온보딩을 먼저 완료해주세요."),
    DUPLICATE_NOTIFICATION_SLOT(HttpStatus.BAD_REQUEST, "USER_006", "동일 요일은 한 번만 선택할 수 있어요."),
    NOTIFICATION_DAYS_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_007", "알림 활성화 시 최소 1개 이상의 요일을 선택해야 해요."),

    // Record
    TITLE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_001", "스크럼 제목을 찾을 수 없습니다."),
    SCRUM_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_002", "스크럼을 찾을 수 없습니다."),
    SCRUM_EDIT_LOCKED_14D(HttpStatus.CONFLICT, "RECORD_003", "2주 이상 지난 스크럼은 수정할 수 없어요."),
    SCRUM_EDIT_LOCKED_STAR(HttpStatus.CONFLICT, "RECORD_004", "심화기록이 작성된 스크럼은 수정할 수 없어요."),
    SCRUM_DATE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "RECORD_005", "하루에 작성할 수 있는 스크럼은 최대 5개입니다."),
    STAR_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_006", "심화기록을 찾을 수 없습니다."),
    STAR_FORBIDDEN(HttpStatus.FORBIDDEN, "RECORD_007", "본인의 심화기록만 접근할 수 있습니다."),
    SCRUM_COMPETENCY_UPDATE_LOCKED(HttpStatus.CONFLICT, "RECORD_008", "심화기록이 작성된 스크럼의 역량은 수정할 수 없어요."),

    // Record - StarRecord
    STAR_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "REC_001", "STAR 기록을 찾을 수 없습니다."),
    STAR_RECORD_NOT_READY(HttpStatus.BAD_REQUEST, "REC_002", "STAR 작성이 완료되지 않아 AI 태깅을 요청할 수 없습니다."),

    // Record - AI Tagging
    AI_TAGGING_ALREADY_RUNNING(HttpStatus.CONFLICT, "REC_003", "AI 태깅이 이미 진행 중입니다."),
    AI_TAGGING_PERMANENTLY_FAILED(HttpStatus.BAD_REQUEST, "REC_004", "AI 태깅이 최종 실패하여 재시도할 수 없습니다."),
    AI_TAGGING_NOT_COMPLETED(
            HttpStatus.BAD_REQUEST, "REC_005", "AI 태깅이 아직 완료되지 않아 결과를 조회할 수 없습니다."),
    AI_TAGGING_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "REC_006", "AI 태깅 잡을 찾을 수 없습니다."),

    // Calendar
    CALENDAR_INVALID_DATE_FORMAT(
            HttpStatus.BAD_REQUEST, "CALENDAR_001", "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)"),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_001", "프로젝트 태그를 찾을 수 없습니다."),
    PROJECT_NAME_DUPLICATE(HttpStatus.CONFLICT, "PROJECT_002", "이미 존재하는 프로젝트 태그 이름입니다."),
    PROJECT_HAS_RECORDS(HttpStatus.CONFLICT, "PROJECT_003", "연결된 기록이 있어 삭제할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
