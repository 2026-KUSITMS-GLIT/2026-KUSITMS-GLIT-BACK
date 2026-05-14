package com.groute.groute_server.user.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.groute.groute_server.user.service.AccountHardDeleteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 hard delete 스케줄러(MYP-005).
 *
 * <p>매일 KST 새벽 4시에 트리거되어, grace 기간이 만료된 사용자들에 대해 외부 스토리지·Redis·DB 모든 흔적을 물리 삭제한다. 실제 삭제는 {@link
 * AccountHardDeleteService}에 위임하며, 본 클래스는 사용자 목록을 받아 한 명씩 호출하는 오케스트레이션과 실패 격리만 담당한다.
 *
 * <p>한 사용자의 처리 실패가 다른 사용자 처리를 막지 않도록 사용자별 try-catch로 격리한다. 실패한 사용자는 {@code hardDeleteAt}이 그대로 유지되므로
 * 다음 사이클에서 자동으로 재대상이 된다 (storage·Redis·DB 단계 모두 멱등 보장).
 *
 * <p>실행 시각이 새벽 4시인 이유: 사용자 트래픽이 가장 적은 시간대 + KST 자정 직후의 cron timing 어색함을 회피.
 *
 * <p>단일 인스턴스 가정. 다중 인스턴스 배포 시 동일 시각에 모든 인스턴스가 동시 발사되어 같은 사용자에 대해 중복 처리가 발생할 수 있어 ShedLock 등 분산 락이
 * 필요하다(별도 이슈).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountHardDeleteScheduler {

    private final AccountHardDeleteService accountHardDeleteService;

    /**
     * 일일 hard delete 사이클. KST 매일 04:00에 트리거된다.
     *
     * <p>흐름: 1) 만료 사용자 목록 조회 2) 사용자별 try-catch로 hardDelete 호출 3) 사이클 결과 INFO 로그.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void dispatch() {
        List<Long> targetUserIds = accountHardDeleteService.findExpiredUserIds();
        if (targetUserIds.isEmpty()) {
            log.debug("hard delete 대상 사용자 없음");
            return;
        }

        int succeeded = 0;
        int failed = 0;
        for (Long userId : targetUserIds) {
            try {
                accountHardDeleteService.hardDelete(userId);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.error("회원 hard delete 실패 (userId={}) — 다음 사이클에 재시도됨", userId, e);
            }
        }

        log.info(
                "회원 hard delete 사이클 종료 (targets={}, succeeded={}, failed={})",
                targetUserIds.size(),
                succeeded,
                failed);
    }
}
