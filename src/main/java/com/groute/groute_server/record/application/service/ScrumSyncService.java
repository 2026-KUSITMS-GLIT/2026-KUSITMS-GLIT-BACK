package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumCommand;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * 일자별 스크럼 일괄 sync 서비스 (CAL-002).
 *
 * <p>요청 payload만 살아남는 sync 시맨틱. 추가/수정/삭제를 한 트랜잭션에서 atomic 처리하며, 14일·STAR·소유권 검증은 변경 대상에 한해 수행한다. 삭제
 * 시 연결된 STAR 기록도 함께 cascade 삭제한다. 응답 본문은 비우며, 클라이언트는 후속 GET으로 최신 목록을 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScrumSyncService implements SyncDailyScrumUseCase {

    private static final int EDIT_WINDOW_DAYS = 14;
    private static final int MAX_ITEMS_PER_DATE = 5;

    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumWritePort scrumWritePort;
    private final StarRecordCascadePort starRecordCascadePort;
    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    private final UserReferencePort userReferencePort;

    /**
     * 일자별 스크럼을 요청 payload 상태로 동기화.
     *
     * <p>위반 1건이라도 발견되면 트랜잭션 전체 롤백.
     */
    @Override
    public void syncDailyScrum(SyncDailyScrumCommand command) {
        // 1. 일자당 5개 제한 (요청 item 총합)
        int totalItems = command.groups().stream().mapToInt(g -> g.items().size()).sum();
        if (totalItems > MAX_ITEMS_PER_DATE) {
            throw new BusinessException(ErrorCode.SCRUM_DATE_LIMIT_EXCEEDED);
        }

        // 2. 요청 titleId 일괄 소유권 검증
        Set<Long> requestedTitleIds =
                command.groups().stream()
                        .map(SyncDailyScrumCommand.GroupCommand::titleId)
                        .collect(Collectors.toSet());
        Map<Long, ScrumTitle> titleById =
                scrumTitleRepositoryPort
                        .findAllByIdInAndUserId(requestedTitleIds, command.userId())
                        .stream()
                        .collect(Collectors.toMap(ScrumTitle::getId, t -> t));
        if (titleById.size() != requestedTitleIds.size()) {
            throw new BusinessException(ErrorCode.TITLE_NOT_FOUND);
        }

        // 3. 요청 scrumId(non-null) 일괄 소유권 검증
        Set<Long> requestedScrumIds =
                command.groups().stream()
                        .flatMap(g -> g.items().stream())
                        .map(SyncDailyScrumCommand.ItemCommand::scrumId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, Scrum> requestedScrumById =
                scrumQueryPort.findAllByIdInAndUserId(requestedScrumIds, command.userId()).stream()
                        .collect(Collectors.toMap(Scrum::getId, s -> s));
        if (requestedScrumById.size() != requestedScrumIds.size()) {
            throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
        }

        // 4. 현재 DB Scrum 로드 (해당 일자) + 다른 일자 scrumId 거부
        List<Scrum> currentScrums =
                scrumQueryPort.findAllByUserAndDate(command.userId(), command.date());
        Set<Long> currentScrumIds =
                currentScrums.stream().map(Scrum::getId).collect(Collectors.toSet());
        if (!currentScrumIds.containsAll(requestedScrumIds)) {
            throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
        }

        // 5. 변경 분류 + 14일·STAR 검증 (변경 대상에 한해)
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        List<UpdateOp> updates = new ArrayList<>();
        Map<Long, Integer> titleDeltas = new HashMap<>();

        // 5a. 수정·신규 분류
        for (SyncDailyScrumCommand.GroupCommand group : command.groups()) {
            ScrumTitle title = titleById.get(group.titleId());
            for (SyncDailyScrumCommand.ItemCommand item : group.items()) {
                if (item.scrumId() == null) {
                    titleDeltas.merge(title.getId(), 1, Integer::sum);
                    continue;
                }
                Scrum existing = requestedScrumById.get(item.scrumId());
                if (!existing.getContent().equals(item.content())) {
                    rejectIfLocked(existing, today, zone);
                    updates.add(new UpdateOp(existing.getId(), item.content()));
                }
            }
        }

        // 5b. 삭제 분류 (현재 DB에 있는데 요청에 없음)
        List<Scrum> toDelete =
                currentScrums.stream().filter(s -> !requestedScrumIds.contains(s.getId())).toList();
        for (Scrum scrum : toDelete) {
            rejectIfLocked(scrum, today, zone);
            titleDeltas.merge(scrum.getTitle().getId(), -1, Integer::sum);
        }

        // 6. 신규 Scrum 빌드
        List<Scrum> toCreate = new ArrayList<>();
        if (titleDeltas.values().stream().anyMatch(v -> v > 0)) {
            User userRef = userReferencePort.getReferenceById(command.userId());
            for (SyncDailyScrumCommand.GroupCommand group : command.groups()) {
                ScrumTitle title = titleById.get(group.titleId());
                for (SyncDailyScrumCommand.ItemCommand item : group.items()) {
                    if (item.scrumId() == null) {
                        toCreate.add(Scrum.create(userRef, title, item.content(), command.date()));
                    }
                }
            }
        }

        // 7. 반영 (create → update → delete + cascade)
        scrumWritePort.saveAll(toCreate);
        for (UpdateOp op : updates) {
            scrumWritePort.updateContent(op.scrumId(), op.content());
        }
        if (!toDelete.isEmpty()) {
            Set<Long> deleteIds = toDelete.stream().map(Scrum::getId).collect(Collectors.toSet());
            List<StarImage> images = starImageQueryPort.findAllByScrumIdIn(deleteIds);
            images.forEach(img -> presignedUrlGeneratorPort.deleteObject(img.getImageKey()));
            starImageWritePort.deleteAll(images);
            scrumWritePort.softDeleteAllByIdIn(deleteIds);
            starRecordCascadePort.cascadeDeleteByScrumIdIn(deleteIds);
        }

        // 8. 비정규화 카운터 갱신
        for (Map.Entry<Long, Integer> entry : titleDeltas.entrySet()) {
            scrumTitleRepositoryPort.applyScrumCountIncrement(entry.getKey(), entry.getValue());
        }
    }

    /** 14일 초과 또는 hasStar=true인 변경 대상이면 예외. */
    private void rejectIfLocked(Scrum scrum, LocalDate today, ZoneId zone) {
        if (scrum.isHasStar()) {
            throw new BusinessException(ErrorCode.SCRUM_EDIT_LOCKED_STAR);
        }
        LocalDate createdDate = scrum.getCreatedAt().atZoneSameInstant(zone).toLocalDate();
        if (createdDate.plusDays(EDIT_WINDOW_DAYS).isBefore(today)) {
            throw new BusinessException(ErrorCode.SCRUM_EDIT_LOCKED_14D);
        }
    }

    private record UpdateOp(Long scrumId, String content) {}
}
