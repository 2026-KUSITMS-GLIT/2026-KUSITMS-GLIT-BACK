package com.groute.groute_server.record.application.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepUseCase;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * STAR 단계별 저장 서비스 (POST /api/star-records/{id}/steps/{step}).
 *
 * <p>R 단계 저장 시 StarRecord를 완료 처리하고 연결된 Scrum의 hasStar를 true로 설정한다. ScrumTitle COMMITTED 전환과
 * 리포트/코치마크 팝업 트리거도 R단계 완료 시점에 함께 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStarRecordStepService implements UpdateStarRecordStepUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final ScrumWritePort scrumWritePort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final UserPort userPort;

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
            if (scrumWritePort.completeStar(scrum.getId()) == 0) {
                throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
            }
            List<Long> titleIds =
                    scrumQueryPort
                            .findAllByUserAndDate(command.userId(), scrum.getScrumDate())
                            .stream()
                            .map(Scrum::getTitle)
                            .map(t -> t.getId())
                            .distinct()
                            .toList();
            scrumTitleRepositoryPort.commitAllByIds(titleIds);

            long completedCount = starRecordRepositoryPort.countCompleted(command.userId());
            User user = userPort.findById(command.userId());
            if (completedCount == 1) {
                user.markPendingCoachMark();
            }
            if (completedCount == 10) {
                user.markPendingReportModal("MINI");
            } else if (completedCount >= 20 && completedCount % 10 == 0) {
                user.markPendingReportModal("FULL");
            }
        }
    }
}
