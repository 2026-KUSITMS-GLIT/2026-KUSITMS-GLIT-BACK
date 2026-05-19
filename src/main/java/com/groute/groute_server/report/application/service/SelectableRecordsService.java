package com.groute.groute_server.report.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.report.application.port.in.GetSelectableRecordsUseCase;
import com.groute.groute_server.report.application.port.in.SelectableRecordsView;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;

import lombok.RequiredArgsConstructor;

/**
 * 날짜별 심화기록 모달 조회 서비스.
 *
 * <p>리포트 생성 화면에서 날짜 셀 클릭 시 해당 날짜의 완료된 심화기록 목록을 반환한다. 심화기록이 없는 날짜는 빈 배열을 반환한다.
 */
@Service
@RequiredArgsConstructor
public class SelectableRecordsService implements GetSelectableRecordsUseCase {

    private final LoadStarRecordPort loadStarRecordPort;

    @Override
    @Transactional(readOnly = true)
    public SelectableRecordsView getSelectableRecords(Long userId, LocalDate date) {
        List<SelectableRecordsView.StarRecordItem> items =
                loadStarRecordPort.findCompletedByUserIdAndDate(userId, date).stream()
                        .map(
                                sr ->
                                        new SelectableRecordsView.StarRecordItem(
                                                sr.getId(),
                                                sr.getScrum().getTitle().getProject().getName(),
                                                sr.getScrum().getContent()))
                        .toList();

        return new SelectableRecordsView(date.format(DateTimeFormatter.ISO_LOCAL_DATE), items);
    }
}
