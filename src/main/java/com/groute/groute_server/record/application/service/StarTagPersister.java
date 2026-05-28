package com.groute.groute_server.record.application.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarTagSavePort;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** AI 태깅 결과를 검증한 뒤 star_tags 행을 저장한다. primaryCategory 누락·미일치 시 저장을 생략한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StarTagPersister {

    private final StarTagSavePort starTagSavePort;

    public void persist(StarRecord record, String primaryCategory, List<String> detailTags) {
        if (primaryCategory == null || primaryCategory.isBlank()) {
            log.warn(
                    "[AI Tagging] primaryCategory가 비어있어 star_tags 저장 생략 — starRecordId={}",
                    record.getId());
            return;
        }
        CompetencyCategory category;
        try {
            category = CompetencyCategory.valueOf(primaryCategory);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "[AI Tagging] primaryCategory 값 불일치 — value={}, starRecordId={}",
                    primaryCategory,
                    record.getId());
            return;
        }
        if (detailTags == null || detailTags.isEmpty()) {
            return;
        }
        List<StarTag> tags =
                detailTags.stream()
                        .map(detailTag -> StarTag.of(record, category, detailTag))
                        .toList();
        starTagSavePort.saveAll(tags);
    }
}
