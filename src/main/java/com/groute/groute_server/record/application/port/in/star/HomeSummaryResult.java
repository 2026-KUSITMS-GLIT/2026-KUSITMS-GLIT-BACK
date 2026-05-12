package com.groute.groute_server.record.application.port.in.star;

/** 홈 복귀 시 요약 정보. */
public record HomeSummaryResult(boolean isFirstStar, ReportModal reportModal) {

    public record ReportModal(boolean show, String type) {

        public static ReportModal none() {
            return new ReportModal(false, null);
        }
    }
}