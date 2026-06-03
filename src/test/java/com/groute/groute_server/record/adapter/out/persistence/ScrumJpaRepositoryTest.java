package com.groute.groute_server.record.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.support.DataJpaTestBase;
import com.groute.groute_server.user.entity.User;

class ScrumJpaRepositoryTest extends DataJpaTestBase {

    @Autowired ScrumJpaRepository scrumJpaRepository;
    @Autowired TestEntityManager em;

    private User user;
    private ScrumTitle title;

    private static final LocalDate DATE = LocalDate.of(2026, 6, 1);

    @BeforeEach
    void setUpEntities() {
        user = em.persistAndFlush(User.createForSocialLogin());
        Project project = em.persistAndFlush(Project.builder().user(user).name("프로젝트").build());
        title =
                em.persistAndFlush(
                        ScrumTitle.builder().user(user).project(project).freeText("제목").build());
        em.clear();
    }

    @Nested
    @DisplayName("JOIN FETCH 쿼리")
    class JoinFetchQueries {

        @Test
        @DisplayName("일자 일치 스크럼만 반환, title·project 접근 가능")
        void should_returnOnlyDateMatchedScrums_when_findAllByUserIdAndScrumDate() {
            // given
            em.persistAndFlush(Scrum.create(user, title, "내용A", DATE));
            em.persistAndFlush(Scrum.create(user, title, "내용B", DATE));
            em.persistAndFlush(Scrum.create(user, title, "다른날", DATE.minusDays(1)));
            em.clear();

            // when
            List<Scrum> result = scrumJpaRepository.findAllByUserIdAndScrumDate(user.getId(), DATE);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(s -> assertThat(s.getScrumDate()).isEqualTo(DATE));
            // JOIN FETCH로 로드된 title·project에 접근 가능
            assertThat(result.get(0).getTitle().getFreeText()).isEqualTo("제목");
            assertThat(result.get(0).getTitle().getProject().getName()).isEqualTo("프로젝트");
        }

        @Test
        @DisplayName("soft-deleted 스크럼은 결과에서 제외")
        void should_excludeSoftDeleted_when_findAllByUserIdAndScrumDate() {
            // given
            Scrum active = em.persistAndFlush(Scrum.create(user, title, "활성", DATE));
            Scrum deleted = em.persistAndFlush(Scrum.create(user, title, "삭제됨", DATE));
            deleted.softDelete();
            em.flush();
            em.clear();

            // when
            List<Scrum> result = scrumJpaRepository.findAllByUserIdAndScrumDate(user.getId(), DATE);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(active.getId());
        }

        @Test
        @DisplayName("요청 ID 중 본인 소유만 반환")
        void should_returnOnlyOwned_when_findAllByIdInAndUserId() {
            // given
            Scrum mine1 = em.persistAndFlush(Scrum.create(user, title, "내꺼1", DATE));
            Scrum mine2 = em.persistAndFlush(Scrum.create(user, title, "내꺼2", DATE));

            User other = em.persistAndFlush(User.createForSocialLogin());
            Scrum othersScrum = em.persistAndFlush(Scrum.create(other, title, "남꺼", DATE));
            em.clear();

            // when
            List<Scrum> result =
                    scrumJpaRepository.findAllByIdInAndUserId(
                            List.of(mine1.getId(), mine2.getId(), othersScrum.getId()),
                            user.getId());

            // then
            assertThat(result)
                    .hasSize(2)
                    .extracting(Scrum::getId)
                    .containsExactlyInAnyOrder(mine1.getId(), mine2.getId());
        }

        @Test
        @DisplayName("soft-deleted 스크럼은 findAllByIdInAndUserId 결과에서 제외")
        void should_excludeSoftDeleted_when_findAllByIdInAndUserId() {
            // given
            Scrum active = em.persistAndFlush(Scrum.create(user, title, "활성", DATE));
            Scrum deleted = em.persistAndFlush(Scrum.create(user, title, "삭제됨", DATE));
            deleted.softDelete();
            em.flush();
            em.clear();

            // when
            List<Scrum> result =
                    scrumJpaRepository.findAllByIdInAndUserId(
                            List.of(active.getId(), deleted.getId()), user.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(active.getId());
        }
    }

    @Nested
    @DisplayName("@Modifying 벌크 쿼리")
    class ModifyingQueries {

        @Test
        @DisplayName("softDeleteAllByIdIn — 대상만 is_deleted=true, deletedAt 세팅")
        void should_softDeleteTargets_when_softDeleteAllByIdIn() {
            // given
            Scrum target = em.persistAndFlush(Scrum.create(user, title, "삭제 대상", DATE));
            Scrum keep = em.persistAndFlush(Scrum.create(user, title, "유지", DATE));
            em.clear();

            // when
            int affected = scrumJpaRepository.softDeleteAllByIdIn(List.of(target.getId()));
            em.clear();

            // then
            assertThat(affected).isEqualTo(1);

            Scrum deletedFound = em.find(Scrum.class, target.getId());
            assertThat(deletedFound.isDeleted()).isTrue();
            assertThat(deletedFound.getDeletedAt()).isNotNull();

            Scrum keptFound = em.find(Scrum.class, keep.getId());
            assertThat(keptFound.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("updateContent — 본문 변경 후 반환값 1")
        void should_updateContent_when_contentIsValid() {
            // given
            Scrum scrum = em.persistAndFlush(Scrum.create(user, title, "원본 내용", DATE));
            em.clear();

            // when
            int affected = scrumJpaRepository.updateContent(scrum.getId(), "수정된 내용");
            em.clear();

            // then
            assertThat(affected).isEqualTo(1);
            Scrum found = em.find(Scrum.class, scrum.getId());
            assertThat(found.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("hardDeleteAllByUserId — soft-delete 무관 해당 유저 전체 물리 삭제")
        void should_physicallyDeleteAll_when_hardDeleteAllByUserId() {
            // given
            em.persistAndFlush(Scrum.create(user, title, "삭제1", DATE));
            Scrum alsoDeleted = em.persistAndFlush(Scrum.create(user, title, "논리삭제 후 물리삭제", DATE));
            alsoDeleted.softDelete();
            em.flush();

            User other = em.persistAndFlush(User.createForSocialLogin());
            Scrum othersScrum = em.persistAndFlush(Scrum.create(other, title, "다른유저", DATE));
            em.clear();

            // when
            int affected = scrumJpaRepository.hardDeleteAllByUserId(user.getId());
            em.clear();

            // then
            assertThat(affected).isEqualTo(2);
            List<Scrum> remaining = scrumJpaRepository.findAll();
            assertThat(remaining)
                    .hasSize(1)
                    .extracting(Scrum::getId)
                    .containsExactly(othersScrum.getId());
        }
    }
}
