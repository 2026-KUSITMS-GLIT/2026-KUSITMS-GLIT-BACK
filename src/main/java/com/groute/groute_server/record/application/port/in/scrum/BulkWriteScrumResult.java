package com.groute.groute_server.record.application.port.in.scrum;

import java.util.List;

public record BulkWriteScrumResult(List<GroupResult> groups) {

    public record GroupResult(String projectName, String freeText, List<ScrumItem> scrums) {}

    public record ScrumItem(Long scrumId, String content) {}
}
