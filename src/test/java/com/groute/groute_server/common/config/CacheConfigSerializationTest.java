package com.groute.groute_server.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.calendar.service.CalendarMonthlyView;
import com.groute.groute_server.home.dto.RadarResult;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.report.application.port.in.dto.ReportDetailView;
import com.groute.groute_server.report.domain.enums.ReportType;

/**
 * 캐시 값 직렬화 왕복 검증.
 *
 * <p>각 캐시가 쓰는 타입 지정 직렬화기로 직렬화 후 역직렬화했을 때 원본과 동일하게 복원되는지 확인한다. 기존에는 타입정보 없이 직렬화해 역직렬화 시 {@code
 * LinkedHashMap}으로 풀려 record/엔티티 캐스팅에 실패(500)했다. 캐시 단위 테스트가 CacheManager를 Mock 처리하느라 잡지 못한 실제 직렬화
 * 왕복을 여기서 검증한다.
 */
class CacheConfigSerializationTest {

    private final ObjectMapper om = new CacheConfig().cacheObjectMapper();

    private <T> void assertRoundTrips(Jackson2JsonRedisSerializer<T> serializer, T value) {
        byte[] bytes = serializer.serialize(value);
        T restored = serializer.deserialize(bytes);
        assertThat(restored).isEqualTo(value);
    }

    @Test
    void radarResult_roundTrips() {
        RadarResult value =
                new RadarResult(
                        0,
                        5,
                        Map.of(
                                CompetencyCategory.PLANNING_EXECUTION, 5,
                                CompetencyCategory.COLLABORATION, 2));
        assertRoundTrips(new Jackson2JsonRedisSerializer<>(om, RadarResult.class), value);
    }

    @Test
    void competencyStatsMap_roundTrips() {
        Jackson2JsonRedisSerializer<Map<LocalDate, Long>> serializer =
                new Jackson2JsonRedisSerializer<>(
                        om,
                        om.getTypeFactory()
                                .constructMapType(
                                        LinkedHashMap.class, LocalDate.class, Long.class));
        Map<LocalDate, Long> value = new LinkedHashMap<>();
        value.put(LocalDate.of(2026, 5, 1), 3L);
        value.put(LocalDate.of(2026, 5, 2), 1L);
        assertRoundTrips(serializer, value);
    }

    @Test
    void calendarMonthlyView_roundTrips() {
        CalendarMonthlyView value =
                new CalendarMonthlyView(
                        YearMonth.of(2026, 5),
                        List.of(
                                new CalendarMonthlyView.DayAggregate(
                                        LocalDate.of(2026, 5, 1),
                                        true,
                                        true,
                                        CompetencyCategory.COLLABORATION,
                                        2),
                                new CalendarMonthlyView.DayAggregate(
                                        LocalDate.of(2026, 5, 2), false, false, null, 0)));
        assertRoundTrips(new Jackson2JsonRedisSerializer<>(om, CalendarMonthlyView.class), value);
    }

    @Test
    void reportDetailView_roundTrips() {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("title", "성장 리포트");
        content.put("scores", List.of(1, 2, 3));
        ReportDetailView value =
                new ReportDetailView(1L, ReportType.MINI, "2026.05.01", 3, content);
        assertRoundTrips(new Jackson2JsonRedisSerializer<>(om, ReportDetailView.class), value);
    }

    @Test
    void brandingString_roundTrips() {
        assertRoundTrips(new Jackson2JsonRedisSerializer<>(om, String.class), "백엔드 빌더");
    }
}
