package com.groute.groute_server.record.application.service;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/** AI 태깅 완료 누적 카운트에 따라 사용자의 코치마크·리포트 모달 노출 예약을 갱신한다. */
@Component
@RequiredArgsConstructor
public class UserMilestoneTracker {

    private static final long COACH_MARK_MILESTONE = 1L;
    private static final long MINI_REPORT_MILESTONE = 10L;
    private static final long FULL_REPORT_BASE = 20L;
    private static final long FULL_REPORT_PERIOD = 10L;

    private final StarRecordRepositoryPort starRecordPort;
    private final UserPort userPort;

    public void track(Long userId) {
        long taggedCount = starRecordPort.countTaggedByUserId(userId);
        User user = userPort.findById(userId);
        if (taggedCount == COACH_MARK_MILESTONE) {
            user.markPendingCoachMark();
        }
        if (taggedCount == MINI_REPORT_MILESTONE) {
            user.markPendingReportModal("MINI");
        } else if (taggedCount >= FULL_REPORT_BASE && taggedCount % FULL_REPORT_PERIOD == 0) {
            user.markPendingReportModal("FULL");
        }
    }
}
