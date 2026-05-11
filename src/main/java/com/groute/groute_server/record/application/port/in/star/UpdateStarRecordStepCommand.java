package com.groute.groute_server.record.application.port.in.star;

import com.groute.groute_server.record.domain.enums.StarStep;

public record UpdateStarRecordStepCommand(
        Long userId, Long starRecordId, StarStep step, String userAnswer) {}
