package com.groute.groute_server.record.application.port.in.star;

import java.util.List;

public record BulkCreateStarRecordCommand(Long userId, List<Long> scrumIds) {}
