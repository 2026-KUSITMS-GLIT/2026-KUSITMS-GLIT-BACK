package com.groute.groute_server.record.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.record.application.port.in.star.DeleteStarCommand;
import com.groute.groute_server.record.application.port.in.star.DeleteStarUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/**
 * 심화기록 단독 삭제 서비스 (CAL-003).
 *
 * <p>STAR soft-delete + 연결된 StarImage S3·DB 정리 + Scrum의 hasStar 플래그 false 동기화를 한 트랜잭션에서 원자적으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StarRecordDeleteService implements DeleteStarUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    private final ScrumWritePort scrumWritePort;

    /**
     * 심화기록 단독 삭제.
     *
     * <p>위반 시 도메인 예외: 미존재(이미 삭제된 경우 포함) → {@code STAR_NOT_FOUND}, 본인 소유 아님 → {@code
     * STAR_FORBIDDEN}.
     */
    @Override
    public void deleteStar(DeleteStarCommand command) {
        // 1. STAR + Scrum fetch join 로드, 미존재(또는 이미 삭제)면 404
        StarRecord starRecord =
                starRecordRepositoryPort
                        .findByIdWithScrum(command.starRecordId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        // 2. 소유권 검증 (FK id만 보므로 user 테이블 조회는 발생하지 않음)
        if (!Objects.equals(starRecord.getUser().getId(), command.userId())) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        // 3. StarImage S3+DB 정리
        List<StarImage> images =
                starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(command.starRecordId());
        starImageWritePort.deleteAll(images);
        images.forEach(img -> presignedUrlGeneratorPort.deleteObject(img.getImageKey()));

        // 4. STAR soft-delete
        starRecordRepositoryPort.softDeleteById(command.starRecordId());

        // 5. Scrum.hasStar = false (스크럼 본문은 그대로)
        scrumWritePort.clearHasStar(starRecord.getScrum().getId());
    }
}
