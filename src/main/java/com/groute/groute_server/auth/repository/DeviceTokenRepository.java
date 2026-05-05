package com.groute.groute_server.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.auth.entity.DeviceToken;

/**
 * 디바이스 푸시 토큰 저장소.
 *
 * <p>등록(upsert)은 {@code push_token} unique 인덱스를 활용해 단건 조회 후 갱신/삽입을 분기한다. 발송 대상 조회는 {@code
 * idx_device_tokens_user_active} 복합 인덱스를 활용한다(MYP-004 알림 발송 파이프라인).
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /** push_token unique 제약을 활용한 단건 조회. 등록 API의 upsert 분기 판정에 사용. */
    Optional<DeviceToken> findByPushToken(String pushToken);

    /** 스케줄러 발송 대상 조회. 매칭된 user_id 묶음에 속한 활성 토큰만 반환. */
    List<DeviceToken> findAllByUser_IdInAndIsActiveTrue(Collection<Long> userIds);

    /**
     * FCM 발송 실패(UNREGISTERED/INVALID_ARGUMENT) 토큰 비활성화.
     *
     * <p>벌크 UPDATE이라 영속성 컨텍스트 1차 캐시는 동기화되지 않는다 — 동일 트랜잭션 내에서 즉시 재조회 시 주의.
     */
    @Modifying
    @Query("UPDATE DeviceToken t SET t.isActive = false WHERE t.pushToken = :pushToken")
    void deactivateByPushToken(@Param("pushToken") String pushToken);
}
