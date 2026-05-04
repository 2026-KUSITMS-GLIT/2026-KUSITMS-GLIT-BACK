package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
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
import com.groute.groute_server.record.application.port.in.star.GetStarDetailQuery;
import com.groute.groute_server.record.application.port.in.star.StarDetailView;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.StarTag;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class StarRecordQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long STAR_ID = 10L;

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock StarTagQueryPort starTagQueryPort;
    @Mock StarImageQueryPort starImageQueryPort;

    @InjectMocks StarRecordQueryService service;

    @Nested
    @DisplayName("정상 조회")
    class HappyPath {

        @Test
        @DisplayName("이미지·태그가 모두 있을 때 flat schema로 매핑한다")
        void should_returnFullView_when_tagsAndImagesExist() {
            // given
            StarRecord star = star(STAR_ID, USER_ID, "ST", "A", "R");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));
            given(starTagQueryPort.findAllByStarRecordId(STAR_ID))
                    .willReturn(
                            List.of(
                                    tag(CompetencyCategory.PLANNING_EXECUTION, "UX 설계"),
                                    tag(CompetencyCategory.PLANNING_EXECUTION, "품질 관리")));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(
                            List.of(
                                    image(100L, "https://a", (short) 0),
                                    image(101L, "https://b", (short) 1)));

            // when
            StarDetailView view = service.getStarDetail(new GetStarDetailQuery(USER_ID, STAR_ID));

            // then
            assertThat(view.starRecordId()).isEqualTo(STAR_ID);
            assertThat(view.projectTag()).isEqualTo("밋업 프로젝트");
            assertThat(view.freeText()).isEqualTo("기획 작업");
            assertThat(view.primaryCategory()).isEqualTo("PLANNING_EXECUTION");
            assertThat(view.detailTags()).containsExactly("UX 설계", "품질 관리");
            assertThat(view.situationTask()).isEqualTo("ST");
            assertThat(view.action()).isEqualTo("A");
            assertThat(view.result()).isEqualTo("R");
            assertThat(view.images())
                    .extracting(
                            StarDetailView.ImageView::imageId, StarDetailView.ImageView::sortOrder)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(100L, 0),
                            org.assertj.core.groups.Tuple.tuple(101L, 1));
        }
    }

    @Nested
    @DisplayName("빈 컬렉션 처리")
    class EmptyCollections {

        @Test
        @DisplayName("이미지가 없으면 빈 배열을 반환한다")
        void should_returnEmptyImages_when_noImages() {
            // given
            StarRecord star = star(STAR_ID, USER_ID, "ST", "A", "R");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));
            given(starTagQueryPort.findAllByStarRecordId(STAR_ID))
                    .willReturn(List.of(tag(CompetencyCategory.COLLABORATION, "조율")));
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());

            // when
            StarDetailView view = service.getStarDetail(new GetStarDetailQuery(USER_ID, STAR_ID));

            // then
            assertThat(view.images()).isEmpty();
        }

        @Test
        @DisplayName("태그가 없으면 detailTags 빈 배열·primaryCategory null을 반환한다")
        void should_returnEmptyTagsAndNullPrimary_when_noTags() {
            // given
            StarRecord star = star(STAR_ID, USER_ID, "ST", "A", "R");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));
            given(starTagQueryPort.findAllByStarRecordId(STAR_ID)).willReturn(List.of());
            given(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(STAR_ID))
                    .willReturn(List.of());

            // when
            StarDetailView view = service.getStarDetail(new GetStarDetailQuery(USER_ID, STAR_ID));

            // then
            assertThat(view.detailTags()).isEmpty();
            assertThat(view.primaryCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("미존재 또는 이미 삭제된 STAR면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_missing() {
            // given
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () -> service.getStarDetail(new GetStarDetailQuery(USER_ID, STAR_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
        }

        @Test
        @DisplayName("타 유저의 STAR면 STAR_FORBIDDEN을 던진다")
        void should_throwStarForbidden_when_otherUser() {
            // given — STAR는 OTHER_USER_ID 소유, 요청은 USER_ID
            StarRecord star = star(STAR_ID, OTHER_USER_ID, "ST", "A", "R");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(star));

            // when & then
            assertThatThrownBy(
                            () -> service.getStarDetail(new GetStarDetailQuery(USER_ID, STAR_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
        }
    }

    // ============== helpers ==============

    private static StarRecord star(
            Long id, Long ownerUserId, String situationTask, String action, String result) {
        StarRecord star = new StarRecord();
        ReflectionTestUtils.setField(star, "id", id);
        ReflectionTestUtils.setField(star, "user", user(ownerUserId));
        ReflectionTestUtils.setField(star, "scrum", scrum(50L));
        ReflectionTestUtils.setField(star, "situationTask", situationTask);
        ReflectionTestUtils.setField(star, "action", action);
        ReflectionTestUtils.setField(star, "result", result);
        return star;
    }

    private static User user(Long id) {
        // User no-args ctor가 PROTECTED라 reflection으로 인스턴스화
        try {
            java.lang.reflect.Constructor<User> ctor = User.class.getDeclaredConstructor();
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
        ReflectionTestUtils.setField(scrum, "title", title());
        return scrum;
    }

    private static ScrumTitle title() {
        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", 1L);
        ReflectionTestUtils.setField(project, "name", "밋업 프로젝트");
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 5L);
        ReflectionTestUtils.setField(title, "project", project);
        ReflectionTestUtils.setField(title, "freeText", "기획 작업");
        return title;
    }

    private static StarTag tag(CompetencyCategory primary, String detail) {
        StarTag tag = new StarTag();
        ReflectionTestUtils.setField(tag, "primaryCategory", primary);
        ReflectionTestUtils.setField(tag, "detailTag", detail);
        return tag;
    }

    private static StarImage image(Long id, String url, short sortOrder) {
        StarImage image = new StarImage();
        ReflectionTestUtils.setField(image, "id", id);
        ReflectionTestUtils.setField(image, "imageUrl", url);
        ReflectionTestUtils.setField(image, "sortOrder", sortOrder);
        return image;
    }
}
