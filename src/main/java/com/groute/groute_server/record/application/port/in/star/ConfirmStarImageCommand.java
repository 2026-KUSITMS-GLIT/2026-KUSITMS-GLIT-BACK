package com.groute.groute_server.record.application.port.in.star;

import java.util.List;

public record ConfirmStarImageCommand(Long userId, Long starRecordId, List<String> imageKeys) {}
