package com.groute.groute_server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.entity.DeviceToken;
import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 디바이스 푸시 토큰 등록/갱신 서비스(MYP-004).
 *
 * <p>같은 {@code push_token}은 unique 제약상 한 row만 존재한다. 클라가 같은 토큰을 다시 등록하면 소유자·플랫폼·활성 플래그를 갱신해 공용
 * 디바이스/계정 전환/{@code onTokenRefresh} 재호출 케이스를 모두 흡수한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * 디바이스 토큰 등록(upsert).
     *
     * <p>존재 시: 소유자/플랫폼 갱신 + 활성화. 미존재 시: 신규 insert. 어느 경우든 호출 후엔 {@code (userId, pushToken,
     * isActive=true)} 상태가 보장된다.
     */
    @Transactional
    public void register(Long userId, DevicePlatform platform, String pushToken) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        deviceTokenRepository
                .findByPushToken(pushToken)
                .ifPresentOrElse(
                        existing -> {
                            existing.changeOwner(user);
                            existing.updatePlatform(platform);
                            existing.activate();
                        },
                        () ->
                                deviceTokenRepository.save(
                                        DeviceToken.register(user, platform, pushToken)));
    }
}
