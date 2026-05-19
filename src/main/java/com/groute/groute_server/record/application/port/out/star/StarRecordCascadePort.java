package com.groute.groute_server.record.application.port.out.star;

import java.util.Collection;

/** Scrum soft-delete 시 연결된 STAR 기록을 cascade soft-delete 하기 위한 포트. */
public interface StarRecordCascadePort {

    /** 동일 트랜잭션에서 Scrum 삭제와 함께 호출. */
    void cascadeDeleteByScrumIdIn(Collection<Long> scrumIds);
}
