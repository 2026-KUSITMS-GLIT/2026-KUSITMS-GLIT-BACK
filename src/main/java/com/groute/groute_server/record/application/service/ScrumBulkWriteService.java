package com.groute.groute_server.record.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumCommand;
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumResult;
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumUseCase;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.enums.ScrumTitleStatus;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * 스크럼 일괄 저장 서비스 (POST /api/scrums/write).
 *
 * <p>심화 기록 플로우 진입 전 스크럼을 PENDING 상태로 저장한다. STAR 완료 시 COMMITTED로 전환되며, 미완료 시 스케줄러가 정리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScrumBulkWriteService implements BulkWriteScrumUseCase {

    private static final int MAX_SCRUMS_PER_DATE = 5;

    private final ProjectPort projectPort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final ScrumWritePort scrumWritePort;
    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    private final UserReferencePort userReferencePort;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    public BulkWriteScrumResult bulkWrite(BulkWriteScrumCommand command) {
        // 1. 해당 날짜 기존 세션 확인
        List<Scrum> existingScrums =
                scrumQueryPort.findAllByUserAndDate(command.userId(), command.date());
        if (!existingScrums.isEmpty()) {
            boolean hasCommitted =
                    existingScrums.stream()
                            .anyMatch(s -> s.getTitle().getStatus() == ScrumTitleStatus.COMMITTED);
            if (hasCommitted) {
                throw new BusinessException(ErrorCode.SCRUM_DATE_ALREADY_WRITTEN);
            }
            cleanupPendingSession(existingScrums);
        }

        // 2. 총 스크럼 수 ≤ 5
        if (command.totalScrumCount() > MAX_SCRUMS_PER_DATE) {
            throw new BusinessException(ErrorCode.SCRUM_DATE_LIMIT_EXCEEDED);
        }

        // 2. projectId 소유권 검증 + Project 로드 (요청 순서 보존)
        List<BulkWriteScrumCommand.GroupCommand> groups = command.groups();
        List<Project> projects =
                groups.stream()
                        .map(
                                g ->
                                        projectPort
                                                .findByIdAndUserId(g.projectId(), command.userId())
                                                .orElseThrow(
                                                        () ->
                                                                new BusinessException(
                                                                        ErrorCode
                                                                                .PROJECT_NOT_FOUND)))
                        .toList();

        // 3. User proxy reference (불필요한 SELECT 회피)
        User userRef = userReferencePort.getReferenceById(command.userId());

        // 4. ScrumTitle 생성(PENDING) + 일괄 저장
        List<ScrumTitle> titles =
                IntStream.range(0, groups.size())
                        .mapToObj(
                                i ->
                                        ScrumTitle.builder()
                                                .user(userRef)
                                                .project(projects.get(i))
                                                .freeText(groups.get(i).freeText())
                                                .build())
                        .toList();
        List<ScrumTitle> savedTitles = scrumTitleRepositoryPort.saveAll(titles);

        // 5. Scrum 생성 + 일괄 저장
        List<Scrum> scrums = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            ScrumTitle title = savedTitles.get(i);
            for (String content : groups.get(i).contents()) {
                scrums.add(Scrum.create(userRef, title, content, command.date()));
            }
        }
        List<Scrum> savedScrums = scrumWritePort.saveAll(scrums);

        // 6. Project.titleCount 증감 (동일 projectId 중복 시 count만큼)
        groups.stream()
                .collect(
                        Collectors.groupingBy(
                                BulkWriteScrumCommand.GroupCommand::projectId,
                                Collectors.counting()))
                .forEach(
                        (projectId, count) ->
                                projectPort.applyTitleCountIncrement(projectId, count.intValue()));

        // 7. 결과 조립 (저장 순서 보장)
        List<BulkWriteScrumResult.GroupResult> results = new ArrayList<>();
        int scrumIdx = 0;
        for (int i = 0; i < savedTitles.size(); i++) {
            int groupSize = groups.get(i).contents().size();
            List<BulkWriteScrumResult.ScrumItem> items =
                    savedScrums.subList(scrumIdx, scrumIdx + groupSize).stream()
                            .map(s -> new BulkWriteScrumResult.ScrumItem(s.getId(), s.getContent()))
                            .toList();
            scrumIdx += groupSize;
            results.add(
                    new BulkWriteScrumResult.GroupResult(
                            projects.get(i).getName(), savedTitles.get(i).getFreeText(), items));
        }
        return new BulkWriteScrumResult(results);
    }

    private void cleanupPendingSession(List<Scrum> scrums) {
        List<Long> scrumIds = scrums.stream().map(Scrum::getId).toList();
        List<Long> titleIds = scrums.stream().map(s -> s.getTitle().getId()).distinct().toList();

        List<StarImage> images = starImageQueryPort.findAllByScrumIdIn(scrumIds);
        starImageWritePort.deleteAll(images);
        List<String> keys = images.stream().map(StarImage::getImageKey).toList();
        afterCommitExecutor.execute(() -> keys.forEach(presignedUrlGeneratorPort::deleteObject));

        starRecordRepositoryPort.softDeleteByScrumIds(scrumIds);
        scrumWritePort.softDeleteAllByIdIn(scrumIds);
        scrumTitleRepositoryPort.softDeleteAllByIds(titleIds);

        scrums.stream()
                .collect(
                        Collectors.groupingBy(
                                s -> s.getTitle().getProject().getId(), Collectors.counting()))
                .forEach(
                        (projectId, count) ->
                                projectPort.applyTitleCountIncrement(projectId, -count.intValue()));
    }
}
