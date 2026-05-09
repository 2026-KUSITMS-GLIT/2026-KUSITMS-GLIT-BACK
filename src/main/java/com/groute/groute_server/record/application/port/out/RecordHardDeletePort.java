package com.groute.groute_server.record.application.port.out;

/**
 * record 도메인의 회원 탈퇴 hard delete 진입 port(MYP-005).
 *
 * <p>한 사용자가 record 도메인에 남긴 모든 row를 FK 의존 순서대로 물리 삭제한다. 복구 불가하며 호출자가 hardDeleteAt 도달 사용자만 전달할 책임을
 * 진다. 외부 스토리지 정리(StarImage 원본 파일)는 별도 storage port에서 선행되어야 하며, 본 port는 DB row만 책임진다.
 *
 * <p>구현체는 영속성 어댑터에 둔다. service는 본 인터페이스에만 의존해 record 내부 구조(엔티티/JPQL/JpaRepository)에 결합되지 않는다.
 */
public interface RecordHardDeletePort {

    /**
     * 사용자에 속한 record 도메인의 모든 row를 물리 삭제한다.
     *
     * <p>호출은 단일 트랜잭션 안에서 이루어져야 하며 (호출자 책임), 트랜잭션 밖에서 호출 시 부분 실패가 발생할 수 있다.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void hardDeleteAllByUserId(Long userId);
}
