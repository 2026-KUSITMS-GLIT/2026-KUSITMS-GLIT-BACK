package com.groute.groute_server.notification.scheduler;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 사이클의 DB 변경분을 단일 트랜잭션으로 반영한다.
 *
 * <p>스케줄러 본체는 FCM 호출과 분리되어 트랜잭션 없이 동작하고, 본 컴포넌트가 invalid 토큰 비활성화와 user 카피 인덱스 advance를 한 번의 트랜잭션으로
 * 처리한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatchWriter {

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * @param processedUserIds 카피 인덱스를 advance할 user id 집합
     * @param copyPoolSize 카피 풀 크기 (mod 정규화에 사용)
     * @param invalidTokens FCM 응답이 token-invalid로 돌아온 push token 목록
     */
    @Transactional
    public void apply(
            Set<Long> processedUserIds, int copyPoolSize, Collection<String> invalidTokens) {
        for (String token : invalidTokens) {
            deviceTokenRepository.deactivateByPushToken(token);
        }
        if (processedUserIds.isEmpty() || copyPoolSize <= 0) {
            return;
        }
        for (User user : userRepository.findAllById(processedUserIds)) {
            user.advanceCopyIndex(copyPoolSize);
        }
    }
}
