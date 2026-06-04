package com.groute.groute_server.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.groute.groute_server.support.E2ETestBase;

import okhttp3.mockwebserver.MockResponse;

/**
 * 인증 플로우 E2E 테스트.
 *
 * <p>카카오 OAuth2 콜백 → JWT 발급 → 토큰 재발급(rotate) → 로그아웃 → 재발급 불가 순서로 검증한다.
 */
class AuthFlowE2ETest extends E2ETestBase {

    @Test
    void 카카오_OAuth2_콜백_JWT_발급_재발급_로그아웃() throws Exception {
        // 1. OAuth2 인가 시작 — noRedirectRest: redirect 비활성, 302 그대로 수신
        var initResp =
                noRedirectRest.getForEntity(url("/oauth2/authorization/kakao"), String.class);
        assertThat(initResp.getStatusCode().value()).isEqualTo(302);
        String kakaoRedirectUrl = initResp.getHeaders().getLocation().toString();
        String state = queryParam(kakaoRedirectUrl, "state");
        assertThat(state).isNotBlank();
        // Spring Security가 state를 HTTP session에 저장 → session cookie 추출
        String sessionCookie = sessionCookie(initResp.getHeaders().get("Set-Cookie"));
        assertThat(sessionCookie).isNotBlank();

        // 2. OAUTH_MOCK 응답 세팅 — ①token 교환 ②user-info
        OAUTH_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                """
                                {"access_token":"kakao-mock","token_type":"Bearer","expires_in":3600}
                                """));
        OAUTH_MOCK.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                """
                                {"id":99999,"kakao_account":{"email":"test@kakao.com"}}
                                """));

        // 3. 콜백 → JWT redirect (session cookie 수동 전달로 state 검증 통과)
        HttpHeaders cookieHeaders = new HttpHeaders();
        cookieHeaders.set("Cookie", sessionCookie);
        var callbackResp =
                noRedirectRest.exchange(
                        url("/login/oauth2/code/kakao?code=fake-code&state=" + state),
                        HttpMethod.GET,
                        new HttpEntity<>(null, cookieHeaders),
                        String.class);
        assertThat(callbackResp.getStatusCode().value()).isEqualTo(302);
        String callbackLocation = callbackResp.getHeaders().getLocation().toString();
        String accessToken = queryParam(callbackLocation, "access");
        String refreshToken = queryParam(callbackLocation, "refresh");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 4. 토큰 재발급 — refresh rotate
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        var reissueResp =
                restTemplate.exchange(
                        url("/api/auth/reissue"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"refreshToken\":\"" + refreshToken + "\"}", jsonHeaders),
                        String.class);
        assertThat(reissueResp.getStatusCode().is2xxSuccessful()).isTrue();
        String rotatedRefresh = jsonRead(reissueResp.getBody(), "$.data.refreshToken");
        assertThat(rotatedRefresh).isNotBlank();

        // 5. 로그아웃 — Redis에서 rotated refresh 삭제
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set("Authorization", "Bearer " + accessToken);
        var logoutResp =
                restTemplate.exchange(
                        url("/api/auth/logout"),
                        HttpMethod.POST,
                        new HttpEntity<>(null, authHeaders),
                        String.class);
        assertThat(logoutResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 6. 로그아웃 후 재발급 시도 → 401
        var afterLogoutResp =
                restTemplate.exchange(
                        url("/api/auth/reissue"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"refreshToken\":\"" + rotatedRefresh + "\"}", jsonHeaders),
                        String.class);
        assertThat(afterLogoutResp.getStatusCode().value()).isEqualTo(401);
    }

    private static String jsonRead(String json, String jsonPath) {
        return com.jayway.jsonpath.JsonPath.parse(json).read(jsonPath, String.class);
    }
}
