package com.groute.groute_server.record.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyCommand.ScrumCompetency;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.domain.Scrum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ScrumCompetencyUpdateService implements UpdateScrumCompetencyUseCase {

    private final ScrumQueryPort scrumQueryPort;
    private final ScrumWritePort scrumWritePort;

    @Override
    public void updateCompetency(UpdateScrumCompetencyCommand command) {
        List<Long> scrumIds = command.items().stream().map(ScrumCompetency::scrumId).toList();
        List<Scrum> owned = scrumQueryPort.findAllByIdInAndUserId(scrumIds, command.userId());

        if (owned.size() != scrumIds.size()) {
            throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
        }

        command.items()
                .forEach(
                        item -> scrumWritePort.updateCompetency(item.scrumId(), item.competency()));
    }
}
