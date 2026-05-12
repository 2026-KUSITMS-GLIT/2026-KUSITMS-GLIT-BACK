package com.groute.groute_server.record.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult;
import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult.ReportModal;
import com.groute.groute_server.record.application.port.in.star.HomeSummaryUseCase;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.enums.ReportModalType;

import lombok.RequiredArgsConstructor;

/**
 * STAR 기록 후 홈 복귀 요약 서비스 (GET /api/star-records/home-summary).
 *
 * <p>tagged STAR 개수 기준으로 코치마크(isFirstStar)와 리포트 모달(reportModal) 노출 여부를 판단한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeSummaryService implements HomeSummaryUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;

    @Override
    public HomeSummaryResult getSummary(Long userId) {
        long count = starRecordRepositoryPort.countTaggedByUserId(userId);
        return new HomeSummaryResult(count == 1, resolveModal(count));
    }

    private static ReportModal resolveModal(long count) {
        if (count == 10) {
            return new ReportModal(true, ReportModalType.MINI);
        }
        if (count >= 20 && count % 10 == 0) {
            return new ReportModal(true, ReportModalType.FULL);
        }
        return ReportModal.none();
    }
}
