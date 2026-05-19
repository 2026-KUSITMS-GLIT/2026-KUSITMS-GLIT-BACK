package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.record.application.port.in.star.StarDetailView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 심화기록 상세 조회(CAL-003) 응답 DTO. {@link StarDetailView}의 Web 표현 (flat schema). */
@Schema(description = "심화기록 상세 조회 응답")
public record StarDetailResponse(
        @Schema(description = "심화기록 식별자", example = "1") Long starRecordId,
        @Schema(description = "프로젝트 태그명 (최대 15자)", example = "밋업 프로젝트") String projectTag,
        @Schema(description = "제목 자유작성 (최대 20자)", example = "기획 작업") String freeText,
        @Schema(description = "대표 역량 (5대 역량 enum)", example = "PLANNING_EXECUTION")
                String primaryCategory,
        @Schema(description = "세부 역량 태그 목록 (최대 3개, 빈 배열 가능)", example = "[\"UX 설계\", \"품질 관리\"]")
                List<String> detailTags,
        @Schema(description = "S·T 단계 내용 (최대 300자)", example = "어드민 페이지 기획을 맡게 되었고...")
                String situationTask,
        @Schema(description = "A 단계 내용 (최대 300자)", example = "팀원들과 회의를 통해...") String action,
        @Schema(description = "R 단계 내용 (최대 300자)", example = "기능명세서를 완성하여...") String result,
        @Schema(description = "R 단계 첨부 이미지 목록 (최대 2장, 빈 배열 가능)") List<ImageResponse> images) {

    public static StarDetailResponse from(StarDetailView view) {
        return new StarDetailResponse(
                view.starRecordId(),
                view.projectTag(),
                view.freeText(),
                view.primaryCategory(),
                view.detailTags(),
                view.situationTask(),
                view.action(),
                view.result(),
                view.images().stream().map(ImageResponse::from).toList());
    }

    @Schema(description = "STAR R 단계 첨부 이미지")
    public record ImageResponse(
            @Schema(description = "이미지 식별자", example = "1") Long imageId,
            @Schema(description = "이미지 URL (최대 500자)", example = "https://...") String imageUrl,
            @Schema(description = "표시 순서 (0~1)", example = "0") int sortOrder) {

        static ImageResponse from(StarDetailView.ImageView image) {
            return new ImageResponse(image.imageId(), image.imageUrl(), image.sortOrder());
        }
    }
}
