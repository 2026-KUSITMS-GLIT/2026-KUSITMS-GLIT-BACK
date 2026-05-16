package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class DeleteStarImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_RECORD_ID = 10L;
    private static final Long OTHER_STAR_RECORD_ID = 999L;
    private static final Long IMAGE_ID = 200L;
    private static final String IMAGE_KEY = "star-images/1/10/uuid.jpg";
    private static final String IMAGE_URL = "https://cdn.example.com/image.jpg";

    @Mock StarImageQueryPort starImageQueryPort;
    @Mock StarImageWritePort starImageWritePort;
    @Mock PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    @Spy AfterCommitExecutor afterCommitExecutor;

    @InjectMocks DeleteStarImageService service;

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
        ReflectionTestUtils.setField(record, "id", STAR_RECORD_ID);
    }

    @Nested
    @DisplayName("정상 삭제")
    class HappyPath {

        @Test
        @DisplayName("DB 레코드 삭제 후 트랜잭션 커밋 시점에 S3 오브젝트를 삭제한다")
        void should_deleteDbThenScheduleS3Deletion_when_validRequest() {
            // given
            StarImage image = starImage(IMAGE_ID, record, (short) 0);
            given(starImageQueryPort.findById(IMAGE_ID)).willReturn(Optional.of(image));

            // when
            assertThatCode(() -> service.delete(USER_ID, STAR_RECORD_ID, IMAGE_ID))
                    .doesNotThrowAnyException();

            // then
            verify(presignedUrlGeneratorPort).deleteObject(IMAGE_KEY);
            verify(starImageWritePort).deleteById(IMAGE_ID);
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("URL 경로의 starRecordId와 이미지가 속한 StarRecord가 다르면 STAR_IMAGE_NOT_FOUND를 던진다")
        void should_throwImageNotFound_when_starRecordIdMismatch() {
            StarImage image = starImage(IMAGE_ID, record, (short) 0);
            given(starImageQueryPort.findById(IMAGE_ID)).willReturn(Optional.of(image));

            assertThatThrownBy(() -> service.delete(USER_ID, OTHER_STAR_RECORD_ID, IMAGE_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_NOT_FOUND);
            verify(presignedUrlGeneratorPort, never()).deleteObject(IMAGE_KEY);
            verify(starImageWritePort, never()).deleteById(IMAGE_ID);
        }

        @Test
        @DisplayName("존재하지 않는 imageId면 STAR_IMAGE_NOT_FOUND를 던진다")
        void should_throwImageNotFound_when_notExist() {
            given(starImageQueryPort.findById(IMAGE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(USER_ID, STAR_RECORD_ID, IMAGE_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_NOT_FOUND);
            verify(presignedUrlGeneratorPort, never()).deleteObject(IMAGE_KEY);
            verify(starImageWritePort, never()).deleteById(IMAGE_ID);
        }

        @Test
        @DisplayName("타 유저의 이미지면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            StarImage image = starImage(IMAGE_ID, record, (short) 0);
            given(starImageQueryPort.findById(IMAGE_ID)).willReturn(Optional.of(image));

            assertThatThrownBy(() -> service.delete(OTHER_USER_ID, STAR_RECORD_ID, IMAGE_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
            verify(presignedUrlGeneratorPort, never()).deleteObject(IMAGE_KEY);
            verify(starImageWritePort, never()).deleteById(IMAGE_ID);
        }

        @Test
        @DisplayName("완료된(WRITTEN) StarRecord의 이미지면 STAR_WRITE_LOCKED를 던진다")
        void should_throwWriteLocked_when_starCompleted() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            record.complete(OffsetDateTime.now());
            StarImage image = starImage(IMAGE_ID, record, (short) 0);
            given(starImageQueryPort.findById(IMAGE_ID)).willReturn(Optional.of(image));

            assertThatThrownBy(() -> service.delete(USER_ID, STAR_RECORD_ID, IMAGE_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
            verify(presignedUrlGeneratorPort, never()).deleteObject(IMAGE_KEY);
            verify(starImageWritePort, never()).deleteById(IMAGE_ID);
        }
    }

    // ============== helpers ==============

    private static StarImage starImage(Long id, StarRecord record, short sortOrder) {
        StarImage image =
                StarImage.create(record, IMAGE_KEY, IMAGE_URL, "image/jpeg", 1024, sortOrder);
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
