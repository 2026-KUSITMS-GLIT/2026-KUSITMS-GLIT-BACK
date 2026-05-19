package com.groute.groute_server.record.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordCommand;
import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordResult;
import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordWritePort;
import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * 심화 기록 일괄 생성 서비스 (POST /api/star-records/bulk).
 *
 * <p>스크럼 선택 단계에서 호출되며, 선택된 스크럼마다 PENDING 상태의 StarRecord를 생성한다. 역량 선택(PATCH
 * /api/scrums/competencies) 전에 호출되어야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BulkCreateStarRecordService implements BulkCreateStarRecordUseCase {

    private final ScrumQueryPort scrumQueryPort;
    private final StarRecordWritePort starRecordWritePort;
    private final UserReferencePort userReferencePort;

    @Override
    public BulkCreateStarRecordResult bulkCreate(BulkCreateStarRecordCommand command) {
        List<Long> scrumIds = command.scrumIds();

        if (scrumIds.size() != scrumIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        List<Scrum> owned = scrumQueryPort.findAllByIdInAndUserId(scrumIds, command.userId());
        if (owned.size() != scrumIds.size()) {
            throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
        }

        if (owned.stream().anyMatch(Scrum::isHasStar)) {
            throw new BusinessException(ErrorCode.SCRUM_EDIT_LOCKED_STAR);
        }

        boolean allSameDate = owned.stream().map(Scrum::getScrumDate).distinct().count() == 1;
        if (!allSameDate) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        User userRef = userReferencePort.getReferenceById(command.userId());
        Map<Long, Scrum> scrumById = owned.stream().collect(Collectors.toMap(Scrum::getId, s -> s));

        List<Long> starRecordIds =
                scrumIds.stream()
                        .map(scrumId -> StarRecord.create(userRef, scrumById.get(scrumId)))
                        .map(starRecordWritePort::save)
                        .map(StarRecord::getId)
                        .toList();

        return new BulkCreateStarRecordResult(starRecordIds);
    }
}
