package com.groute.groute_server.record.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult;
import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult.ReportModal;
import com.groute.groute_server.record.application.port.in.star.HomeSummaryUseCase;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.domain.enums.ReportModalType;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * STAR 기록 후 홈 복귀 요약 서비스 (GET /api/star-records/home-summary).
 *
 * <p>User 엔티티의 pending 플래그(코치마크·리포트 모달)를 소비하여 반환한다. 플래그는 AI 태깅 완료 시점에 설정되므로, 홈을 거치지 않고 연속으로 STAR를
 * 작성해도 마일스톤 알림이 누락되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class HomeSummaryService implements HomeSummaryUseCase {

    private final UserPort userPort;

    @Override
    @Transactional
    public HomeSummaryResult getSummary(Long userId) {
        User user = userPort.findById(userId);
        boolean isFirstStar = user.consumeCoachMark();
        String modalType = user.consumeReportModal();
        ReportModal reportModal;
        try {
            reportModal =
                    modalType != null
                            ? new ReportModal(true, ReportModalType.valueOf(modalType))
                            : ReportModal.none();
        } catch (IllegalArgumentException e) {
            reportModal = ReportModal.none();
        }
        return new HomeSummaryResult(isFirstStar, reportModal);
    }
}
