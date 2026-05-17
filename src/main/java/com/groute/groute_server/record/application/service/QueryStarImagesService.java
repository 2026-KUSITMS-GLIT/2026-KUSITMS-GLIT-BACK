package com.groute.groute_server.record.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.QueryStarImagesResult;
import com.groute.groute_server.record.application.port.in.star.QueryStarImagesUseCase;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryStarImagesService implements QueryStarImagesUseCase {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarImageQueryPort starImageQueryPort;

    @Override
    public List<QueryStarImagesResult> query(Long userId, Long starRecordId) {
        StarRecord record =
                starRecordRepositoryPort
                        .findById(starRecordId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        if (!record.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        return starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(starRecordId).stream()
                .map(img -> new QueryStarImagesResult(img.getId(), img.getImageUrl()))
                .toList();
    }
}
