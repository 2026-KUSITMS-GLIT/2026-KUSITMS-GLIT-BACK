package com.groute.groute_server.home.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.home.repository.HomeRepository;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;

    public RadarResult getRadar(Long userId) {
        List<Object[]> rows =
                homeRepository.countCompletedByCompetency(userId, StarRecordStatus.TAGGED);

        Map<CompetencyCategory, Integer> categories = new EnumMap<>(CompetencyCategory.class);
        for (CompetencyCategory cat : CompetencyCategory.values()) {
            categories.put(cat, 0);
        }
        for (Object[] row : rows) {
            categories.put((CompetencyCategory) row[0], ((Number) row[1]).intValue());
        }

        int min = Collections.min(categories.values());
        int max = Collections.max(categories.values());
        return new RadarResult(min, max, categories);
    }

    public String getBrandingTitle(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .getBrandingTitle();
    }

    public record RadarResult(int min, int max, Map<CompetencyCategory, Integer> categories) {}
}
