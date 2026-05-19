package com.groute.groute_server.user.repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.enums.DayOfWeek;

/**
 * 유저 알림 설정 슬롯 저장소(MYP-004).
 *
 * <p>조회/삭제는 user_id 기준, 스케줄러 발송 매칭은 (day_of_week, notify_time, is_active) 복합 인덱스를 활용한다.
 */
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 특정 유저의 모든 알림 슬롯 조회. {@code day_of_week}는 ENUM STRING 컬럼이라 ORDER BY 결과가 요일 자연 순서(MON→SUN)가 아닌
     * 알파벳순(FRI/MON/SAT/SUN/THU/TUE/WED)으로 나온다. 응답 가공 단계에서 enum 자연 순서로 재정렬해야 한다.
     */
    List<NotificationSetting> findAllByUser_IdOrderByDayOfWeekAscNotifyTimeAsc(Long userId);

    /**
     * 특정 유저의 모든 알림 슬롯 일괄 삭제. PATCH "전체 교체" 패턴에서 기존 슬롯 일소용.
     *
     * <p>벌크 DELETE이라 영속성 컨텍스트 1차 캐시는 동기화되지 않는다. 동일 트랜잭션 내에서 즉시 재조회·재삽입 시 {@code
     * EntityManager.flush()}/{@code clear()} 또는 별도 트랜잭션 분리 고려.
     */
    @Modifying
    @Query("DELETE FROM NotificationSetting n WHERE n.user.id = :userId")
    void deleteAllByUser_Id(@Param("userId") Long userId);

    /** 스케줄러 발송 대상 조회. 현재 KST의 (요일, 시각)에 매칭되며 활성 상태인 슬롯. */
    List<NotificationSetting> findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(
            DayOfWeek dayOfWeek, LocalTime notifyTime);

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 진입.
     *
     * <p>{@link #deleteAllByUser_Id(Long)}와 동일 쿼리지만, 호출 의도(PATCH 전체 교체 vs 회원 탈퇴 hard delete)와 로깅
     * 시그니처(int 반환)를 분리하기 위해 별도 메서드로 노출한다.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM NotificationSetting n WHERE n.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
