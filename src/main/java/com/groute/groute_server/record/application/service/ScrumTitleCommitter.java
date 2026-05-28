package com.groute.groute_server.record.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;

import lombok.RequiredArgsConstructor;

/** 사용자·일자별로 더 이상 미완료 StarRecord가 없으면 해당 일자의 ScrumTitle을 COMMITTED로 전환한다. */
@Component
@RequiredArgsConstructor
public class ScrumTitleCommitter {

    private final StarRecordRepositoryPort starRecordPort;
    private final ScrumQueryPort scrumQueryPort;
    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;

    public void commitIfFullyTagged(Long userId, LocalDate date) {
        if (starRecordPort.existsUntaggedByUserAndDate(userId, date)) {
            return;
        }
        List<Long> titleIds =
                scrumQueryPort.findAllByUserAndDate(userId, date).stream()
                        .map(Scrum::getTitle)
                        .map(ScrumTitle::getId)
                        .distinct()
                        .toList();
        scrumTitleRepositoryPort.commitAllByIds(titleIds);
    }
}
