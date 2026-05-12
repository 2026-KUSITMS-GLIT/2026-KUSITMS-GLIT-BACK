package com.groute.groute_server.report.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;

/** 심화기록 조회 포트. 리포트 도메인이 record 도메인 DB에 접근하기 위한 out 포트. */
public interface LoadStarRecordPort {

    /**
     * 유저의 전체 완료된 심화기록 수를 반환한다.
     *
     * @param userId 유저 PK
     * @return 완료된 심화기록 총 개수
     */
    int countCompletedByUserId(Long userId);

    /**
     * 유저의 완료된 심화기록을 최신순으로 반환한다.
     *
     * <p>같은 날짜 내 여러 심화기록은 기록 순서 기준 가장 나중 것부터 산정한다(기능명세서 RPT-002).
     *
     * @param userId 유저 PK
     * @param limit 최대 반환 개수
     * @return 완료된 심화기록 목록 (최신순)
     */
    List<StarRecord> findCompletedByUserIdOrderByLatest(Long userId, int limit);

    /**
     * 유저의 완료된 심화기록이 있는 날짜 목록을 전체 기간 반환한다.
     *
     * <p>달력 하이라이트 렌더링용(기능명세서 RPT-002).
     *
     * @param userId 유저 PK
     * @return 완료된 심화기록이 존재하는 날짜 목록
     */
    List<LocalDate> findCompletedStarDatesByUserId(Long userId);

    /**
     * 심화기록 ID 목록으로 심화기록을 조회한다.
     *
     * <p>POST /api/reports 시 선택된 심화기록 로드용. userId로 소유권을 함께 검증한다.
     *
     * @param userId 유저 PK
     * @param starRecordIds 심화기록 ID 목록
     * @return 심화기록 목록
     */
    List<StarRecord> findAllByIds(Long userId, List<Long> starRecordIds);

    /**
     * 선택된 심화기록의 날짜에 속한 스크럼 목록을 반환한다.
     *
     * <p>AI 인풋 구성 시 심화기록과 함께 해당 날짜 스크럼을 자동 포함(기능명세서 RPT-002).
     *
     * @param userId 유저 PK
     * @param starRecordIds 선택된 심화기록 ID 목록
     * @return 해당 날짜들의 스크럼 목록
     */
    List<Scrum> findScrumsByStarRecordIds(Long userId, List<Long> starRecordIds);

    /**
     * 유저의 특정 날짜에 완료된 심화기록 목록을 반환한다.
     *
     * <p>리포트 생성 화면에서 날짜 셀 클릭 시 모달에 노출할 심화기록 목록 조회용. 기록 순서 기준 오름차순 정렬.
     *
     * @param userId 유저 PK
     * @param date 조회 날짜
     * @return 해당 날짜에 완료된 심화기록 목록
     */
    List<StarRecord> findCompletedByUserIdAndDate(Long userId, LocalDate date);
}