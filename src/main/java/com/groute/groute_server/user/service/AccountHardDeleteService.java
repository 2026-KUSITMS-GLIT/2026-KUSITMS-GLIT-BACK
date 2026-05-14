package com.groute.groute_server.user.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.record.application.port.in.RecordAccountHardDeleteUseCase;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 hard delete 오케스트레이터(MYP-005).
 *
 * <p>{@link com.groute.groute_server.user.entity.User#hardDeleteAt}이 도달한 사용자에 대해 외부 스토리지 → Redis →
 * DB 순서로 모든 흔적을 물리 삭제한다. 복구 불가하며, 호출자(스케줄러)가 hardDeleteAt 도달 사용자만 전달할 책임을 진다.
 *
 * <p>처리 단계와 트랜잭션 경계는 다음과 같이 분리한다 (DB 커넥션 점유 최소화):
 *
 * <ol>
 *   <li><b>외부 스토리지 정리</b> (트랜잭션 밖): S3 등 외부 호출이라 네트워크 I/O가 길 수 있어 DB 커넥션을 잡고 있으면 안 된다.
 *   <li><b>Redis refresh token 정리</b> (트랜잭션 밖): DB와 분리된 별도 클라이언트. 멱등 DELETE라 부분 실패도 무해.
 *   <li><b>DB 도메인 cascade 삭제</b> ({@link AccountHardDeleteDbCleaner}에 위임 — REQUIRES_NEW 트랜잭션):
 *       record → report → user/auth → users 순서로 FK 위반을 피한다.
 * </ol>
 *
 * <p>Storage·Redis를 DB 트랜잭션 앞에 두는 이유: 어느 단계에서 실패해도 다음 배치 사이클이 user row가 DB에 남아있는 한 재시도하므로 결과적 일관성이
 * 보장된다. DB delete가 먼저 일어나면 storage·Redis에 고아 데이터가 영구 잔존한다.
 *
 * <p>DB cascade는 {@link AccountHardDeleteDbCleaner}로 분리해 별도 빈으로 둔다 — 같은 클래스 안의 self-invocation은
 * Spring AOP proxy를 거치지 않아 {@code @Transactional}이 적용되지 않기 때문.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountHardDeleteService {

    private final UserRepository userRepository;
    private final RecordAccountHardDeleteUseCase recordAccountHardDelete;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountHardDeleteDbCleaner dbCleaner;
    private final Clock clock;

    /**
     * grace 기간이 만료된 hard delete 대상 사용자 ID 조회.
     *
     * <p>스케줄러가 본 결과를 받아 사용자별로 {@link #hardDelete(Long)}를 호출한다. 한 명 실패가 다른 사용자 처리를 막지 않도록 try-catch는
     * 호출자(스케줄러) 책임.
     *
     * @return hard delete 대상 사용자 ID 목록 (없으면 빈 리스트)
     */
    public List<Long> findExpiredUserIds() {
        return userRepository.findExpiredHardDeleteUserIds(OffsetDateTime.now(clock));
    }

    /**
     * 단일 사용자에 대한 hard delete 실행. 복구 불가.
     *
     * <p>호출자는 {@code hardDeleteAt}이 실제로 도달한 사용자만 전달할 책임을 진다 (본 메서드는 사용자 상태 검증을 하지 않음). 호출 순서: 외부
     * 스토리지 → Redis → DB cascade.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    public void hardDelete(Long userId) {
        recordAccountHardDelete.purgeExternalStorage(userId);
        refreshTokenRepository.deleteByUserId(userId);
        dbCleaner.cascadeDelete(userId);
        log.info("회원 hard delete 완료 (userId={})", userId);
    }
}
