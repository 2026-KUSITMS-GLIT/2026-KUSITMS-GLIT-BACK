package com.groute.groute_server.common.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis 기반 캐시 설정.
 *
 * <p>refresh token용 {@link org.springframework.data.redis.core.StringRedisTemplate}과 완전히 분리된 {@link
 * RedisCacheManager}를 구성한다. {@link JavaTimeModule}을 주입한 {@link ObjectMapper}로 {@code LocalDate} 등
 * JSR-310 타입 직렬화를 보장한다.
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
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put(CACHE_HOME_RADAR, defaultConfig().entryTtl(Duration.ofHours(1)));
        configs.put(CACHE_HOME_COMPETENCY_STATS, defaultConfig().entryTtl(Duration.ofHours(1)));
        configs.put(CACHE_CALENDAR_MONTHLY, defaultConfig().entryTtl(Duration.ofMinutes(30)));
        configs.put(CACHE_USERS_ME, defaultConfig().entryTtl(Duration.ofMinutes(10)));
        configs.put(CACHE_REPORTS_DETAIL, defaultConfig().entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(cf).withInitialCacheConfigurations(configs).build();
    }

    private RedisCacheConfiguration defaultConfig() {
        ObjectMapper om =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(om)));
    }
}
