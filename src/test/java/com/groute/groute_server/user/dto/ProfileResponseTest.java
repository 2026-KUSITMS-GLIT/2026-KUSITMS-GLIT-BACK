package com.groute.groute_server.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.groute.groute_server.user.entity.RecordStreakSnapshot;
import com.groute.groute_server.user.entity.User;

class ProfileResponseTest {

    private static final RecordStreakSnapshot STREAK = new RecordStreakSnapshot(0, false);

    @Test
    @DisplayName("joinedAt은 UTC createdAt을 KST 날짜로 변환한다")
    void joinedAt_convertsUtcToKstDate() {
        // UTC 2026-05-14 15:30 = KST 2026-05-15 00:30
        User user = userCreatedAt(OffsetDateTime.parse("2026-05-14T15:30:00Z"));

        ProfileResponse response = ProfileResponse.from(user, "img", STREAK);

        assertThat(response.joinedAt()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("joinedAt은 yyyy-MM-dd 문자열로 직렬화된다")
    void joinedAt_serializesAsYyyyMmDd() throws Exception {
        User user = userCreatedAt(OffsetDateTime.parse("2026-05-15T03:00:00Z"));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        JsonNode json =
                objectMapper.readTree(
                        objectMapper.writeValueAsString(ProfileResponse.from(user, "img", STREAK)));

        assertThat(json.get("joinedAt").asText()).isEqualTo("2026-05-15");
    }

    private static User userCreatedAt(OffsetDateTime createdAt) {
        User user = User.createForSocialLogin();
        ReflectionTestUtils.setField(user, "createdAt", createdAt);
        return user;
    }
}
