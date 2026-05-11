package com.groute.groute_server.record.application.port.out.star;

import com.groute.groute_server.record.domain.StarImage;

/** StarImage 쓰기 포트. */
public interface StarImageWritePort {

    StarImage save(StarImage starImage);
}