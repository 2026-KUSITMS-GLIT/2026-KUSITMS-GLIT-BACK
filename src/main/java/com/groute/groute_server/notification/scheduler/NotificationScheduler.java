package com.groute.groute_server.notification.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.entity.DeviceToken;
import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.common.notification.copy.NotificationCopy;
import com.groute.groute_server.common.notification.copy.NotificationCopyProperties;
import com.groute.groute_server.common.notification.fcm.client.FcmPushClient;
import com.groute.groute_server.common.notification.fcm.model.FcmPayload;
import com.groute.groute_server.common.notification.fcm.model.SendResult;
import com.groute.groute_server.record.application.service.ScrumDailyQueryService;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.DayOfWeek;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 발송 스케줄러(MYP-004).
 *
 * <p>매시 0/30분(KST) 트리거. 매칭 활성 슬롯 → 작성자 제외 → user별 카피 라운드로빈 발송 → invalid 토큰 비활성화.
 *
 * <p>FCM 호출이 트랜잭션 안에서 일어나 DB 커넥션이 잠시 점유된다. MVP 볼륨(수백 건 이하)에서는 허용 가능하며, 트래픽이 커지면 send와 deactivate를
 * 분리해 트랜잭션을 좁히는 리팩토링이 필요하다.
 *
 * <p>단일 인스턴스 가정. 다중 인스턴스 배포 시 동일 시각에 모든 인스턴스가 동시에 발사돼 N배 발송이 발생하므로 ShedLock 등 분산 락이 필요하다(별도 이슈).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NICKNAME_TOKEN = "{닉네임}";

    private final NotificationSettingRepository notificationSettingRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final FcmPushClient fcmPushClient;
    private final NotificationCopy notificationCopy;
    private final NotificationCopyProperties notificationCopyProperties;
    private final ScrumDailyQueryService scrumDailyQueryService;
    private final Clock clock;

    /**
     * 30분 단위 발송 사이클. KST 매시 0분/30분에 트리거된다.
     *
     * <p>흐름: 1) 현재 (요일, 시각, 날짜) 추출 2) 매칭 활성 슬롯 → user 모음 3) 카피 풀 비어 있으면 스킵 4) 당일 작성자 제외 5) 남은 user
     * 엔티티/토큰 일괄 조회 6) user별 카피 1개 선택 + {닉네임} 치환 + 토큰별 발송 7) 발송 후 카피 인덱스 advance 8) 사이클 결과 INFO 로그.
     */
    @Scheduled(cron = "0 0,30 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void dispatch() {
        // 1. KST 현재 (요일, 시각, 날짜) 추출 — DB의 notify_time은 분 단위라 초·나노 truncate
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        DayOfWeek dow = toDomainDay(now.getDayOfWeek());
        LocalTime time = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = now.toLocalDate();

        // 2. 매칭 활성 슬롯 조회 → 후보 user_id 모음
        List<NotificationSetting> slots =
                notificationSettingRepository.findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(
                        dow, time);
        if (slots.isEmpty()) {
            log.debug("발송 대상 슬롯 없음 (dow={}, time={})", dow, time);
            return;
        }
        Set<Long> candidateUserIds =
                slots.stream()
                        .map(s -> s.getUser().getId())
                        .collect(Collectors.toCollection(HashSet::new));

        // 3. 카피 풀 비어 있으면 발송 스킵
        List<NotificationCopy.Item> copies = notificationCopy.copies();
        if (copies.isEmpty()) {
            log.warn("알림 카피 풀이 비어 있음 — 발송 스킵 (slots={})", slots.size());
            return;
        }

        // 4. 당일 스크럼 작성자 제외 (기획 D-1)
        Set<Long> writers = scrumDailyQueryService.findUserIdsWithScrumOn(today, candidateUserIds);
        candidateUserIds.removeAll(writers);
        if (candidateUserIds.isEmpty()) {
            log.info("발송 대상 모두 당일 작성 — 스킵 (slots={}, writers={})", slots.size(), writers.size());
            return;
        }

        // 5. 남은 user 엔티티 + 활성 토큰 일괄 조회
        Map<Long, User> usersById =
                userRepository.findAllById(candidateUserIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, List<DeviceToken>> tokensByUserId =
                deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(candidateUserIds).stream()
                        .collect(Collectors.groupingBy(t -> t.getUser().getId()));

        // 6. user별 발송 + 인덱스 advance
        String link = notificationCopyProperties.deepLinkUrl();
        int sent = 0;
        int deactivated = 0;
        int processed = 0;
        for (Long userId : candidateUserIds) {
            User user = usersById.get(userId);
            List<DeviceToken> userTokens = tokensByUserId.get(userId);
            if (user == null || userTokens == null || userTokens.isEmpty()) {
                continue;
            }

            // 카피 선택 + 닉네임 치환 (mod로 인덱스 정규화 — 풀 크기 축소 케이스 대비)
            int idx = Math.floorMod(user.getNotificationCopyIndex(), copies.size());
            NotificationCopy.Item template = copies.get(idx);
            FcmPayload payload =
                    new FcmPayload(
                            replaceNickname(template.title(), user.getNickname()),
                            replaceNickname(template.body(), user.getNickname()),
                            link);

            // 토큰별 발송 + invalid 토큰 비활성화
            for (DeviceToken token : userTokens) {
                SendResult result = fcmPushClient.send(token.getPushToken(), payload);
                if (result.success()) {
                    sent++;
                } else if (result.tokenInvalid()) {
                    deviceTokenRepository.deactivateByPushToken(token.getPushToken());
                    deactivated++;
                }
            }

            // 7. 발송 후 카피 인덱스 advance (트랜잭션 dirty checking으로 UPDATE 발행)
            user.advanceCopyIndex(copies.size());
            processed++;
        }

        // 8. 사이클 결과 로그
        log.info(
                "알림 발송 사이클 종료 (dow={}, time={}, candidates={}, writers={}, processed={}, sent={}, deactivated={})",
                dow,
                time,
                slots.size(),
                writers.size(),
                processed,
                sent,
                deactivated);
    }

    /**
     * Java 표준 {@link java.time.DayOfWeek}를 도메인 enum으로 변환한다.
     *
     * <p>도메인 enum의 상수명({@code MON, TUE, ...})은 표준 이름의 첫 3글자와 일치하므로 {@code substring(0,3)}으로 매핑.
     */
    private DayOfWeek toDomainDay(java.time.DayOfWeek d) {
        return DayOfWeek.valueOf(d.name().substring(0, 3));
    }

    /** 카피 템플릿의 {@code 닉네임} 토큰을 user.nickname으로 치환. nickname이 null이면 빈 문자열로 대체. */
    private String replaceNickname(String template, String nickname) {
        if (template == null) {
            return null;
        }
        return template.replace(NICKNAME_TOKEN, nickname == null ? "" : nickname);
    }
}
