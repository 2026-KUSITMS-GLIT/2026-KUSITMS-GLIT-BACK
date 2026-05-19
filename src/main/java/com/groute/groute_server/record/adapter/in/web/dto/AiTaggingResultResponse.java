package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.JobStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REC-007: AI 태깅 결과 조회 응답 DTO.
 *
 * <p>잡 상태가 SUCCESS일 때만 반환된다. star_tags 테이블에서 primary_category와 detail_tag를 조합해 구성한다.
 *
 * @param status 잡 상태 (항상 SUCCESS)
 * @param primaryCategory 대표 역량 1개. 홈 잔디 색상 결정에 사용 (HOM002)
 * @param detailTags 세부 태그 목록. AI가 자유롭게 생성, 최대 3개 (REC007)
 */
@Schema(description = "AI 태깅 결과 조회 응답")
public record AiTaggingResultResponse(
        @Schema(description = "잡 상태", example = "SUCCESS") JobStatus status,
        @Schema(description = "대표 역량", example = "DISCOVERY_ANALYSIS")
                CompetencyCategory primaryCategory,
        @Schema(description = "세부 태그 목록 (최대 3개)", example = "[\"이해관계자 조율\", \"UX 설계\", \"품질 관리\"]")
                List<String> detailTags) {

    /**
     * star_tags 목록에서 응답 DTO를 생성한다.
     *
     * <p>star_tags는 primary_category가 동일한 여러 row로 구성되므로, 첫 번째 row에서 primary_category를 가져오고 모든 row의
     * detail_tag를 수집한다.
     *
     * @param tags AI 태깅 결과 태그 목록 (1개 이상 보장)
     * @return 결과 응답 DTO
     * @throws BusinessException tags가 비어있으면 AI_TAGGING_JOB_NOT_FOUND 예외 발생
     */
    public static AiTaggingResultResponse from(List<StarTag> tags) {
        if (tags.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_TAGGING_JOB_NOT_FOUND);
        }
        CompetencyCategory primaryCategory = tags.get(0).getPrimaryCategory();
        List<String> detailTags = tags.stream().map(StarTag::getDetailTag).toList();
        return new AiTaggingResultResponse(JobStatus.SUCCESS, primaryCategory, detailTags);
    }
}
