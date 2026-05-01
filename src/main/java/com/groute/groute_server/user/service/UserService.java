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

    /** 온보딩 완료 여부 — {@code nickname}이 NULL이면 미완료. */
    public boolean isOnboardingCompleted(Long userId) {
        return userRepository
                .findById(userId)
                .map(user -> user.getNickname() != null)
                .orElse(false);
    }

    /** 내 프로필 조회 — 존재하지 않으면 {@link ErrorCode#USER_NOT_FOUND}. */
    public User getMyProfile(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 프로필 수정 — 한글 라벨을 enum으로 변환해 엔티티에 덮어쓴다.
     *
     * <p>라벨 파싱 실패는 필드별로 구분된 400 응답을 반환한다 — 직군은 {@link ErrorCode#INVALID_JOB_ROLE}, 상태는 {@link
     * ErrorCode#INVALID_USER_STATUS}. enum에서 던지는 {@link IllegalArgumentException}을 {@link
     * BusinessException}으로 래핑해 일관된 에러 포맷을 유지한다.
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

    /**
     * 온보딩 일괄 완료 — 닉네임·직군·상태를 한 번에 저장한다.
     *
     * <p>{@code nickname IS NULL} 조건부 단건 UPDATE로 원자적으로 처리해 동시 요청이 모두 성공하는 레이스를 방지한다. 0 rows 반환 시 유저
     * 존재 여부로 {@link ErrorCode#USER_NOT_FOUND}와 {@link ErrorCode#ONBOARDING_ALREADY_COMPLETED}를
     * 구분한다.
     */
    @Transactional
    public User completeOnboarding(
            Long userId, String nickname, String jobRoleLabel, String userStatusLabel) {
        JobRole jobRole = parseJobRole(jobRoleLabel);
        UserStatus userStatus = parseUserStatus(userStatusLabel);
        int updated =
                userRepository.completeOnboardingIfNotDone(userId, nickname, jobRole, userStatus);
        if (updated == 0) {
            if (!userRepository.existsById(userId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private JobRole parseJobRole(String label) {
        try {
            return JobRole.fromLabel(label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_JOB_ROLE, e.getMessage());
        }
    }

    private UserStatus parseUserStatus(String label) {
        try {
            return UserStatus.fromLabel(label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS, e.getMessage());
        }
    }
}
