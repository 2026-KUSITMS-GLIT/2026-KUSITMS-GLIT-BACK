package com.groute.groute_server.record.application.port.out.user;

import java.time.LocalDate;

/**
 * 스크럼이 작성됐을 때 user의 연속 기록 일수/마지막 기록일을 갱신해 달라고 알리는 outbound port(REC-001).
 *
 * <p>읽기용 {@link UserReferencePort}와 쓰기 책임을 분리했다. 호출자는 "이 user가 이 날짜에 기록했다"는 사실만 전달하고, streak를 어떻게
 * 증가시키거나 리셋할지는 user 엔티티가 알아서 판단한다.
 */
public interface UserStreakPort {

    /**
     * user가 KST 기준 {@code kstDate}에 기록을 남겼음을 알린다 → 결과로 user의 streak 컬럼이 갱신된다.
     *
     * <p>같은 일자로 여러 번 호출돼도 안전하다. entity의 {@link
     * com.groute.groute_server.user.entity.User#recordOnDate}가 같은 날 중복 호출을 무시하기 때문이다. 과거 일자(백데이트)도
     * entity가 걸러 streak에는 영향이 없다.
     *
     * <p>부분 커밋이 일어나지 않도록 호출자의 {@code @Transactional} 안에서 호출해야 한다.
     *
     * @param userId 갱신 대상 user.
     * @param kstDate 기록이 발생한 KST 일자.
     */
    void recordOnDate(Long userId, LocalDate kstDate);
}
