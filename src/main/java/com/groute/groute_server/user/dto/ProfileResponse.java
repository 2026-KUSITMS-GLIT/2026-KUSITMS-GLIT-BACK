package com.groute.groute_server.user.dto;

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
        @Schema(description = "유저 상태", example = "재학 중") String userStatus) {

    /**
     * 엔티티와 기본 프로필 이미지 URL로부터 응답 DTO를 생성하는 정적 팩토리.
     *
     * <p>enum → 한글 라벨 변환은 이 팩토리에서 수행한다. 온보딩 미완료 상태의 유저는 {@code jobRole}/{@code userStatus}가 DB에서
     * {@code null}일 수 있으므로 방어적으로 null을 허용한다.
     */
    public static ProfileResponse from(User user, String profileImageUrl) {
        String jobRoleLabel = user.getJobRole() != null ? user.getJobRole().getLabel() : null;
        String userStatusLabel =
                user.getUserStatus() != null ? user.getUserStatus().getLabel() : null;
        return new ProfileResponse(
                profileImageUrl, user.getNickname(), jobRoleLabel, userStatusLabel);
    }
}
