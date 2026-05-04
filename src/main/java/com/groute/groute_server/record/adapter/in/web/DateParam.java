package com.groute.groute_server.record.adapter.in.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

/**
 * Web 컨트롤러용 LocalDate 쿼리 파라미터 파서.
 *
 * <p>Spring 기본 변환은 실패 시 500으로 떨어지므로, ISO 포맷(yyyy-MM-dd) 검증과 400 변환을 한곳에 모은다.
 */
final class DateParam {

    private DateParam() {}

    static LocalDate parseIso(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.CALENDAR_INVALID_DATE_FORMAT);
        }
    }
}
