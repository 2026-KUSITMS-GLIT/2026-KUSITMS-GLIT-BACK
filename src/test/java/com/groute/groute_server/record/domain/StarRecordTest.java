package com.groute.groute_server.record.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;

class StarRecordTest {

    @Test
    @DisplayName("scrum 소유자가 다른 user이면 DOMAIN_OWNER_MISMATCH 예외가 발생한다")
    void should_throw_when_scrum_belongs_to_other_user() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", 1L);

        User other = User.createForSocialLogin();
        ReflectionTestUtils.setField(other, "id", 2L);

        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "user", other);

        assertThatThrownBy(() -> StarRecord.create(owner, scrum))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DOMAIN_OWNER_MISMATCH);
    }

    @Test
    @DisplayName("scrum 소유자와 user가 같으면 정상 생성된다")
    void should_create_when_owner_matches() {
        User owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", 1L);

        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "user", owner);

        StarRecord record = StarRecord.create(owner, scrum);

        assertThat(record).isNotNull();
    }

    @Test
    @DisplayName("revertComplete 호출 시 isCompleted가 false로 변경된다")
    void should_setIsCompletedFalse_when_revertComplete() {
        StarRecord record = new StarRecord();
        ReflectionTestUtils.setField(record, "isCompleted", true);

        record.revertComplete();

        assertThat((Boolean) ReflectionTestUtils.getField(record, "isCompleted")).isFalse();
    }
}
