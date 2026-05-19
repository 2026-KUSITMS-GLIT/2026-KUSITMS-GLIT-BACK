package com.groute.groute_server.auth.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.auth.entity.DeviceToken;

/**
 * 디바이스 푸시 토큰 저장소.
 *
 * <p>등록은 PostgreSQL native {@code INSERT ... ON CONFLICT} 한 문장으로 atomic upsert 처리해 동시 등록 race 시
 * unique 제약 위반을 원천 차단한다. 발송 대상 조회는 {@code idx_device_tokens_user_active} 복합 인덱스를 활용한다(MYP-004 알림 발송
 * 파이프라인).
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * push_token unique 인덱스 기반 atomic upsert(MYP-004 race 방지).
     *
     * <p>같은 push_token으로 동시 요청이 들어와도 한 row로 수렴한다 — INSERT 충돌 시 user_id/platform/is_active를 갱신. JPA
     * 영속성 컨텍스트와 audit listener는 우회되므로 {@code updated_at}은 명시적으로 갱신한다. {@code created_at}은 INSERT 시
     * 컬럼 DEFAULT(now())로 채워진다.
     */
    @Modifying
    @Query(
            value =
                    "INSERT INTO device_tokens (user_id, platform, push_token, is_active) "
                            + "VALUES (:userId, :platform, :pushToken, true) "
                            + "ON CONFLICT (push_token) DO UPDATE SET "
                            + "    user_id = EXCLUDED.user_id, "
                            + "    platform = EXCLUDED.platform, "
                            + "    is_active = TRUE, "
                            + "    updated_at = now()",
            nativeQuery = true)
    void upsertByPushToken(
            @Param("userId") Long userId,
            @Param("platform") String platform,
            @Param("pushToken") String pushToken);

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

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 진입. 해당 사용자의 모든 디바이스 토큰 row 물리 삭제.
     *
     * <p>발송 비활성(is_active=false)과 무관하게 모두 삭제한다. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM DeviceToken t WHERE t.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
