package com.groute.groute_server.record.application.port.out.star;

import java.util.Collection;

import com.groute.groute_server.record.domain.StarImage;

/** StarImage 쓰기 포트. */
public interface StarImageWritePort {

    StarImage save(StarImage starImage);

    void deleteById(Long id);

    void deleteAll(Collection<StarImage> images);
}
