package com.groute.groute_server.user.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.SoftDeleteEntity;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 사용자.
 *
 * <p>소셜 로그인으로 가입하며 닉네임/직군/현재 상태를 보유한다. 회원 탈퇴 시 {@link #hardDeleteAt}을 설정해두고, 배치 잡이 해당 시각 이후 모든 연관
 * 데이터를 물리 삭제한다(MYP005). 논리 삭제 플래그(is_deleted/deleted_at)는 부모 {@link SoftDeleteEntity}에서 상속.
 *
 * <p>소셜 로그인 시점(ONB002)에는 JWT 발급을 위해 row만 먼저 생성되고, 닉네임·직군·상태는 온보딩 완료 이슈에서 채워진다. 따라서 세 컬럼은 모두
 * nullable이며, 온보딩 완료 여부는 {@code nickname IS NOT NULL}로 판정한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 닉네임. 2~12자, 한글/영문/숫자만 허용(ONB003). 온보딩 완료 전 NULL. 중복 검사는 수행하지 않는다. */
    @Column(name = "nickname", length = 12)
    private String nickname;

    /** 직군. 온보딩 시 1회 선택(ONB004), 마이페이지에서 변경 가능(MYP002). AI 태깅 컨텍스트로 사용. 온보딩 완료 전 NULL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_role")
    private JobRole jobRole;

    /** 현재 상태(재학/취준/재직). 온보딩 시 선택(ONB005), 마이페이지에서 자유롭게 변경 가능(MYP002). 온보딩 완료 전 NULL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus;

    /**
     * 리포트에서 생성된 커리어 브랜딩 문장.
     *
     * <p>리포트 미생성 시 NULL → 기본 카피("하루를 기록하고, 나는 어떤 사람인지 확인해보세요") 노출(HOM001). 리포트 발행 시 자동 교체된다.
     */
    @Column(name = "branding_title", length = 100)
    private String brandingTitle;

    /** 마지막 로그인 시각. 활성 유저 분석/이탈 알림 집계에 사용. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * 물리 삭제 예약 시각.
     *
     * <p>회원 탈퇴 요청 시 set. 배치 잡이 이 시각 이후 모든 연관 데이터를 물리 삭제(MYP005). 복구 불가. 개인정보보호법 대응 목적.
     */
    @Column(name = "hard_delete_at")
    private OffsetDateTime hardDeleteAt;

    /** 소셜 로그인 신규 가입 시 호출. 온보딩 전이므로 프로필 필드는 모두 NULL로 둔다. */
    public static User createForSocialLogin() {
        return new User();
    }

    /** 로그인 성공 시 마지막 로그인 시각 갱신. */
    public void recordLogin() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    /**
     * 마이페이지 프로필 수정(MYP002). 직군·상태를 덮어쓴다.
     *
     * <p>요청 바디가 항상 두 필드를 모두 포함한다는 전제(부분 수정 아님). null 허용 여부는 호출부 검증 책임이다.
     */
    public void updateProfile(JobRole jobRole, UserStatus userStatus) {
        this.jobRole = jobRole;
        this.userStatus = userStatus;
    }
}
