package com.groute.groute_server.user.service;

import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.DayOfWeek;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 설정 조회/저장 서비스(MYP-004).
 *
 * <p>저장은 "전체 교체" 방식이다. 요일 중복·활성화 정합성 검증 → 기존 슬롯 일괄 삭제 → 신규 슬롯 일괄 삽입 순서로 진행하며, 모두 동일 트랜잭션 안에서 수행된다.
 * 한 유저의 모든 슬롯은 동일한 {@code notifyTime}을 갖는다(기획 E).
 *
 * <p>서비스 시그니처는 도메인 원시값/엔티티만 노출한다 — DTO ↔ 도메인 변환은 컨트롤러 레이어 책임.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /** 내 알림 설정 슬롯 조회. 정렬·DTO 변환은 호출 측이 담당. */
    public List<NotificationSetting> getMySettings(Long userId) {
        return notificationSettingRepository.findAllByUser_IdOrderByDayOfWeekAscNotifyTimeAsc(
                userId);
    }

    /**
     * 내 알림 설정 전체 교체.
     *
     * <p>요일 N개를 받아 동일한 {@code notifyTime}으로 row N개를 만든다. 빈 {@code daysOfWeek}는 {@code
     * isActive=false}와 함께일 때만 허용되며 이 경우 모든 슬롯이 삭제된다.
     */
    @Transactional
    public void replaceMySettings(
            Long userId, boolean isActive, List<DayOfWeek> daysOfWeek, LocalTime notifyTime) {
        // 1. 요일 중복 검증
        validateNoDuplicateDays(daysOfWeek);

        // 2. 빈 days + isActive=true 거부 (의미적 모순)
        if (daysOfWeek.isEmpty() && isActive) {
            throw new BusinessException(ErrorCode.NOTIFICATION_DAYS_REQUIRED);
        }

        // 3. 유저 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 4. 기존 슬롯 bulk DELETE + 1차 캐시 동기화 (unique 제약 충돌 방지)
        notificationSettingRepository.deleteAllByUser_Id(userId);
        entityManager.flush();
        entityManager.clear();

        // 5. 빈 days면 모든 슬롯 삭제만 하고 종료
        if (daysOfWeek.isEmpty()) {
            return;
        }

        // 6. 신규 슬롯 일괄 삽입 — 모든 row에 동일한 notifyTime, is_active
        User userRef = userRepository.getReferenceById(userId);
        List<NotificationSetting> entities =
                daysOfWeek.stream()
                        .map(day -> NotificationSetting.create(userRef, day, notifyTime, isActive))
                        .toList();
        notificationSettingRepository.saveAll(entities);
    }

    /**
     * 요일 배열의 중복 여부 검증. 동일 요일이 두 번 이상 들어오면 {@link ErrorCode#DUPLICATE_NOTIFICATION_SLOT}로 거부한다(동일
     * 요일은 한 번만 선택 가능).
     */
    private void validateNoDuplicateDays(List<DayOfWeek> days) {
        long distinct = days.stream().distinct().count();
        if (distinct != days.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_NOTIFICATION_SLOT);
        }
    }
}
