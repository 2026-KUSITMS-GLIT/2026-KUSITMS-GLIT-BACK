package com.groute.groute_server.record.application.port.in.scrum;

import java.time.LocalDate;
import java.util.List;

public record BulkWriteScrumCommand(Long userId, LocalDate date, List<GroupCommand> groups) {

    public int totalScrumCount() {
        return groups.stream().mapToInt(g -> g.contents().size()).sum();
    }

    public record GroupCommand(Long projectId, String freeText, List<String> contents) {}
}