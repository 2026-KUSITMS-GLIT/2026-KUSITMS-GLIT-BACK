package com.groute.groute_server.record.adapter.out.storage;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarImageStoragePort;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link StarImageStoragePort}의 임시 stub 구현체.
 *
 * <p>외부 스토리지 백엔드(S3 등)가 아직 도입되지 않아 실제 파일 삭제는 수행하지 못한다. 호출은 받되 WARN 로그만 남기고 no-op으로 처리한다 — 회원 탈퇴
 * hard delete 배치가 동작은 하되 파일은 실제로 정리되지 않음을 운영자가 인지할 수 있도록 한다.
 *
 * <p>백엔드 도입 PR에서 본 adapter는 실제 S3 client 호출 구현체로 교체된다. service 코드 변경은 불필요(port 인터페이스 그대로).
 */
@Slf4j
@Component
class StarImageStorageStubAdapter implements StarImageStoragePort {

    @Override
    public void deleteAllByUserId(Long userId) {
        log.warn(
                "StarImage 외부 스토리지 정리 미구현 — 백엔드 도입 전까지 no-op (userId={}). 파일 원본은 수동 정리 필요.",
                userId);
    }
}
