package com.groute.groute_server.record.application.port.in.scrum;

import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

public record UpdateScrumCompetencyCommand(Long userId, List<ScrumCompetency> items) {

    public record ScrumCompetency(Long scrumId, CompetencyCategory competency) {}
}
