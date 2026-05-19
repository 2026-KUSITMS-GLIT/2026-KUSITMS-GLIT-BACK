package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand.ScrumCompetency;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

public record ScrumCompetencyUpdateRequest(
        @NotNull @NotEmpty @Valid List<ScrumCompetencyItem> items) {

    public UpdateScrumCompetencyCommand toCommand(Long userId) {
        List<ScrumCompetency> mapped =
                items.stream()
                        .map(item -> new ScrumCompetency(item.scrumId(), item.competency()))
                        .toList();
        return new UpdateScrumCompetencyCommand(userId, mapped);
    }

    public record ScrumCompetencyItem(
            @NotNull Long scrumId, @NotNull CompetencyCategory competency) {}
}
