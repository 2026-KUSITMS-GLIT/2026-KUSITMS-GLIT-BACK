package com.groute.groute_server.user.entity;

import java.time.LocalTime;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.BaseTimeEntity;
import com.groute.groute_server.user.enums.DayOfWeek;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 알림 설정(MYP003).
 *
 * <p>요일 + 시간 조합으로 여러 슬롯을 등록할 수 있다. 시간대는 07:00~자정(00:00) 범위 내 30분 단위로 제한(DB CHECK로 강제). 스케줄러는
 * (day_of_week, notify_time) 기준으로 발송 대상 유저를 역조회한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification_settings")
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 요일. 월~일 체크박스 다중 선택 가능(MYP003). */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** 알림 시각. 07:00~자정(00:00) 사이 30분 단위만 허용(MYP003). 범위·그리드 제약은 DB CHECK가 강제. */
    @Column(name = "notify_time", nullable = false)
    private LocalTime notifyTime;

    /** 활성 여부. OS 알림 권한 비활성 시 false 처리(권한 필요 안내 노출, MYP003). */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * 신규 알림 슬롯 생성용 정적 팩토리. PATCH "전체 교체" 흐름에서 기존 슬롯을 일괄 삭제 후 본 팩토리로 재생성한다. {@code active}는 요청 본문의
     * {@code isActive} 플래그가 그대로 전달돼 모든 슬롯이 동일한 활성 여부를 갖는다(이슈 본문 "슬롯 유지" 정책).
     */
    public static NotificationSetting create(
            User user, DayOfWeek dayOfWeek, LocalTime notifyTime, boolean active) {
        NotificationSetting setting = new NotificationSetting();
        setting.user = user;
        setting.dayOfWeek = dayOfWeek;
        setting.notifyTime = notifyTime;
        setting.isActive = active;
        return setting;
    }
}
