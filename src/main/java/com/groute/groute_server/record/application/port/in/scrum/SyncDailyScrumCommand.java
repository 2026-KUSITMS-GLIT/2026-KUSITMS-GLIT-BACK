package com.groute.groute_server.record.application.port.in.scrum;

import java.time.LocalDate;
import java.util.List;

/**
 * 일자별 스크럼 일괄 sync 입력.
 *
 * <p>요청 payload에 포함된 group/item만 살아남는다. 기존 DB에 있으나 요청에 없는 항목은 soft-delete 대상.
 */
public record SyncDailyScrumCommand(Long userId, LocalDate date, List<GroupCommand> groups) {

    /** 그룹 단위. titleId는 항상 기존 ScrumTitle 참조 (신규 Title 생성은 별도 API). */
    public record GroupCommand(Long titleId, List<ItemCommand> items) {}

    /**
     * 항목 단위.
     *
     * @param scrumId null이면 신규 생성, 값이 있으면 기존 항목 식별자(content 변경 시 update 대상)
     */
    public record ItemCommand(Long scrumId, String content) {}
}
