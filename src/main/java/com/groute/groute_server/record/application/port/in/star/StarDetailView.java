package com.groute.groute_server.record.application.port.in.star;

import java.util.List;

/**
 * 심화기록 상세 조회(CAL-003) 응답 모델.
 *
 * <p>프론트와 합의된 스키마. {@code detailTags}/{@code images} 빈 배열은 UI 영역 숨김 신호로 사용된다.
 */
public record StarDetailView(
        Long starRecordId,
        String projectTag,
        String freeText,
        String primaryCategory,
        List<String> detailTags,
        String situationTask,
        String action,
        String result,
        List<ImageView> images) {

    /** STAR R 단계 첨부 이미지. */
    public record ImageView(Long imageId, String imageUrl, int sortOrder) {}
}
