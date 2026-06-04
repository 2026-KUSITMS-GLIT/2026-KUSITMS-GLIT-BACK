package com.groute.groute_server.common.config;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.groute.groute_server.calendar.service.CalendarMonthlyView;
import com.groute.groute_server.home.dto.RadarResult;
import com.groute.groute_server.report.application.port.in.dto.ReportDetailView;

/**
 * Redis 기반 캐시 설정.
 *
 * <p>refresh token용 {@link org.springframework.data.redis.core.StringRedisTemplate}과 완전히 분리된 {@link
 * RedisCacheManager}를 구성한다.
 *
 * <p>캐시마다 반환 타입을 못박은 {@link Jackson2JsonRedisSerializer}를 사용한다. default typing(@class) 없이 직렬화하던 기존
 * 방식은 역직렬화 시 타입정보가 없어 {@code LinkedHashMap}으로 복원되어 record/엔티티 캐스팅 실패(500)를 유발했다. 타입을 명시하면 해당 타입으로
 * 정확히 복원되어 왕복이 안전하다. {@link JavaTimeModule}로 {@code LocalDate}/{@code YearMonth} 등 JSR-310 타입 직렬화를
 * 보장한다.
 *
 * <p>{@code users:me} 캐시는 직군 브랜딩 문구(String)만 담는다. 마이페이지 프로필은 JPA 엔티티를 그대로 캐싱하면 역직렬화가 취약해 캐싱하지
 * 않는다(필요 시 별도 DTO 캐싱으로 후속 도입).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_HOME_RADAR = "home:radar";
    public static final String CACHE_HOME_COMPETENCY_STATS = "home:competency-stats";
    public static final String CACHE_CALENDAR_MONTHLY = "calendar:monthly";
    public static final String CACHE_USERS_ME = "users:me";
    public static final String CACHE_REPORTS_DETAIL = "reports:detail";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper om = cacheObjectMapper();

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put(
                CACHE_HOME_RADAR,
                typedConfig(new Jackson2JsonRedisSerializer<>(om, RadarResult.class))
                        .entryTtl(Duration.ofHours(1)));
        configs.put(
                CACHE_HOME_COMPETENCY_STATS,
                typedConfig(
                                new Jackson2JsonRedisSerializer<>(
                                        om,
                                        om.getTypeFactory()
                                                .constructMapType(
                                                        LinkedHashMap.class,
                                                        LocalDate.class,
                                                        Long.class)))
                        .entryTtl(Duration.ofHours(1)));
        configs.put(
                CACHE_CALENDAR_MONTHLY,
                typedConfig(new Jackson2JsonRedisSerializer<>(om, CalendarMonthlyView.class))
                        .entryTtl(Duration.ofMinutes(30)));
        configs.put(
                CACHE_USERS_ME,
                typedConfig(new Jackson2JsonRedisSerializer<>(om, String.class))
                        .entryTtl(Duration.ofMinutes(10)));
        configs.put(
                CACHE_REPORTS_DETAIL,
                typedConfig(new Jackson2JsonRedisSerializer<>(om, ReportDetailView.class))
                        .entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(cf).withInitialCacheConfigurations(configs).build();
    }

    /** JSR-310 타입(LocalDate/YearMonth 등) 직렬화를 보장하는 캐시 전용 ObjectMapper. */
    ObjectMapper cacheObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private RedisCacheConfiguration typedConfig(RedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                valueSerializer));
    }
}
