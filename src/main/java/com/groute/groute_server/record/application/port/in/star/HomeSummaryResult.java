package com.groute.groute_server.record.application.port.in.star;

import com.groute.groute_server.record.domain.enums.ReportModalType;

/** 홈 복귀 시 요약 정보. */
public record HomeSummaryResult(boolean isFirstStar, ReportModal reportModal) {

    public record ReportModal(boolean show, ReportModalType type) {

        public static ReportModal none() {
            return new ReportModal(false, null);
        }
    }
}
