package com.groute.groute_server.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 내 정보 조회(MYP001) / 프로필 수정(MYP002) 서비스.
 *
 * <p>DTO ↔ Entity 변환은 컨트롤러·DTO 정적 팩토리가 담당하고, 서비스 시그니처에는 원시 타입·엔티티만 노출한다(Layered 규칙).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /** 내 프로필 조회 — 존재하지 않으면 {@link ErrorCode#USER_NOT_FOUND}. */
    public User getMyProfile(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 프로필 수정 — 한글 라벨을 enum으로 변환해 엔티티에 덮어쓴다.
     *
     * <p>라벨 파싱 실패는 {@link ErrorCode#INVALID_INPUT}으로 400 응답. enum에서 던지는 {@link
     * IllegalArgumentException}을 {@link BusinessException}으로 래핑해 일관된 에러 포맷을 유지한다.
     */
    @Transactional
    public User updateMyProfile(Long userId, String jobRoleLabel, String userStatusLabel) {
        JobRole jobRole = parseJobRole(jobRoleLabel);
        UserStatus userStatus = parseUserStatus(userStatusLabel);
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateProfile(jobRole, userStatus);
        return user;
    }

    private JobRole parseJobRole(String label) {
        try {
            return JobRole.fromLabel(label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
        }
    }

    private UserStatus parseUserStatus(String label) {
        try {
            return UserStatus.fromLabel(label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
        }
    }
}
