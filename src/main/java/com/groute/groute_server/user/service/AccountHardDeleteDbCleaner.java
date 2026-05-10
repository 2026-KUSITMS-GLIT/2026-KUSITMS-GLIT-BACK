package com.groute.groute_server.user.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.auth.repository.SocialAccountRepository;
import com.groute.groute_server.auth.repository.UserTermAgreementRepository;
import com.groute.groute_server.record.application.port.in.RecordAccountHardDeleteUseCase;
import com.groute.groute_server.report.application.port.in.ReportAccountHardDeleteUseCase;
import com.groute.groute_server.user.repository.CoachmarkHistoryRepository;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 hard delete 배치(MYP-005)의 DB cascade 전담 컴포넌트.
 *
 * <p>{@link AccountHardDeleteService}가 외부 스토리지·Redis 정리를 트랜잭션 밖에서 처리한 뒤 본 메서드를 호출한다. DB 도메인
 * cascade만 짧은 REQUIRES_NEW 트랜잭션 안에서 처리해 DB 커넥션 점유를 최소화한다.
 *
 * <p>오케스트레이터에서 본 메서드를 직접 호출해도 다른 빈을 거치므로 Spring AOP proxy가 적용되어 {@code @Transactional}이 정상 동작한다. 같은
 * 클래스 내부 self-invocation 문제 회피 목적의 분리.
 */
@Component
@RequiredArgsConstructor
class AccountHardDeleteDbCleaner {

    private final RecordAccountHardDeleteUseCase recordAccountHardDelete;
    private final ReportAccountHardDeleteUseCase reportAccountHardDelete;
    private final NotificationSettingRepository notificationSettingRepository;
    private final CoachmarkHistoryRepository coachmarkHistoryRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;
    private final UserRepository userRepository;

    /**
     * 단일 사용자의 모든 DB 도메인 데이터를 FK 의존 순서대로 cascade 삭제한다.
     *
     * <p>호출 순서: record → report → user/auth Layered repos → users row. 마지막 users row 삭제 전까지 자식 테이블이
     * 모두 정리되어야 FK 위반 없이 끝난다.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cascadeDelete(Long userId) {
        recordAccountHardDelete.purgeDb(userId);
        reportAccountHardDelete.purgeDb(userId);
        notificationSettingRepository.hardDeleteAllByUserId(userId);
        coachmarkHistoryRepository.hardDeleteAllByUserId(userId);
        deviceTokenRepository.hardDeleteAllByUserId(userId);
        socialAccountRepository.hardDeleteAllByUserId(userId);
        userTermAgreementRepository.hardDeleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
