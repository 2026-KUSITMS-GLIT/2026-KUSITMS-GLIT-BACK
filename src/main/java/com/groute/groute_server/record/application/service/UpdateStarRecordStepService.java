package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepUseCase;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.user.UserStreakPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.ScrumTitleStatus;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * STAR 단계별 저장 서비스 (POST /api/star-records/{id}/steps/{step}).
 *
 * <p>R 단계 저장 시 StarRecord를 완료 처리하고 연결된 Scrum의 hasStar를 true로 설정한다. ScrumTitle COMMITTED 전환과
 * 리포트/코치마크 팝업 트리거도 R단계 완료 시점에 함께 처리한다.
 *
 * <p>스크럼 임시저장(PENDING) 단계에서는 어떤 카운터도 올리지 않으므로, 확정 시점인 R단계 완료에서 {@code Project.titleCount}와 user
 * streak(연속 기록 일수)도 함께 반영한다. 심화기록을 완료하지 않고 이탈한 세션이 홈 지표에 남지 않도록 하기 위함이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStarRecordStepService implements UpdateStarRecordStepUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final ScrumWritePort scrumWritePort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final ProjectPort projectPort;
    private final UserStreakPort userStreakPort;
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
            commitSession(command.userId(), scrum.getScrumDate());

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

    /**
     * 해당 일자의 PENDING ScrumTitle을 COMMITTED로 확정하고, 확정으로 파생되는 카운터를 반영한다.
     *
     * <p>같은 일자에 STAR가 여러 건 완료되어도 두 번째 호출부터는 PENDING 대상이 없어 {@code Project.titleCount}가 중복 증가하지 않는다.
     * streak은 {@link User#recordOnDate}가 같은 일자·과거 일자를 멱등 처리하므로 조건 없이 호출한다.
     */
    private void commitSession(Long userId, LocalDate scrumDate) {
        Map<Long, ScrumTitle> pendingTitles = new LinkedHashMap<>();
        for (Scrum scrum : scrumQueryPort.findAllByUserAndDate(userId, scrumDate)) {
            ScrumTitle title = scrum.getTitle();
            if (title.getStatus() == ScrumTitleStatus.PENDING) {
                pendingTitles.putIfAbsent(title.getId(), title);
            }
        }

        if (!pendingTitles.isEmpty()) {
            scrumTitleRepositoryPort.commitAllByIds(pendingTitles.keySet());
            pendingTitles.values().stream()
                    .collect(
                            Collectors.groupingBy(
                                    t -> t.getProject().getId(), Collectors.counting()))
                    .forEach(
                            (projectId, count) ->
                                    projectPort.applyTitleCountIncrement(
                                            projectId, count.intValue()));
        }

        userStreakPort.recordOnDate(userId, scrumDate);
    }
}
