package xyz.kip.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.IpUtil;

/**
 * IP 黑白名单管理服务
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Service
public class IpManagementService {

    private static final Logger logger = LoggerFactory.getLogger(IpManagementService.class);

    private static final String REDIS_BLACKLIST_KEY = "gateway:ip:blacklist";
    private static final String REDIS_WHITELIST_KEY = "gateway:ip:whitelist";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public IpManagementService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 添加 IP 到黑名单
     */
    public Mono<Boolean> addToBlacklist(String ip) {
        if (!IpUtil.isValidCidr(ip)) {
            return Mono.error(new IllegalArgumentException("Invalid IP or CIDR format: " + ip));
        }
        logger.info("Adding IP to blacklist: {}", ip);
        return redisTemplate.opsForSet().add(REDIS_BLACKLIST_KEY, ip)
                .map(count -> count > 0);
    }

    /**
     * 从黑名单移除 IP
     */
    public Mono<Boolean> removeFromBlacklist(String ip) {
        logger.info("Removing IP from blacklist: {}", ip);
        return redisTemplate.opsForSet().remove(REDIS_BLACKLIST_KEY, ip)
                .map(count -> count > 0);
    }

    /**
     * 获取黑名单列表
     */
    public Flux<String> getBlacklist() {
        return redisTemplate.opsForSet().members(REDIS_BLACKLIST_KEY);
    }

    /**
     * 清空黑名单
     */
    public Mono<Boolean> clearBlacklist() {
        logger.warn("Clearing all blacklist entries");
        return redisTemplate.delete(REDIS_BLACKLIST_KEY)
                .map(count -> count > 0);
    }

    /**
     * 添加 IP 到白名单
     */
    public Mono<Boolean> addToWhitelist(String ip) {
        if (!IpUtil.isValidCidr(ip)) {
            return Mono.error(new IllegalArgumentException("Invalid IP or CIDR format: " + ip));
        }
        logger.info("Adding IP to whitelist: {}", ip);
        return redisTemplate.opsForSet().add(REDIS_WHITELIST_KEY, ip)
                .map(count -> count > 0);
    }

    /**
     * 从白名单移除 IP
     */
    public Mono<Boolean> removeFromWhitelist(String ip) {
        logger.info("Removing IP from whitelist: {}", ip);
        return redisTemplate.opsForSet().remove(REDIS_WHITELIST_KEY, ip)
                .map(count -> count > 0);
    }

    /**
     * 获取白名单列表
     */
    public Flux<String> getWhitelist() {
        return redisTemplate.opsForSet().members(REDIS_WHITELIST_KEY);
    }

    /**
     * 清空白名单
     */
    public Mono<Boolean> clearWhitelist() {
        logger.warn("Clearing all whitelist entries");
        return redisTemplate.delete(REDIS_WHITELIST_KEY)
                .map(count -> count > 0);
    }
}
