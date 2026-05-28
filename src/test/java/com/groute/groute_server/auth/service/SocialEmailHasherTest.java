package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SocialEmailHasherTest {

    private static final String PEPPER = "test-pepper-must-be-at-least-32-bytes-for-hmac-sha256";

    private final SocialEmailHasher hasher =
            new SocialEmailHasher(new SocialEmailHashProperties(PEPPER));

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("성공 — 같은 입력은 항상 같은 64자 hex 해시로 매핑된다")
        void returnsDeterministicHexDigest_forSameInput() {
            // given
            String email = "user@example.com";

            // when
            String first = hasher.hash(email);
            String second = hasher.hash(email);

            // then
            assertThat(first).isEqualTo(second);
            assertThat(first).hasSize(64).matches("^[0-9a-f]+$");
        }

        @Test
        @DisplayName("성공 — 다른 입력은 다른 해시값으로 매핑된다")
        void returnsDifferentDigest_forDifferentInput() {
            // given
            String a = "user@example.com";
            String b = "other@example.com";

            // when
            String hashA = hasher.hash(a);
            String hashB = hasher.hash(b);

            // then
            assertThat(hashA).isNotEqualTo(hashB);
        }

        @Test
        @DisplayName("성공 — pepper가 다르면 같은 이메일도 다른 해시값으로 매핑된다")
        void returnsDifferentDigest_whenPepperDiffers() {
            // given
            SocialEmailHasher other =
                    new SocialEmailHasher(
                            new SocialEmailHashProperties(
                                    "another-pepper-also-must-be-at-least-32-bytes"));

            // when
            String hashA = hasher.hash("user@example.com");
            String hashB = other.hash("user@example.com");

            // then
            assertThat(hashA).isNotEqualTo(hashB);
        }
    }

    @Nested
    @DisplayName("Edges")
    class Edges {

        @Test
        @DisplayName("성공 — null 입력은 null 반환")
        void returnsNull_whenInputIsNull() {
            assertThat(hasher.hash(null)).isNull();
        }

        @Test
        @DisplayName("성공 — 빈 문자열 입력은 null 반환")
        void returnsNull_whenInputIsEmpty() {
            assertThat(hasher.hash("")).isNull();
        }
    }
}
