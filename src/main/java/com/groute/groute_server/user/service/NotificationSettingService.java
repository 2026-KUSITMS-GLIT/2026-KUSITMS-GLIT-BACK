package com.groute.groute_server.user.service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.dto.NotificationSettingItem;
import com.groute.groute_server.user.dto.NotificationSettingsResponse;
import com.groute.groute_server.user.dto.NotificationSettingsUpdateRequest;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 설정 조회/저장 서비스(MYP-004).
 *
 * <p>저장은 "전체 교체" 방식이다. 요청 본문 검증(중복 슬롯 거부) → 기존 슬롯 일괄 삭제 → 신규 슬롯 일괄 삽입 순서로 진행하며, 모두 동일 트랜잭션 안에서
 * 수행된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /** 내 알림 설정 조회. 응답 정렬·{@code isActive} 도출은 {@link NotificationSettingsResponse#from}이 담당. */
    public NotificationSettingsResponse getMySettings(Long userId) {
        List<NotificationSetting> entities =
                notificationSettingRepository.findAllByUser_IdOrderByDayOfWeekAscNotifyTimeAsc(
                        userId);
        return NotificationSettingsResponse.from(entities);
    }

    /**
     * 내 알림 설정 전체 교체.
     *
     * <p>1) 요청 내부 (요일+시간) 중복 검증. 2) 유저 존재 확인. 3) 기존 슬롯 bulk DELETE 후 {@code flush + clear}로 1차 캐시
     * 동기화 — 같은 트랜잭션 안에서 unique 제약이 있는 새 슬롯을 안전하게 INSERT하기 위함. 4) 신규 슬롯을 {@link
     * NotificationSetting#create}로 만들어 {@code saveAll}. 모든 슬롯의 {@code is_active}는 요청의 마스터 플래그를 그대로
     * 따른다(이슈 본문 "슬롯 유지" 정책에 따라 {@code isActive=false}여도 슬롯은 보존되며 발송 대상에서만 제외).
     */
    @Transactional
    public void replaceMySettings(Long userId, NotificationSettingsUpdateRequest request) {
        // 1. 요청 내부 (요일+시간) 중복 검증
        validateNoDuplicates(request.settings());

        // 2. 유저 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 기존 슬롯 bulk DELETE + 1차 캐시 동기화 (unique 제약 충돌 방지)
        notificationSettingRepository.deleteAllByUser_Id(userId);
        entityManager.flush();
        entityManager.clear();

        // 4. 신규 슬롯 일괄 삽입 — is_active는 요청의 마스터 플래그 그대로 마킹
        boolean active = Boolean.TRUE.equals(request.isActive());
        User userRef = userRepository.getReferenceById(userId);
        List<NotificationSetting> entities =
                request.settings().stream()
                        .map(
                                item ->
                                        NotificationSetting.create(
                                                userRef,
                                                item.dayOfWeek(),
                                                item.notifyTime(),
                                                active))
                        .toList();
        notificationSettingRepository.saveAll(entities);
    }

    private void validateNoDuplicates(List<NotificationSettingItem> items) {
        long distinct =
                items.stream()
                        .map(
                                item ->
                                        (Entry<?, ?>)
                                                new AbstractMap.SimpleEntry<>(
                                                        item.dayOfWeek(), item.notifyTime()))
                        .distinct()
                        .count();
        if (distinct != items.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_NOTIFICATION_SLOT);
        }
    }
}
