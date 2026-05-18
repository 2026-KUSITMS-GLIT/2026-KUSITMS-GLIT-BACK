package com.groute.groute_server.record.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.record.application.port.out.RecordHardDeletePort;

@ExtendWith(MockitoExtension.class)
class RecordAccountHardDeleteServiceTest {

    private static final Long USER_ID = 1L;

    @Mock RecordHardDeletePort recordHardDeletePort;
    @Mock PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @InjectMocks RecordAccountHardDeleteService service;

    @Nested
    @DisplayName("외부 스토리지 정리 (purgeExternalStorage) - 정상")
    class PurgeExternalStorageHappyPath {

        @Test
        @DisplayName("키가 없으면 S3 호출 미발생")
        void should_skipDeleteObject_when_noKeys() {
            // given
            given(recordHardDeletePort.findStarImageKeysByUserId(USER_ID)).willReturn(List.of());

            // when
            service.purgeExternalStorage(USER_ID);

            // then
            verifyNoInteractions(presignedUrlGeneratorPort);
        }

        @Test
        @DisplayName("키 목록을 받아 각 키마다 S3 deleteObject 호출")
        void should_callDeleteObjectForEachKey_when_keysExist() {
            // given
            given(recordHardDeletePort.findStarImageKeysByUserId(USER_ID))
                    .willReturn(List.of("a.jpg", "b.jpg", "c.jpg"));

            // when
            service.purgeExternalStorage(USER_ID);

            // then
            verify(presignedUrlGeneratorPort).deleteObject("a.jpg");
            verify(presignedUrlGeneratorPort).deleteObject("b.jpg");
            verify(presignedUrlGeneratorPort).deleteObject("c.jpg");
        }
    }

    @Nested
    @DisplayName("외부 스토리지 정리 (purgeExternalStorage) - 예외")
    class PurgeExternalStorageErrors {

        @Test
        @DisplayName("개별 S3 삭제 실패해도 다음 키 처리는 계속 (best-effort)")
        void should_continueOnFailure_when_someDeletesThrow() {
            // given
            given(recordHardDeletePort.findStarImageKeysByUserId(USER_ID))
                    .willReturn(List.of("a.jpg", "b.jpg", "c.jpg"));
            willThrow(new RuntimeException("S3 down"))
                    .given(presignedUrlGeneratorPort)
                    .deleteObject("b.jpg");

            // when
            service.purgeExternalStorage(USER_ID);

            // then
            verify(presignedUrlGeneratorPort, times(3))
                    .deleteObject(org.mockito.ArgumentMatchers.anyString());
            verify(presignedUrlGeneratorPort).deleteObject("a.jpg");
            verify(presignedUrlGeneratorPort).deleteObject("b.jpg");
            verify(presignedUrlGeneratorPort).deleteObject("c.jpg");
        }
    }

    @Nested
    @DisplayName("DB 정리 (purgeDb)")
    class PurgeDb {

        @Test
        @DisplayName("RecordHardDeletePort.hardDeleteAllByUserId 위임")
        void should_delegateToHardDeletePort_when_called() {
            // when
            service.purgeDb(USER_ID);

            // then
            verify(recordHardDeletePort).hardDeleteAllByUserId(USER_ID);
            verify(presignedUrlGeneratorPort, never())
                    .deleteObject(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    @DisplayName("키 조회 순서")
    class CallOrder {

        @Test
        @DisplayName("키 조회 → S3 삭제 순서로 호출")
        void should_queryKeysBeforeDelete_when_purgeExternalStorageCalled() {
            // given
            given(recordHardDeletePort.findStarImageKeysByUserId(USER_ID))
                    .willReturn(List.of("a.jpg"));

            // when
            service.purgeExternalStorage(USER_ID);

            // then
            InOrder inOrder = inOrder(recordHardDeletePort, presignedUrlGeneratorPort);
            inOrder.verify(recordHardDeletePort).findStarImageKeysByUserId(USER_ID);
            inOrder.verify(presignedUrlGeneratorPort).deleteObject("a.jpg");
        }
    }
}
