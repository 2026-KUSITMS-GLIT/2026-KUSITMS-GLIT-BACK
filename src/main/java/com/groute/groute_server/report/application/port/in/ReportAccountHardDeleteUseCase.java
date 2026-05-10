package com.groute.groute_server.report.application.port.in;

/**
 * report 도메인의 회원 탈퇴 hard delete 진입점(MYP-005).
 *
 * <p>다른 도메인(특히 user)이 회원 탈퇴 cascade 처리 시 report 내부 port/out에 결합되지 않도록 외부 진입점을 use case로 노출한다. 호출자는
 * 본 인터페이스만 의존해 report 내부 변경에 영향받지 않는다.
 *
 * <p>Report는 외부 스토리지·자식 테이블이 없는 leaf 도메인이라 DB row 정리 단일 메서드만 노출한다.
 */
public interface ReportAccountHardDeleteUseCase {

    /**
     * 사용자가 발행한 모든 Report 행을 물리 삭제한다.
     *
     * <p>호출자(user 도메인)가 동일 트랜잭션 안에서 다른 도메인 cleanup을 함께 호출하므로, 본 메서드는 자체 트랜잭션 경계를 두지 않는다.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void purgeDb(Long userId);
}
