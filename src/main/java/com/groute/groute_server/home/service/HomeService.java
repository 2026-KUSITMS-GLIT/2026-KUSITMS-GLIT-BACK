package com.groute.groute_server.home.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.home.dto.RadarResult;
import com.groute.groute_server.record.application.port.out.star.CompetencyCount;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final UserRepository userRepository;

    public RadarResult getRadar(Long userId) {
        List<CompetencyCount> rows =
                starRecordRepositoryPort.countCompletedByCompetency(
                        userId, StarRecordStatus.TAGGED);

        Map<CompetencyCategory, Integer> categories = new EnumMap<>(CompetencyCategory.class);
        for (CompetencyCategory cat : CompetencyCategory.values()) {
            categories.put(cat, 0);
        }
        for (CompetencyCount cc : rows) {
            categories.put(cc.competency(), cc.count().intValue());
        }

        int min = Collections.min(categories.values());
        int max = Collections.max(categories.values());
        return new RadarResult(min, max, Collections.unmodifiableMap(categories));
    }

    public String getBrandingTitle(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .getBrandingTitle();
    }
}
