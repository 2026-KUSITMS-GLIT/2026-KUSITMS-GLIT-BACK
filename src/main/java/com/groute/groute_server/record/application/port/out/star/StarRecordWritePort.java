package com.groute.groute_server.record.application.port.out.star;

import com.groute.groute_server.record.domain.StarRecord;

/** StarRecord 쓰기 포트. */
public interface StarRecordWritePort {

    /** 신규 StarRecord 저장. */
    StarRecord save(StarRecord starRecord);
}
