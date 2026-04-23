package com.groute.groute_server.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.groute.groute_server.auth.enums.SocialProvider;
import com.groute.groute_server.user.entity.User;

class SocialAccountTest {

    // ── 1. create: 모든 필드가 올바르게 설정 ─────────────────────────
    @Test
    void create_setsAllFieldsCorrectly() {
        User user = mock(User.class);

        SocialAccount account =
                SocialAccount.create(user, SocialProvider.KAKAO, "kakao-uid-123", "user@kakao.com");

        assertThat(account.getUser()).isSameAs(user);
        assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(account.getProviderUid()).isEqualTo("kakao-uid-123");
        assertThat(account.getEmail()).isEqualTo("user@kakao.com");
    }

    // ── 2. create: email이 null이어도 생성 가능 ───────────────────────
    @Test
    void create_withNullEmail_isAllowed() {
        User user = mock(User.class);

        SocialAccount account = SocialAccount.create(user, SocialProvider.NAVER, "naver-uid", null);

        assertThat(account.getEmail()).isNull();
    }

    // ── 3. create: 구글 프로바이더 설정 확인 ──────────────────────────
    @Test
    void create_googleProvider_setsProviderCorrectly() {
        User user = mock(User.class);

        SocialAccount account =
                SocialAccount.create(user, SocialProvider.GOOGLE, "google-sub-456", "g@gmail.com");

        assertThat(account.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(account.getProviderUid()).isEqualTo("google-sub-456");
    }

    // ── 4. updateEmail: 다른 이메일로 업데이트 ───────────────────────
    @Test
    void updateEmail_newEmail_updatesEmail() {
        User user = mock(User.class);
        SocialAccount account =
                SocialAccount.create(user, SocialProvider.KAKAO, "uid-1", "old@kakao.com");

        account.updateEmail("new@kakao.com");

        assertThat(account.getEmail()).isEqualTo("new@kakao.com");
    }

    // ── 5. updateEmail: null이면 no-op ────────────────────────────────
    @Test
    void updateEmail_null_noOp() {
        User user = mock(User.class);
        SocialAccount account =
                SocialAccount.create(user, SocialProvider.KAKAO, "uid-2", "original@kakao.com");

        account.updateEmail(null);

        assertThat(account.getEmail()).isEqualTo("original@kakao.com");
    }

    // ── 6. updateEmail: 동일 이메일이면 no-op ─────────────────────────
    @Test
    void updateEmail_sameEmail_noOp() {
        User user = mock(User.class);
        SocialAccount account =
                SocialAccount.create(user, SocialProvider.KAKAO, "uid-3", "same@kakao.com");

        account.updateEmail("same@kakao.com");

        assertThat(account.getEmail()).isEqualTo("same@kakao.com");
    }

    // ── 7. updateEmail: 기존 email이 null인 경우 새 값으로 업데이트 ───
    @Test
    void updateEmail_fromNullToNewEmail_updates() {
        User user = mock(User.class);
        SocialAccount account = SocialAccount.create(user, SocialProvider.NAVER, "uid-4", null);

        account.updateEmail("new@naver.com");

        assertThat(account.getEmail()).isEqualTo("new@naver.com");
    }

    // ── 8. 경계값: updateEmail을 여러 번 호출해도 최신 값 유지 ─────────
    @Test
    void updateEmail_calledMultipleTimes_keepsLatestValue() {
        User user = mock(User.class);
        SocialAccount account =
                SocialAccount.create(user, SocialProvider.GOOGLE, "uid-5", "first@gmail.com");

        account.updateEmail("second@gmail.com");
        account.updateEmail("third@gmail.com");

        assertThat(account.getEmail()).isEqualTo("third@gmail.com");
    }

    // ── 9. create: 네이버 프로바이더 설정 확인 ────────────────────────
    @Test
    void create_naverProvider_setsProviderCorrectly() {
        User user = mock(User.class);

        SocialAccount account =
                SocialAccount.create(user, SocialProvider.NAVER, "naver-uid-789", "n@naver.com");

        assertThat(account.getProvider()).isEqualTo(SocialProvider.NAVER);
    }
}