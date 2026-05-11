package com.groute.groute_server.record.application.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarStep;

import lombok.RequiredArgsConstructor;

/**
 * STAR 단계별 저장 서비스 (POST /api/star-records/{id}/steps/{step}).
 *
 * <p>R 단계 저장 시 StarRecord를 완료 처리하고 연결된 Scrum의 hasStar를 true로 설정한다. ScrumTitle COMMITTED 전환은 StarTag
 * 생성 완료 시점에 별도로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStarRecordStepService implements UpdateStarRecordStepUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final ScrumWritePort scrumWritePort;

    @Override
    public void updateStep(UpdateStarRecordStepCommand command) {
        StarRecord record =
                starRecordRepositoryPort
                        .findByIdWithScrum(command.starRecordId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        if (!record.isOwnedBy(command.userId())) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        if (record.isWriteLocked()) {
            throw new BusinessException(ErrorCode.STAR_WRITE_LOCKED);
        }

        record.saveStep(command.step(), command.userAnswer());

        if (command.step() == StarStep.R) {
            record.complete(OffsetDateTime.now());
            Scrum scrum = record.getScrum();
            scrumWritePort.completeStar(scrum.getId());
        }
    }
}
