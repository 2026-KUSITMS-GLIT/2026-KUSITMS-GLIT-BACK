package com.groute.groute_server.common.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.groute.groute_server.common.response.ErrorResponse;
import com.groute.groute_server.common.webhook.ErrorWebhookNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리기.
 *
 * <p>모든 예외를 {@link ErrorResponse} 형식으로 변환하여 일관된 에러 응답을 보장한다. {@link BusinessException} 등 핸들링된 예외는
 * Discord 웹훅 알림 대상이 아니며, {@link #handleException} 로 떨어지는 unhandled 500만 {@link
 * ErrorWebhookNotifier}로 알린다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorWebhookNotifier errorWebhookNotifier;

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        List<ErrorResponse.FieldError> fieldErrors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        ErrorResponse.FieldError.of(
                                                error.getField(),
                                                error.getRejectedValue() == null
                                                        ? ""
                                                        : error.getRejectedValue().toString(),
                                                error.getDefaultMessage()))
                        .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        List<ErrorResponse.FieldError> fieldErrors =
                e.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        ErrorResponse.FieldError.of(
                                                violation.getPropertyPath().toString(),
                                                violation.getInvalidValue() == null
                                                        ? ""
                                                        : violation.getInvalidValue().toString(),
                                                violation.getMessage()))
                        .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ErrorCode.DUPLICATE_DATA));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn(
                "MethodArgumentTypeMismatchException: name={}, value={}",
                e.getName(),
                e.getValue());
        String requiredType =
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "?";
        ErrorResponse.FieldError fieldError =
                ErrorResponse.FieldError.of(
                        e.getName(),
                        e.getValue() == null ? "" : e.getValue().toString(),
                        "타입 변환 실패: " + requiredType + " 형식이어야 합니다.");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, List.of(fieldError)));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception: ", e);
        errorWebhookNotifier.notifyUnhandledError(e, request);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
