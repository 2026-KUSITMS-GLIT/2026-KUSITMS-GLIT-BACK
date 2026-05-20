package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageCommand;
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
class ConfirmStarImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_ID = 10L;
    private static final String IMAGE_KEY_1 = "star-images/1/10/uuid1.jpg";
    private static final String IMAGE_KEY_2 = "star-images/1/10/uuid2.png";
    private static final String IMAGE_URL = "https://cdn.example.com/star-images/1/10/uuid1.jpg";

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarImageQueryPort starImageQueryPort;
    @Mock StarImageWritePort starImageWritePort;
    @Mock PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @InjectMocks ConfirmStarImageService service;

    private StarRecord record;

    @BeforeEach
    void setUp() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 20L);

        Scrum scrum = Scrum.create(owner, title, "스크럼 내용", LocalDate.of(2026, 5, 12));
        ReflectionTestUtils.setField(scrum, "id", 50L);

        record = StarRecord.create(owner, scrum);
        ReflectionTestUtils.setField(record, "id", STAR_ID);
    }

    @Nested
    @DisplayName("정상 등록")
    class HappyPath {

        @Test
        @DisplayName("1장 confirm 시 sortOrder=0으로 DB에 저장된다")
        void should_saveOneImage_with_sortOrder0() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.toImageUrl(IMAGE_KEY_1)).willReturn(IMAGE_URL);
            given(starImageWritePort.save(any(StarImage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            service.confirm(command(USER_ID, IMAGE_KEY_1));

            verify(starImageWritePort, times(1)).save(any(StarImage.class));
        }

        @Test
        @DisplayName("2장 동시 confirm 시 sortOrder=0,1로 각각 DB에 저장된다")
        void should_saveTwoImages_with_sortOrder0And1() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            given(presignedUrlGeneratorPort.toImageUrl(anyString())).willReturn(IMAGE_URL);
            given(starImageWritePort.save(any(StarImage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            service.confirm(command(USER_ID, IMAGE_KEY_1, IMAGE_KEY_2));

            verify(starImageWritePort, times(2)).save(any(StarImage.class));
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirm(command(USER_ID, IMAGE_KEY_1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("타 유저의 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.confirm(command(OTHER_USER_ID, IMAGE_KEY_1)))
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
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.confirm(command(USER_ID, IMAGE_KEY_1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("imageKey 경로가 요청자 userId·starRecordId와 불일치하면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_imageKeyPrefixMismatch() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());
            ConfirmStarImageCommand tamperedCommand =
                    new ConfirmStarImageCommand(
                            USER_ID, STAR_ID, List.of("star-images/999/999/hack.jpg"));

            assertThatThrownBy(() -> service.confirm(tamperedCommand))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
            verify(starImageWritePort, never()).save(any());
        }

        @Test
        @DisplayName("중복 imageKey가 있으면 INVALID_INPUT을 던진다")
        void should_throwInvalidInput_when_duplicateImageKeys() {
            given(starRecordRepositoryPort.findByIdWithLock(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(
                            () ->
                                    service.confirm(
                                            new ConfirmStarImageCommand(
                                                    USER_ID,
                                                    STAR_ID,
                                                    List.of(IMAGE_KEY_1, IMAGE_KEY_1))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
            verify(starImageWritePort, never()).save(any());
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

            assertThatThrownBy(() -> service.confirm(command(USER_ID, IMAGE_KEY_1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
            verify(presignedUrlGeneratorPort, never()).toImageUrl(anyString());
            verify(starImageWritePort, never()).save(any());
        }
    }

    // ============== helpers ==============

    private ConfirmStarImageCommand command(Long userId, String... imageKeys) {
        return new ConfirmStarImageCommand(userId, STAR_ID, List.of(imageKeys));
    }
}
