package xyz.kip.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 缓存管理服务
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Service
public class CacheManagementService {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagementService.class);

    private static final String CACHE_KEY_PREFIX = "gateway:cache:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public CacheManagementService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 清除指定路径的缓存
     */
    public Mono<Long> clearCacheByPath(String path) {
        String pattern = CACHE_KEY_PREFIX + path + "*";
        logger.info("Clearing cache for pattern: {}", pattern);

        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .reduce(0L, Long::sum);
    }

    /**
     * 清除所有缓存
     */
    public Mono<Long> clearAllCache() {
        String pattern = CACHE_KEY_PREFIX + "*";
        logger.warn("Clearing all cache entries");

        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .reduce(0L, Long::sum);
    }

    /**
     * 获取所有缓存键
     */
    public Flux<String> getAllCacheKeys() {
        String pattern = CACHE_KEY_PREFIX + "*";
        return redisTemplate.keys(pattern)
                .map(key -> key.substring(CACHE_KEY_PREFIX.length()));
    }

    /**
     * 获取缓存统计信息
     */
    public Mono<CacheStats> getCacheStats() {
        String pattern = CACHE_KEY_PREFIX + "*";
        return redisTemplate.keys(pattern)
                .count()
                .map(count -> new CacheStats(count, CACHE_KEY_PREFIX));
    }

    public static class CacheStats {
        private final long totalKeys;
        private final String prefix;

        public CacheStats(long totalKeys, String prefix) {
            this.totalKeys = totalKeys;
            this.prefix = prefix;
        }

        public long getTotalKeys() {
            return totalKeys;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
