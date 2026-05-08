package com.groute.groute_server.user.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.config.UserProperties;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 내 정보 조회(MYP001) / 프로필 수정(MYP002) / 회원 탈퇴(MYP005) 서비스.
 *
 * <p>DTO ↔ Entity 변환은 컨트롤러·DTO 정적 팩토리가 담당하고, 서비스 시그니처에는 원시 타입·엔티티만 노출한다(Layered 규칙).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserProperties userProperties;
    private final Clock clock;

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

    /**
     * 회원 탈퇴 처리(MYP-005). soft delete + 30일(설정값) 후 물리 삭제 예약 + refresh token 즉시 무효화.
     *
     * <p>흐름:
     *
     * <ol>
     *   <li>사용자 조회 — 없으면 {@link ErrorCode#USER_NOT_FOUND}.
     *   <li>{@link User#scheduleHardDelete}로 soft delete + {@code hardDeleteAt} set. 이미 탈퇴한 사용자 재호출
     *       시 멱등 no-op이라 grace 기간이 연장되지 않는다.
     *   <li>refresh token Redis 키 삭제 — 보유 토큰으로 액세스 토큰 재발급 시도 시 401. 액세스 토큰 자체는 stateless라 만료까지 유효한
     *       점은 정책상 허용.
     * </ol>
     *
     * <p>refresh token 무효화는 멱등 케이스(이미 탈퇴된 사용자)에도 그대로 호출한다. Redis {@code DELETE}는 키 없을 때 noop이라
     * 무해하며, "탈퇴 처리됐지만 refresh token이 어딘가 살아있는" 엣지 케이스(예: 직전 호출이 token 삭제 직전에 실패)를 자동 복구하는 효과가 있다.
     *
     * @param userId 탈퇴할 사용자 ID
     */
    @Transactional
    public void deleteMyAccount(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.scheduleHardDelete(clock, Duration.ofDays(userProperties.hardDeleteGraceDays()));
        refreshTokenRepository.deleteByUserId(userId);
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
