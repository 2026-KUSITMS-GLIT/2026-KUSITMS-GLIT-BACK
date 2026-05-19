package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.record.application.port.in.calendar.DailyCalendarView;
import com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarQuery;
import com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;

import lombok.RequiredArgsConstructor;

/**
 * 일자별 스크럼 조회 서비스 (CAL-002).
 *
 * <p>해당 일자의 사용자 스크럼 전체를 ScrumTitle 단위로 그룹핑하여 반환한다. 각 스크럼은 작성 14일 이내이고 hasStar=false 일 때만 수정 가능하다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService implements GetDailyCalendarUseCase {

    private static final int EDIT_WINDOW_DAYS = 14;

    private final ScrumQueryPort scrumQueryPort;

    /**
     * 일자별 스크럼 전체를 ScrumTitle 단위로 묶어 group/item 2계층 View로 반환.
     *
     * <p>스크럼이 없는 일자는 빈 그룹 배열을 반환한다. 그룹·항목의 정렬은 레포지토리 쿼리(titleId asc, id asc)에 위임한다.
     */
    @Override
    public DailyCalendarView getDailyCalendar(GetDailyCalendarQuery query) {
        // 1. 해당 일자의 사용자 스크럼 전체 로드 (Title·Project fetch join)
        List<Scrum> scrums = scrumQueryPort.findAllByUserAndDate(query.userId(), query.date());
        if (scrums.isEmpty()) {
            return new DailyCalendarView(List.of());
        }

        // 2. 14일 경계 비교용 today (DB UTC ↔ 시스템 zone 정합)
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        // 3. titleId 기준 그룹핑 (LinkedHashMap으로 정렬 보존)
        Map<Long, List<Scrum>> grouped = new LinkedHashMap<>();
        for (Scrum scrum : scrums) {
            grouped.computeIfAbsent(scrum.getTitle().getId(), k -> new ArrayList<>()).add(scrum);
        }

        // 4. 그룹별로 ItemView 생성 + 그룹 editable 집계
        List<DailyCalendarView.GroupView> groups = new ArrayList<>(grouped.size());
        for (List<Scrum> bucket : grouped.values()) {
            ScrumTitle title = bucket.get(0).getTitle();
            List<DailyCalendarView.ItemView> items = new ArrayList<>(bucket.size());
            boolean groupEditable = false;
            for (Scrum scrum : bucket) {
                boolean editable = isEditable(scrum, today, zone);
                items.add(
                        new DailyCalendarView.ItemView(
                                scrum.getId(), scrum.getContent(), scrum.isHasStar(), editable));
                if (editable) {
                    groupEditable = true;
                }
            }
            groups.add(
                    new DailyCalendarView.GroupView(
                            title.getId(),
                            title.getProject().getName(),
                            title.getFreeText(),
                            groupEditable,
                            items));
        }
        return new DailyCalendarView(groups);
    }

    /**
     * 항목 수정 가능 여부.
     *
     * <p>심화기록(STAR)이 작성된 스크럼은 수정 잠금. 그 외에는 작성 14일 이내일 때만 수정 가능.
     */
    private boolean isEditable(Scrum scrum, LocalDate today, ZoneId zone) {
        if (scrum.isHasStar()) {
            return false;
        }
        LocalDate createdDate = scrum.getCreatedAt().atZoneSameInstant(zone).toLocalDate();
        return !createdDate.plusDays(EDIT_WINDOW_DAYS).isBefore(today);
    }
}
