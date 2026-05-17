package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.util.Optional;

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
import com.groute.groute_server.record.application.port.in.star.DeleteStarCommand;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class StarRecordDeleteServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_ID = 10L;
    private static final Long SCRUM_ID = 50L;

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarImageCascadeCleaner starImageCascadeCleaner;
    @Mock ScrumWritePort scrumWritePort;

    @InjectMocks StarRecordDeleteService service;

    @Nested
    @DisplayName("정상 삭제")
    class HappyPath {

        @Test
        @DisplayName("STAR soft-delete + Scrum.hasStar=false 호출을 수행한다")
        void should_softDeleteStarAndClearHasStar_when_owned() {
            // given
            StarRecord star = star(STAR_ID, USER_ID, SCRUM_ID);
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));
            // when
            assertThatCode(() -> service.deleteStar(new DeleteStarCommand(USER_ID, STAR_ID)))
                    .doesNotThrowAnyException();

            // then
            verify(starRecordRepositoryPort).softDeleteById(STAR_ID);
            verify(scrumWritePort).clearHasStar(SCRUM_ID);
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("미존재 STAR면 STAR_NOT_FOUND를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwStarNotFound_when_missing() {
            // given
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.deleteStar(new DeleteStarCommand(USER_ID, STAR_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
            verify(starRecordRepositoryPort, never()).softDeleteById(anyLong());
            verify(scrumWritePort, never()).clearHasStar(anyLong());
        }

        @Test
        @DisplayName("타 유저의 STAR면 STAR_FORBIDDEN을 던지고 어떤 쓰기도 하지 않는다")
        void should_throwStarForbidden_when_otherUser() {
            // given — STAR는 OTHER_USER_ID 소유, 요청은 USER_ID
            StarRecord star = star(STAR_ID, OTHER_USER_ID, SCRUM_ID);
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));

            // when & then
            assertThatThrownBy(() -> service.deleteStar(new DeleteStarCommand(USER_ID, STAR_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
            verify(starRecordRepositoryPort, never()).softDeleteById(anyLong());
            verify(scrumWritePort, never()).clearHasStar(anyLong());
        }

        @Test
        @DisplayName("이미 soft-delete된 STAR(레포가 isDeleted=false 필터로 빈 결과 반환)는 멱등하게 STAR_NOT_FOUND")
        void should_throwStarNotFound_when_alreadyDeleted() {
            // given — 두 번째 삭제 호출 가정: 레포의 findByIdWithScrum이 isDeleted=false 필터로 Optional.empty 반환
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.deleteStar(new DeleteStarCommand(USER_ID, STAR_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
            verify(starRecordRepositoryPort, never()).softDeleteById(anyLong());
            verify(scrumWritePort, never()).clearHasStar(anyLong());
        }
    }

    // ============== helpers ==============

    private static StarRecord star(Long id, Long ownerUserId, Long scrumId) {
        StarRecord star = new StarRecord();
        ReflectionTestUtils.setField(star, "id", id);
        ReflectionTestUtils.setField(star, "user", user(ownerUserId));
        ReflectionTestUtils.setField(star, "scrum", scrum(scrumId));
        return star;
    }

    private static User user(Long id) {
        // User no-args ctor가 PROTECTED라 reflection으로 인스턴스화
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Scrum scrum(Long id) {
        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "id", id);
        return scrum;
    }
}
