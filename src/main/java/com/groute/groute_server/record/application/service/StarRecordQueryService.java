package com.groute.groute_server.record.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.GetStarDetailQuery;
import com.groute.groute_server.record.application.port.in.star.GetStarDetailUseCase;
import com.groute.groute_server.record.application.port.in.star.StarDetailView;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;

import lombok.RequiredArgsConstructor;

/**
 * 심화기록 상세 조회 서비스 (CAL-003).
 *
 * <p>StarRecord + 연결된 Scrum/Title/Project + 역량 태그 + 이미지를 합쳐 프론트 합의 schema(flat)로 반환한다. 미존재(404)와 타인
 * 소유(403)를 구분한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StarRecordQueryService implements GetStarDetailUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarTagQueryPort starTagQueryPort;
    private final StarImageQueryPort starImageQueryPort;

    /**
     * 심화기록 상세 조회.
     *
     * <p>위반 시 도메인 예외: 미존재 → {@code STAR_NOT_FOUND}, 본인 소유 아님 → {@code STAR_FORBIDDEN}.
     */
    @Override
    public StarDetailView getStarDetail(GetStarDetailQuery query) {
        // 1. STAR + Scrum/Title/Project fetch join 로드, 미존재면 404
        StarRecord starRecord =
                starRecordRepositoryPort
                        .findByIdWithScrum(query.starRecordId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        // 2. 소유권 검증 (FK id만 보므로 user 테이블 조회는 발생하지 않음)
        if (!Objects.equals(starRecord.getUser().getId(), query.userId())) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        // 3. 역량 태그: primary 1 + detail 1~3 (모든 row의 primaryCategory는 동일)
        List<StarTag> tags = starTagQueryPort.findAllByStarRecordId(query.starRecordId());
        String primaryCategory = tags.isEmpty() ? null : tags.get(0).getPrimaryCategory().name();
        List<String> detailTags = tags.stream().map(StarTag::getDetailTag).toList();

        // 4. 이미지: sortOrder 오름차순, 없으면 빈 배열
        List<StarDetailView.ImageView> images =
                starImageQueryPort
                        .findAllByStarRecordIdOrderBySortOrder(query.starRecordId())
                        .stream()
                        .map(StarRecordQueryService::toImageView)
                        .toList();

        // 5. View 빌드 (메모리에 저장된 프론트 합의 schema)
        ScrumTitle title = starRecord.getScrum().getTitle();
        return new StarDetailView(
                starRecord.getId(),
                title.getProject().getName(),
                title.getFreeText(),
                primaryCategory,
                detailTags,
                starRecord.getSituationTask(),
                starRecord.getAction(),
                starRecord.getResult(),
                images);
    }

    private static StarDetailView.ImageView toImageView(StarImage image) {
        return new StarDetailView.ImageView(
                image.getId(), image.getImageUrl(), image.getSortOrder().intValue());
    }
}
