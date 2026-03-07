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

    /**
     * 缓存统计信息记录类（Record）
     * <p>
     * Record 是 Java 14+ 引入的不可变数据载体类型，用于简洁地表示纯数据对象。
     * 编译器会自动生成构造函数、访问器方法（getter）、equals()、hashCode() 和 toString() 方法。
     * </p>
     * <p>
     * 特性：
     * - 所有字段都是 final 的，保证不可变性和线程安全
     * - 提供便捷的访问器方法：{@code totalKeys()} 和 {@code prefix()}
     * - 自动实现基于字段的 equals() 和 hashCode()
     * - 自动生成格式化的 toString() 输出
     * </p>
     *
     * @param totalKeys 缓存键总数
     * @param prefix 缓存键前缀
     */
    public record CacheStats(long totalKeys, String prefix) {
    }
}
