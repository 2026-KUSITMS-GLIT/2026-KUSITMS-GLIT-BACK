package com.groute.groute_server.record.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;

class ScrumTest {

    @Test
    @DisplayName("title 소유자가 다른 user이면 DOMAIN_OWNER_MISMATCH 예외가 발생한다")
    void should_throw_when_title_belongs_to_other_user() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", 1L);

        User other = User.createForSocialLogin();
        ReflectionTestUtils.setField(other, "id", 2L);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "user", other);

        assertThatThrownBy(() -> Scrum.create(owner, title, "내용", LocalDate.of(2026, 5, 31)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DOMAIN_OWNER_MISMATCH);
    }

    @Test
    @DisplayName("title 소유자와 user가 같으면 정상 생성된다")
    void should_create_when_owner_matches() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", 1L);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "user", owner);

        Scrum scrum = Scrum.create(owner, title, "내용", LocalDate.of(2026, 5, 31));

        assert scrum != null;
    }
}
