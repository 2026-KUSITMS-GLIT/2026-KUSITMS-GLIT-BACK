package com.groute.groute_server.report.application.port.in.dto;

/**
 * RPT-001: 리포트 게이지 조회 뷰.
 *
 * <p>마지막 리포트 생성 이후 완료된 심화기록 수와 다음 생성 임계치를 담는다.
 */
public record ReportGaugeView(
        int currentCount, int nextThreshold, double progressRate, boolean isGeneratable) {

    public static ReportGaugeView of(int currentCount, int nextThreshold) {
        double progressRate = (double) currentCount / nextThreshold;
        boolean isGeneratable = currentCount >= nextThreshold;
        return new ReportGaugeView(currentCount, nextThreshold, progressRate, isGeneratable);
    }
}
