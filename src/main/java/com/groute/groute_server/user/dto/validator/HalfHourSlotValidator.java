package com.groute.groute_server.user.dto.validator;

import java.time.LocalTime;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** {@link HalfHourSlot} 어노테이션의 검증 로직(MYP-004 시간 그리드). */
public class HalfHourSlotValidator implements ConstraintValidator<HalfHourSlot, LocalTime> {

    private static final LocalTime LOWER_INCLUSIVE = LocalTime.of(7, 0);
    private static final LocalTime UPPER_INCLUSIVE = LocalTime.of(23, 30);

    @Override
    public boolean isValid(LocalTime value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.getSecond() != 0 || value.getNano() != 0) {
            return false;
        }
        int minute = value.getMinute();
        if (minute != 0 && minute != 30) {
            return false;
        }
        if (value.equals(LocalTime.MIDNIGHT)) {
            return true;
        }
        return !value.isBefore(LOWER_INCLUSIVE) && !value.isAfter(UPPER_INCLUSIVE);
    }
}
