package com.groute.groute_server.user.dto.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 알림 시각 그리드 검증(MYP-004).
 *
 * <p>허용 슬롯: {@code 00:00}(자정) 또는 {@code 07:00}~{@code 23:30} 사이 30분 단위. DB CHECK 제약과 동일한 규칙을 어플리케이션
 * 계층에서도 강제해 4xx 응답으로 명시 거부한다.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HalfHourSlotValidator.class)
public @interface HalfHourSlot {

    String message() default "알림 시각은 00:00 또는 07:00~23:30 사이의 30분 단위여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
