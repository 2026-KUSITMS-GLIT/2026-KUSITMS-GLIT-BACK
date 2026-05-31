package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final String PRESIGNED_URL = "https://s3.example.com/presigned";
    private static final String IMAGE_URL = "https://cdn.example.com/image.jpg";

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarImageQueryPort starImageQueryPort;
    @Mock PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @InjectMocks UploadStarImageService service;

    private StarRecord record;

    @BeforeEach
    void setUp() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 20L);
        ReflectionTestUtils.setField(title, "user", owner);

        Scrum scrum = Scrum.create(owner, title, "스크럼 내용", LocalDate.of(2026, 5, 12));
        ReflectionTestUtils.setField(scrum, "id", 50L);

        record = StarRecord.create(owner, scrum);
        ReflectionTestUtils.setField(record, "id", STAR_ID);
    }

    @Nested
    @DisplayName("정상 발급")
    class HappyPath {

        @Test
        @DisplayName("1장 요청 시 presigned URL 1개를 반환한다")
        void should_returnOnePresignedUrl_when_oneMimeType() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));

            List<UploadStarImageResult> results = service.upload(command(USER_ID, MIME_JPEG));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).imageKey()).isNotBlank();
            assertThat(results.get(0).presignedUrl()).isEqualTo(PRESIGNED_URL);
            assertThat(results.get(0).imageUrl()).isEqualTo(IMAGE_URL);
        }

        @Test
        @DisplayName("2장 동시 요청 시 presigned URL 2개를 반환한다")
        void should_returnTwoPresignedUrls_when_twoMimeTypes() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));

            List<UploadStarImageResult> results =
                    service.upload(command(USER_ID, MIME_JPEG, MIME_PNG));

            assertThat(results).hasSize(2);
            verify(presignedUrlGeneratorPort, times(2)).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("기존 1장 있을 때 1장 추가 요청은 정상 발급된다")
        void should_returnPresignedUrl_when_oneExistingAndOneRequested() {
            StarImage firstImage =
                    StarImage.create(record, "star-images/1/10/uuid.jpg", IMAGE_URL, (short) 0);
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(firstImage));
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));

            List<UploadStarImageResult> results = service.upload(command(USER_ID, MIME_JPEG));

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("image/png mimeType이면 S3 키 확장자가 png다")
        void should_generatePngKey_when_mimeTypeIsPng() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.generate(anyString(), anyString()))
                    .willReturn(new PresignedUrlResult(PRESIGNED_URL, IMAGE_URL));

            service.upload(command(USER_ID, MIME_PNG));

            verify(presignedUrlGeneratorPort)
                    .generate(argThat(key -> key.endsWith(".png")), anyString());
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.upload(command(USER_ID, MIME_JPEG)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("타 유저의 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.upload(command(OTHER_USER_ID, MIME_JPEG)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("완료된(WRITTEN) StarRecord면 STAR_WRITE_LOCKED를 던진다")
        void should_throwWriteLocked_when_alreadyCompleted() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            record.complete(java.time.OffsetDateTime.now());
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.upload(command(USER_ID, MIME_JPEG)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("이미 2장 존재하면 STAR_IMAGE_LIMIT_EXCEEDED를 던진다")
        void should_throwLimitExceeded_when_alreadyTwoImages() {
            StarImage img1 =
                    StarImage.create(record, "star-images/1/10/a.jpg", IMAGE_URL, (short) 0);
            StarImage img2 =
                    StarImage.create(record, "star-images/1/10/b.jpg", IMAGE_URL, (short) 1);
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(img1, img2));

            assertThatThrownBy(() -> service.upload(command(USER_ID, MIME_JPEG)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
        }

        @Test
        @DisplayName("기존 1장 + 요청 2장이면 합계 초과로 STAR_IMAGE_LIMIT_EXCEEDED를 던진다")
        void should_throwLimitExceeded_when_existingPlusRequestedExceedsMax() {
            StarImage img1 =
                    StarImage.create(record, "star-images/1/10/a.jpg", IMAGE_URL, (short) 0);
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of(img1));

            assertThatThrownBy(() -> service.upload(command(USER_ID, MIME_JPEG, MIME_PNG)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
            verify(presignedUrlGeneratorPort, never()).generate(anyString(), anyString());
        }
    }

    // ============== helpers ==============

    private UploadStarImageCommand command(Long userId, String... mimeTypes) {
        return new UploadStarImageCommand(userId, STAR_ID, List.of(mimeTypes));
    }
}
