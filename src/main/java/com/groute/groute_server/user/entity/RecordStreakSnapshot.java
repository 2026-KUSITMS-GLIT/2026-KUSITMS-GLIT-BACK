package com.groute.groute_server.user.entity;

/**
 * REC-001 연속 기록 일수 + 째려보는 캐릭터 노출 여부 read model.
 *
 * <p>API 응답 DTO가 아닌 {@link User}가 자체 상태로부터 산정해 노출하는 도메인 read model. 같은 패키지에 둬 entity → service 역방향
 * import를 피한다. KST 기준이며 산정 책임은 {@link User#streakSnapshotAsOf(java.time.LocalDate)}.
 *
 * @param consecutiveDays 끊기지 않은 기록 연속 일수. 0~2일 미기록은 기존 streak 유지, 3일 이상 미기록·기록 0건은 0.
 * @param glaring 째려보는 캐릭터 노출 여부 — 마지막 기록일이 3일 이상 이전이면 true, 그 외/0건은 false.
 */
public record RecordStreakSnapshot(int consecutiveDays, boolean glaring) {}
