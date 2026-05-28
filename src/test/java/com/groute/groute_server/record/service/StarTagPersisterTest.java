package com.groute.groute_server.record.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.record.application.port.out.star.StarTagSavePort;
import com.groute.groute_server.record.application.service.StarTagPersister;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;

@ExtendWith(MockitoExtension.class)
class StarTagPersisterTest {

    private static final long STAR_RECORD_ID = 10L;

    @Mock StarTagSavePort starTagSavePort;

    @InjectMocks StarTagPersister starTagPersister;

    private StarRecord makeStarRecord() {
        StarRecord record = new StarRecord();
        ReflectionTestUtils.setField(record, "id", STAR_RECORD_ID);
        return record;
    }

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("성공 — primaryCategory와 detailTags가 유효하면 detailTag 수만큼 저장")
        void savesAllTags_whenInputsAreValid() {
            // given
            StarRecord record = makeStarRecord();
            List<String> detailTags = List.of("이해관계자 조율", "UX 설계");

            // when
            starTagPersister.persist(record, "DISCOVERY_ANALYSIS", detailTags);

            // then
            ArgumentCaptor<List<StarTag>> captor = ArgumentCaptor.forClass(List.class);
            verify(starTagSavePort, times(1)).saveAll(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("실패 — primaryCategory가 null이면 저장 생략")
        void skips_whenPrimaryCategoryIsNull() {
            // given
            StarRecord record = makeStarRecord();

            // when
            starTagPersister.persist(record, null, List.of("문제해결"));

            // then
            verify(starTagSavePort, never()).saveAll(any());
        }

        @Test
        @DisplayName("실패 — primaryCategory가 공백이면 저장 생략")
        void skips_whenPrimaryCategoryIsBlank() {
            // given
            StarRecord record = makeStarRecord();

            // when
            starTagPersister.persist(record, "   ", List.of("문제해결"));

            // then
            verify(starTagSavePort, never()).saveAll(any());
        }

        @Test
        @DisplayName("실패 — primaryCategory enum 미일치면 저장 생략")
        void skips_whenPrimaryCategoryIsInvalid() {
            // given
            StarRecord record = makeStarRecord();

            // when
            starTagPersister.persist(record, "NOT_A_CATEGORY", List.of("문제해결"));

            // then
            verify(starTagSavePort, never()).saveAll(any());
        }

        @Test
        @DisplayName("실패 — detailTags가 비어있으면 저장 생략")
        void skips_whenDetailTagsEmpty() {
            // given
            StarRecord record = makeStarRecord();

            // when
            starTagPersister.persist(record, "DISCOVERY_ANALYSIS", List.of());

            // then
            verify(starTagSavePort, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("실패 — detailTags가 null이면 저장 생략")
        void skips_whenDetailTagsNull() {
            // given
            StarRecord record = makeStarRecord();

            // when
            starTagPersister.persist(record, "DISCOVERY_ANALYSIS", null);

            // then
            verify(starTagSavePort, never()).saveAll(anyList());
        }
    }
}
