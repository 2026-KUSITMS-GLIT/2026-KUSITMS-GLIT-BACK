package com.groute.groute_server.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.groute.groute_server.support.E2ETestBase;

/**
 * 온보딩 플로우 E2E 테스트.
 *
 * <p>신규 유저 로그인 → 온보딩 미완료 확인 → 온보딩 완료 → 프로필 검증 → 온보딩 완료 상태 재확인 순서로 검증한다.
 */
class OnboardingFlowE2ETest extends E2ETestBase {

    @Test
    void 온보딩_완료_및_프로필_조회() throws Exception {
        // 1. 카카오 OAuth2로 신규 유저 생성 → access token 발급
        String accessToken = loginAsNewKakaoUser(11111L);
        assertThat(accessToken).isNotBlank();

        // 2. 온보딩 미완료 상태 확인
        var statusResp =
                restTemplate.exchange(
                        url("/api/onboarding/status"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(accessToken)),
                        String.class);
        assertThat(statusResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(jsonRead(statusResp.getBody(), "$.data.isOnboardingCompleted", Boolean.class))
                .isFalse();

        // 3. 온보딩 완료
        var completeHeaders = jsonAuthHeaders(accessToken);
        completeHeaders.setContentType(MediaType.APPLICATION_JSON);
        var completeResp =
                restTemplate.exchange(
                        url("/api/onboarding/complete"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                """
                                {"nickname":"테스트유저","jobRole":"개발자","userStatus":"취업 준비 중"}
                                """,
                                completeHeaders),
                        String.class);
        assertThat(completeResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(jsonRead(completeResp.getBody(), "$.data.nickname", String.class))
                .isEqualTo("테스트유저");
        assertThat(jsonRead(completeResp.getBody(), "$.data.jobRole", String.class))
                .isEqualTo("개발자");
        assertThat(jsonRead(completeResp.getBody(), "$.data.userStatus", String.class))
                .isEqualTo("취업 준비 중");

        // 4. 온보딩 완료 상태 재확인
        var statusAfterResp =
                restTemplate.exchange(
                        url("/api/onboarding/status"),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders(accessToken)),
                        String.class);
        assertThat(
                        jsonRead(
                                statusAfterResp.getBody(),
                                "$.data.isOnboardingCompleted",
                                Boolean.class))
                .isTrue();

        // 5. 온보딩 중복 완료 시도 → 409
        var duplicateResp =
                restTemplate.exchange(
                        url("/api/onboarding/complete"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                """
                                {"nickname":"다른닉네임","jobRole":"개발자","userStatus":"재직 중"}
                                """,
                                completeHeaders),
                        String.class);
        assertThat(duplicateResp.getStatusCode().value()).isEqualTo(409);
    }
}
