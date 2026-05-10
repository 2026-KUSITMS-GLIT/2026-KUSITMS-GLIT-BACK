package com.groute.groute_server.user.dto;

import com.groute.groute_server.user.entity.RecordStreakSnapshot;
import com.groute.groute_server.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

/** 마이페이지 내 정보 조회(MYP001) / 프로필 수정(MYP002) 공용 응답 DTO. */
public record ProfileResponse(
        @Schema(
                        description = "캐릭터 프로필 이미지 URL (미등록 시 모든 유저 공통 기본 이미지)",
                        example = "https://cdn.groute.app/static/default-character.png")
                String profileImage,
        @Schema(description = "유저 닉네임", example = "겨레") String nickname,
        @Schema(description = "유저 직군", example = "개발자") String jobRole,
        @Schema(description = "유저 상태", example = "재학 중") String userStatus,
        @Schema(
                        description = "연속 기록 일수 — 오늘 또는 어제 기록 시 유지, 2일 이상 미기록·기록 0건은 0(REC-001).",
                        example = "8")
                int consecutiveRecordDays,
        @Schema(
                        description =
                                "째려보는 캐릭터 노출 여부. 마지막 기록일이 3일 이상 이전이면 true, 그 외/0건은 false(REC-001).",
                        example = "false")
                boolean glaring) {

    /**
     * 엔티티 + 기본 프로필 이미지 URL + streak 산정 결과로부터 응답 DTO를 생성하는 정적 팩토리.
     *
     * <p>enum → 한글 라벨 변환은 이 팩토리에서 수행한다. 온보딩 미완료 상태의 유저는 {@code jobRole}/{@code userStatus}가 DB에서
     * {@code null}일 수 있으므로 방어적으로 null을 허용한다.
     */
    public static ProfileResponse from(
            User user, String profileImageUrl, RecordStreakSnapshot streak) {
        String jobRoleLabel = user.getJobRole() != null ? user.getJobRole().getLabel() : null;
        String userStatusLabel =
                user.getUserStatus() != null ? user.getUserStatus().getLabel() : null;
        return new ProfileResponse(
                profileImageUrl,
                user.getNickname(),
                jobRoleLabel,
                userStatusLabel,
                streak.consecutiveDays(),
                streak.glaring());
    }
}
