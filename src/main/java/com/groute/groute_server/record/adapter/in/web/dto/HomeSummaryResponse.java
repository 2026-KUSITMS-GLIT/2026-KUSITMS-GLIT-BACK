package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult;

public record HomeSummaryResponse(boolean isFirstStar, ReportModal reportModal) {

    public record ReportModal(boolean show, String type) {}

    public static HomeSummaryResponse from(HomeSummaryResult result) {
        return new HomeSummaryResponse(
                result.isFirstStar(),
                new ReportModal(result.reportModal().show(), result.reportModal().type()));
    }
}