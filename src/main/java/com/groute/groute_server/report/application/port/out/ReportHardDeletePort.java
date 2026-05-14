package com.groute.groute_server.report.application.port.out;

/**
 * report 도메인의 회원 탈퇴 hard delete 진입 port(MYP-005).
 *
 * <p>한 사용자가 발행한 모든 리포트 row를 물리 삭제한다. 복구 불가하며 호출자가 hardDeleteAt 도달 사용자만 전달할 책임을 진다.
 *
 * <p>Report는 다른 도메인에서 참조하지 않는 leaf 테이블이라 호출 순서 제약 없음. service는 본 인터페이스에만 의존해 report 내부 구조에 결합되지
 * 않는다.
 */
public interface ReportHardDeletePort {

    /**
     * 사용자가 발행한 모든 리포트를 물리 삭제한다.
     *
     * <p>호출은 단일 트랜잭션 안에서 이루어져야 하며 (호출자 책임), 트랜잭션 밖에서 호출 시 부분 실패가 발생할 수 있다.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void hardDeleteAllByUserId(Long userId);
}
