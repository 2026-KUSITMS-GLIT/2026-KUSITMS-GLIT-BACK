package com.groute.groute_server.record.application.port.out.star;

/**
 * StarImage 외부 파일 스토리지 정리 port(MYP-005 hard delete 배치 선행 단계).
 *
 * <p>회원 탈퇴 시 DB의 StarImage row를 삭제하기 전에 호출되어, 해당 사용자가 업로드한 이미지 원본 파일을 외부 스토리지에서 정리한다. DB row가 먼저
 * 사라지면 어떤 파일이 누구 것인지 역추적할 수 없어 orphan 파일이 남는다.
 *
 * <p>현재 외부 스토리지 백엔드(S3 등)가 미구현 상태라 어댑터는 일시적으로 no-op이지만, 의존방향 정리와 호출 순서 고정을 위해 port를 먼저 신설한다. 백엔드 도입
 * 시 어댑터만 교체하면 service 코드 변경이 필요 없다.
 */
public interface StarImageStoragePort {

    /**
     * 사용자가 업로드한 모든 StarImage 원본 파일을 외부 스토리지에서 삭제한다.
     *
     * <p>DB의 StarImage row를 삭제하기 전에 호출되어야 한다. 호출 후 DB 정리는 별도 port에서 진행. 복구 불가.
     *
     * @param userId 탈퇴 처리할 사용자 ID
     */
    void deleteAllByUserId(Long userId);
}
