package com.groute.groute_server.user.entity;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

    /**
     * 알림 카피 라운드로빈 인덱스(MYP-004).
     *
     * <p>스케줄러가 발송 시 카피 풀에서 이 인덱스 위치의 카피를 사용하고, 발송 후 {@link #advanceCopyIndex(int)}로 갱신한다. 가입 시 0부터
     * 시작해 A→B→C→A 순으로 순환한다(기획 A).
     */
    @Column(name = "notification_copy_index", nullable = false)
    private short notificationCopyIndex = 0;

    /**
     * 끊기지 않은 KST 일자 기준 연속 기록 일수(REC-001).
     *
     * <p>가입 시 0. 첫 기록 시 1로 시작, {@code gap == 1} 갱신 시 +1, {@code gap >= 2} 갱신 시 1로 리셋. 갱신 책임은 {@link
     * #recordOnDate(LocalDate)}, 산정 결과 노출은 {@link #streakSnapshotAsOf(LocalDate)}.
     */
    @Column(name = "current_streak", nullable = false)
    private short currentStreak = 0;

    /**
     * KST 기준 마지막 기록 일자(REC-001).
     *
     * <p>가입 직후 NULL — {@link #streakSnapshotAsOf(LocalDate)}는 NULL 입력 시 streak/glaring 모두 false(기본
     * 캐릭터)로 산정한다.
     */
    @Column(name = "last_record_date")
    private LocalDate lastRecordDate;

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
     * <p>요청 바디가 항상 두 필드를 모두 포함한다는 전제(부분 수정 아님). 엔티티 invariant 유지를 위해 호출부와 관계없이 null을 거부한다
     */
    public void updateProfile(JobRole jobRole, UserStatus userStatus) {
        this.jobRole = Objects.requireNonNull(jobRole, "jobRole");
        this.userStatus = Objects.requireNonNull(userStatus, "userStatus");
    }

    /**
     * 회원 탈퇴 처리(MYP-005). 즉시 soft delete하고, 일정 기간 뒤 물리 삭제될 시각을 {@link #hardDeleteAt}에 예약한다. 실제 물리
     * 삭제는 후속 배치 잡이 처리한다.
     *
     * <p>이미 탈퇴한 사용자가 다시 호출해도 {@code hardDeleteAt}을 덮어쓰지 않는다. 두 번째 호출이 시각을 갱신해버리면 grace 기간이 사실상 연장되어
     * 복구 윈도우가 늦춰지기 때문 — 첫 요청 시각으로 못 박는 게 사용자 입장에서 안전하다.
     *
     * <p>{@code clock}은 호출하는 서비스가 주입한다. 테스트에서 "지금"을 고정해 {@code hardDeleteAt = 기대값}을 검증할 수 있게 하기 위함.
     * 부모 클래스의 {@code softDelete()}는 시스템 시계({@code OffsetDateTime.now()})를 직접 쓰므로 {@code deletedAt}이
     * 인자 {@code clock}과 미세하게 다를 수 있지만, 배치는 {@code hardDeleteAt} 컬럼만 보고 움직이니 영향이 없다.
     *
     * @param clock 현재 시각 소스. 보통 Spring bean으로 주입된 {@code Clock}.
     * @param grace 탈퇴 요청부터 물리 삭제까지의 기간 (예: 30일). null 금지.
     */
    public void scheduleHardDelete(Clock clock, Duration grace) {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(grace, "grace");
        if (isDeleted()) {
            return;
        }
        softDelete();
        this.hardDeleteAt = OffsetDateTime.now(clock).plus(grace);
    }

    /**
     * 알림 카피 라운드로빈 인덱스 갱신(MYP-004).
     *
     * <p>스케줄러가 발송을 마친 직후 호출한다. {@code (currentIndex + 1) % total}로 다음 카피를 가리키며, 풀 크기({@code
     * total})가 줄어 인덱스가 범위를 벗어나도 mod 연산으로 안전하게 정규화된다.
     *
     * <p>{@code total}은 컬럼 타입({@code SMALLINT})의 양의 범위 안이어야 한다. 풀 크기가 그보다 크면 {@code short} 캐스트 시 음수
     * 인덱스로 오염될 수 있어 사전 거부한다.
     *
     * @param total 카피 풀 크기. 1 이상 {@code Short.MAX_VALUE + 1} 이하.
     */
    public void advanceCopyIndex(int total) {
        if (total <= 0) {
            throw new IllegalArgumentException("total must be > 0, got " + total);
        }
        if (total > Short.MAX_VALUE + 1) {
            throw new IllegalArgumentException(
                    "total must be <= " + (Short.MAX_VALUE + 1) + ", got " + total);
        }
        int next = Math.floorMod(this.notificationCopyIndex + 1, total);
        this.notificationCopyIndex = (short) next;
    }

    /**
     * 스크럼 작성 시 호출 — KST 일자 기준으로 연속 기록 일수와 마지막 기록일을 갱신한다(REC-001).
     *
     * <p>분기:
     *
     * <ul>
     *   <li>{@code lastRecordDate}가 NULL → 첫 기록. {@code currentStreak = 1}, {@code lastRecordDate =
     *       kstDate}.
     *   <li>{@code kstDate}가 {@code lastRecordDate}와 같거나 이전 → noop(같은 날 멱등 + 백데이트 무시).
     *   <li>{@code gap == 1} → {@code currentStreak += 1}({@link Short#MAX_VALUE} 도달 시 cap).
     *   <li>{@code gap >= 2} → {@code currentStreak = 1}로 리셋.
     * </ul>
     *
     * <p>시간대 변환 책임은 호출 측 — KST 기준 {@link LocalDate}를 그대로 받는다. 같은 트랜잭션 내 dirty checking으로 영속화되도록 호출
     * use case가 설계한다.
     *
     * @param kstDate KST 기준 기록 일자.
     */
    public void recordOnDate(LocalDate kstDate) {
        Objects.requireNonNull(kstDate, "kstDate");
        if (this.lastRecordDate == null) {
            this.currentStreak = 1;
            this.lastRecordDate = kstDate;
            return;
        }
        if (!kstDate.isAfter(this.lastRecordDate)) {
            return;
        }
        long gap = ChronoUnit.DAYS.between(this.lastRecordDate, kstDate);
        if (gap == 1) {
            if (this.currentStreak < Short.MAX_VALUE) {
                this.currentStreak = (short) (this.currentStreak + 1);
            }
        } else {
            this.currentStreak = 1;
        }
        this.lastRecordDate = kstDate;
    }

    /**
     * KST 기준 오늘 시점의 연속 기록 일수와 째려보는 캐릭터 노출 여부 산정(REC-001).
     *
     * <p>분기:
     *
     * <ul>
     *   <li>{@code lastRecordDate}가 NULL → {@code (0, false)}(가입 직후 = 기본 캐릭터).
     *   <li>{@code gap <= 1}(오늘 또는 어제 기록) → {@code (currentStreak, false)} — streak 유지/노출.
     *   <li>{@code gap == 2}(2일 미기록) → {@code (0, false)} — streak는 끊겨 0이지만 캐릭터는 아직
     *       기본(glaring=false). 기획상 "3일 이상 미기록"부터 째려보는으로 전환되는 분기.
     *   <li>{@code gap >= 3}(3일 이상 미기록) → {@code (0, true)} — 째려보는 캐릭터.
     * </ul>
     *
     * <p>시간대 변환 책임은 호출 측 — KST 기준 {@link LocalDate}를 그대로 받는다.
     *
     * @param kstToday KST 기준 오늘 날짜.
     */
    public RecordStreakSnapshot streakSnapshotAsOf(LocalDate kstToday) {
        Objects.requireNonNull(kstToday, "kstToday");
        if (this.lastRecordDate == null) {
            return new RecordStreakSnapshot(0, false);
        }
        long gap = ChronoUnit.DAYS.between(this.lastRecordDate, kstToday);
        if (gap <= 1) {
            return new RecordStreakSnapshot(this.currentStreak, false);
        }
        if (gap == 2) {
            return new RecordStreakSnapshot(0, false);
        }
        return new RecordStreakSnapshot(0, true);
    }
}
