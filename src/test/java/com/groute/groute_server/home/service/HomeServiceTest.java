package com.groute.groute_server.home.service;

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

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.home.repository.HomeRepository;
import com.groute.groute_server.home.service.HomeService.RadarResult;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final long USER_ID = 1L;

    @Mock private HomeRepository homeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private HomeService homeService;

    @Nested
    @DisplayName("역량 레이더 조회")
    class GetRadar {

        @Test
        @DisplayName("STAR 0건 — 전 카테고리 0, min=0, max=0")
        void allZero_whenNoCompletedStar() {
            given(homeRepository.countCompletedByCompetency(USER_ID, StarRecordStatus.TAGGED))
                    .willReturn(List.of());

            RadarResult result = homeService.getRadar(USER_ID);

            assertThat(result.min()).isZero();
            assertThat(result.max()).isZero();
            assertThat(result.categories()).hasSize(5);
            assertThat(result.categories()).containsValue(0);
        }

        @Test
        @DisplayName("일부 카테고리만 존재 — 나머지는 0으로 채워짐")
        void fillsZero_forMissingCategories() {
            given(homeRepository.countCompletedByCompetency(USER_ID, StarRecordStatus.TAGGED))
                    .willReturn(
                            List.of(
                                    new Object[] {CompetencyCategory.COLLABORATION, 7L},
                                    new Object[] {CompetencyCategory.PROBLEM_SOLVING, 2L}));

            RadarResult result = homeService.getRadar(USER_ID);

            assertThat(result.categories().get(CompetencyCategory.COLLABORATION)).isEqualTo(7);
            assertThat(result.categories().get(CompetencyCategory.PROBLEM_SOLVING)).isEqualTo(2);
            assertThat(result.categories().get(CompetencyCategory.DISCOVERY_ANALYSIS)).isZero();
            assertThat(result.min()).isZero();
            assertThat(result.max()).isEqualTo(7);
        }

        @Test
        @DisplayName("전 카테고리 존재 — min/max 정확히 계산")
        void calcMinMax_whenAllCategoriesPresent() {
            given(homeRepository.countCompletedByCompetency(USER_ID, StarRecordStatus.TAGGED))
                    .willReturn(
                            List.of(
                                    new Object[] {CompetencyCategory.DISCOVERY_ANALYSIS, 5L},
                                    new Object[] {CompetencyCategory.PLANNING_EXECUTION, 3L},
                                    new Object[] {CompetencyCategory.COLLABORATION, 7L},
                                    new Object[] {CompetencyCategory.PROBLEM_SOLVING, 2L},
                                    new Object[] {CompetencyCategory.REFLECTION_GROWTH, 4L}));

            RadarResult result = homeService.getRadar(USER_ID);

            assertThat(result.min()).isEqualTo(2);
            assertThat(result.max()).isEqualTo(7);
            assertThat(result.categories()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("브랜딩 문구 조회")
    class GetBrandingTitle {

        @Test
        @DisplayName("브랜딩 문구 존재 — 반환")
        void returnsBrandingTitle_whenSet() {
            User user = User.createForSocialLogin();
            org.springframework.test.util.ReflectionTestUtils.setField(
                    user, "brandingTitle", "백엔드 개발자");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            String result = homeService.getBrandingTitle(USER_ID);

            assertThat(result).isEqualTo("백엔드 개발자");
        }

        @Test
        @DisplayName("신규 사용자 — null 반환")
        void returnsNull_whenNewUser() {
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            String result = homeService.getBrandingTitle(USER_ID);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("사용자 없음 — USER_NOT_FOUND 예외")
        void throwsException_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeService.getBrandingTitle(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
