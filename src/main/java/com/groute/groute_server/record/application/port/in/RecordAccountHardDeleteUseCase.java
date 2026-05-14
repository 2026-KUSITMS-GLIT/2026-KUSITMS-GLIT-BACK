package com.groute.groute_server.record.application.port.in;

/**
 * record 도메인의 회원 탈퇴 hard delete 진입점(MYP-005).
 *
 * <p>다른 도메인(특히 user)이 회원 탈퇴 cascade 처리 시 record 내부 구조(JpaRepository, storage adapter 등)에 결합되지 않도록
 * 외부 진입점을 use case로 노출한다. 호출자는 본 인터페이스만 의존해 record 내부 port/out 변경에 영향받지 않는다.
 *
 * <p>외부 스토리지 정리와 DB row 정리는 트랜잭션 경계가 다르므로 별도 메서드로 분리되어 있다 (호출자가 트랜잭션 외부에서 storage 호출 후 트랜잭션 안에서 DB
 * 호출을 결정).
 */
public interface RecordAccountHardDeleteUseCase {

    /**
     * 사용자가 업로드한 StarImage 원본 파일을 외부 스토리지에서 삭제한다.
     *
     * <p>DB row 삭제 전에 호출되어야 하며, DB 트랜잭션 밖에서 호출하는 것을 권장한다 (외부 I/O 지연이 DB 커넥션을 점유하지 않도록).
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void purgeExternalStorage(Long userId);

    /**
     * record 도메인의 모든 DB row를 FK 의존 순서대로 물리 삭제한다.
     *
     * <p>호출자(user 도메인)가 동일 트랜잭션 안에서 다른 도메인 cleanup을 함께 호출하므로, 본 메서드는 자체 트랜잭션 경계를 두지 않는다.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void purgeDb(Long userId);
}
