package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.QueryStarImagesResult;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class QueryStarImagesServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_ID = 10L;

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarImageQueryPort starImageQueryPort;

    @InjectMocks QueryStarImagesService service;

    private User owner;
    private StarRecord record;

    @BeforeEach
    void setUp() {
        owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 20L);

        Scrum scrum = Scrum.create(owner, title, "스크럼 내용", LocalDate.of(2026, 5, 12));
        ReflectionTestUtils.setField(scrum, "id", 50L);

        record = StarRecord.create(owner, scrum);
        ReflectionTestUtils.setField(record, "id", STAR_ID);
    }

    @Nested
    @DisplayName("정상 조회")
    class HappyPath {

        @Test
        @DisplayName("이미지가 없으면 빈 리스트를 반환한다")
        void should_returnEmptyList_when_noImages() {
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());

            List<QueryStarImagesResult> result = service.query(USER_ID, STAR_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이미지가 있으면 sortOrder 순서대로 imageId와 imageUrl을 반환한다")
        void should_returnImages_inSortOrder() {
            StarImage img1 = starImage(101L, record, (short) 0, "https://cdn.example.com/a.jpg");
            StarImage img2 = starImage(102L, record, (short) 1, "https://cdn.example.com/b.jpg");
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(img1, img2));

            List<QueryStarImagesResult> result = service.query(USER_ID, STAR_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).imageId()).isEqualTo(101L);
            assertThat(result.get(0).imageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
            assertThat(result.get(1).imageId()).isEqualTo(102L);
            assertThat(result.get(1).imageUrl()).isEqualTo("https://cdn.example.com/b.jpg");
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.query(USER_ID, STAR_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
        }

        @Test
        @DisplayName("타 유저의 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.query(OTHER_USER_ID, STAR_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
        }
    }

    // ============== helpers ==============

    private static StarImage starImage(
            Long id, StarRecord record, short sortOrder, String imageUrl) {
        StarImage image =
                StarImage.create(
                        record,
                        "star-images/1/10/uuid.jpg",
                        imageUrl,
                        "image/jpeg",
                        1024,
                        sortOrder);
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
