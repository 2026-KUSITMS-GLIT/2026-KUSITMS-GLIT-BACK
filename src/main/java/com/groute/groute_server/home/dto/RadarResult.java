package com.groute.groute_server.home.dto;

import java.util.Map;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

public record RadarResult(int min, int max, Map<CompetencyCategory, Integer> categories) {}
