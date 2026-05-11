package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.storage.PresignedUrlResult;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageCommand;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageResult;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class UploadStarImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_ID = 10L;
    private static final String MIME_TYPE = "image/jpeg";
    private static final int SIZE_BYTES = 1024;
    private static final String PRESIGNED_URL = "https://s3.example.com/presigned";
    private static final String IMAGE_URL = "https://cdn.example.com/image.jpg";

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarImageQueryPort starImageQueryPort;
    @Mock StarImageWritePort starImageWritePort;
    @Mock PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @InjectMocks UploadStarImageService service;

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
    @DisplayName("정상 업로드")
    class HappyPath {

        @Test
        @DisplayName("첫 번째 이미지 업로드 시 sortOrder=0으로 저장하고 결과를 반환한다")
        void should_uploadFirstImage_with_sortOrder0() {
            // given
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));
            StarImage saved = starImage(100L, record, (short) 0);
            given(starImageWritePort.save(any(StarImage.class))).willReturn(saved);

            // when
            UploadStarImageResult result = service.upload(command(USER_ID));

            // then
            assertThat(result.imageId()).isEqualTo(100L);
            assertThat(result.presignedUrl()).isEqualTo(PRESIGNED_URL);
            assertThat(result.imageUrl()).isEqualTo(IMAGE_URL);
            verify(starImageWritePort).save(any(StarImage.class));
        }

        @Test
        @DisplayName("두 번째 이미지 업로드 시 sortOrder=1로 저장한다")
        void should_uploadSecondImage_with_sortOrder1() {
            // given
            StarImage firstImage = starImage(101L, record, (short) 0);
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(firstImage));
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));
            StarImage saved = starImage(102L, record, (short) 1);
            given(starImageWritePort.save(any(StarImage.class))).willReturn(saved);

            // when
            UploadStarImageResult result = service.upload(command(USER_ID));

            // then
            assertThat(result.imageId()).isEqualTo(102L);
            verify(starImageWritePort).save(any(StarImage.class));
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.upload(command(USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("타 유저의 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.upload(command(OTHER_USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("완료된(WRITTEN) StarRecord면 STAR_WRITE_LOCKED를 던진다")
        void should_throwWriteLocked_when_alreadyCompleted() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            record.complete(java.time.OffsetDateTime.now());
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.upload(command(USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("이미 2장 존재하면 STAR_IMAGE_LIMIT_EXCEEDED를 던진다")
        void should_throwLimitExceeded_when_alreadyTwoImages() {
            StarImage img1 = starImage(101L, record, (short) 0);
            StarImage img2 = starImage(102L, record, (short) 1);
            given(starRecordRepositoryPort.findById(STAR_ID)).willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(img1, img2));

            assertThatThrownBy(() -> service.upload(command(USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
            verify(starImageWritePort, never()).save(any());
        }
    }

    // ============== helpers ==============

    private UploadStarImageCommand command(Long userId) {
        return new UploadStarImageCommand(userId, STAR_ID, MIME_TYPE, SIZE_BYTES);
    }

    private static StarImage starImage(Long id, StarRecord record, short sortOrder) {
        StarImage image =
                StarImage.create(
                        record,
                        "star-images/1/10/uuid.jpg",
                        IMAGE_URL,
                        MIME_TYPE,
                        SIZE_BYTES,
                        sortOrder);
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
