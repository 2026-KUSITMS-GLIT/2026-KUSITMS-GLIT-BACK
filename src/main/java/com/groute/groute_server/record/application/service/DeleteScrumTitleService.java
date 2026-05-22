package com.groute.groute_server.record.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.scrumtitle.DeleteScrumTitleCommand;
import com.groute.groute_server.record.application.port.in.scrumtitle.DeleteScrumTitleUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.domain.Scrum;

import lombok.RequiredArgsConstructor;

/**
 * 스크럼 제목(freeText) 단위 일괄 삭제 서비스.
 *
 * <p>지정 ScrumTitle 산하의 모든 Scrum을 soft-delete 하고 연결된 STAR·첨부 이미지를 cascade 정리한 뒤 ScrumTitle 자체도
 * soft-delete 한다. 14일 편집 윈도우와 hasStar 거부 정책은 적용하지 않으며, 빈 산하(Scrum 0개) ScrumTitle도 정상 처리한다.
 * ScrumTitle.scrumCount는 별도 보정하지 않는다(Title 자체가 soft-delete 되어 어떤 reader도 참조하지 않음).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteScrumTitleService implements DeleteScrumTitleUseCase {

    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumWritePort scrumWritePort;
    private final StarRecordCascadePort starRecordCascadePort;
    private final StarImageCascadeCleaner starImageCascadeCleaner;

    @Override
    public void deleteScrumTitle(DeleteScrumTitleCommand command) {
        Long titleId = command.titleId();
        Long userId = command.userId();

        if (scrumTitleRepositoryPort.findAllByIdInAndUserId(Set.of(titleId), userId).isEmpty()) {
            throw new BusinessException(ErrorCode.TITLE_NOT_FOUND);
        }

        List<Scrum> scrums = scrumQueryPort.findAllByTitleIdAndUserId(titleId, userId);
        if (!scrums.isEmpty()) {
            Set<Long> scrumIds = scrums.stream().map(Scrum::getId).collect(Collectors.toSet());
            starImageCascadeCleaner.cleanupByScrumIds(scrumIds);
            scrumWritePort.softDeleteAllByIdIn(scrumIds);
            starRecordCascadePort.cascadeDeleteByScrumIdIn(scrumIds);
        }

        scrumTitleRepositoryPort.softDeleteAllByIds(List.of(titleId));
    }
}
