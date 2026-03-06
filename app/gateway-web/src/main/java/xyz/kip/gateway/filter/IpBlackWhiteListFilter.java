package xyz.kip.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * IP 黑白名单过滤器
 * 支持静态配置和 Redis 动态配置
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Component
public class IpBlackWhiteListFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(IpBlackWhiteListFilter.class);

    private static final String REDIS_BLACKLIST_KEY = "gateway:ip:blacklist";
    private static final String REDIS_WHITELIST_KEY = "gateway:ip:whitelist";

    @Value("${gateway.ip-filter.enabled:true}")
    private boolean enabled;

    @Value("${gateway.ip-filter.mode:blacklist}")
    private String mode; // blacklist 或 whitelist

    @Value("${gateway.ip-filter.static-blacklist:}")
    private String staticBlacklist;

    @Value("${gateway.ip-filter.static-whitelist:}")
    private String staticWhitelist;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public IpBlackWhiteListFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange);
        String path = exchange.getRequest().getPath().value();

        logger.debug("traceId={}, Checking IP filter for IP: {}, path: {}, mode: {}",
                TraceIdUtil.getTraceId(), clientIp, path, mode);

        if ("whitelist".equalsIgnoreCase(mode)) {
            // 白名单模式：只允许白名单中的 IP 访问
            return isInWhitelist(clientIp)
                    .flatMap(inWhitelist -> {
                        if (inWhitelist) {
                            logger.debug("traceId={}, IP {} is in whitelist, allowing access",
                                    TraceIdUtil.getTraceId(), clientIp);
                            return chain.filter(exchange);
                        } else {
                            logger.warn("traceId={}, IP {} is not in whitelist, blocking access",
                                    TraceIdUtil.getTraceId(), clientIp);
                            return handleBlockedRequest(exchange, "IP 不在白名单中");
                        }
                    });
        } else {
            // 黑名单模式：拒绝黑名单中的 IP 访问
            return isInBlacklist(clientIp)
                    .flatMap(inBlacklist -> {
                        if (inBlacklist) {
                            logger.warn("traceId={}, IP {} is in blacklist, blocking access",
                                    TraceIdUtil.getTraceId(), clientIp);
                            return handleBlockedRequest(exchange, "IP 已被封禁");
                        } else {
                            logger.debug("traceId={}, IP {} is not in blacklist, allowing access",
                                    TraceIdUtil.getTraceId(), clientIp);
                            return chain.filter(exchange);
                        }
                    });
        }
    }

    /**
     * 检查 IP 是否在黑名单中（支持 CIDR）
     */
    private Mono<Boolean> isInBlacklist(String ip) {
        // 先检查静态黑名单
        Set<String> staticList = parseIpList(staticBlacklist);
        if (matchesIpList(ip, staticList)) {
            return Mono.just(true);
        }

        // 再检查 Redis 动态黑名单
        return redisTemplate.opsForSet().members(REDIS_BLACKLIST_KEY)
                .collectList()
                .map(list -> matchesIpList(ip, new HashSet<>(list)))
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    /**
     * 检查 IP 是否在白名单中（支持 CIDR）
     */
    private Mono<Boolean> isInWhitelist(String ip) {
        // 先检查静态白名单
        Set<String> staticList = parseIpList(staticWhitelist);
        if (matchesIpList(ip, staticList)) {
            return Mono.just(true);
        }

        // 再检查 Redis 动态白名单
        return redisTemplate.opsForSet().members(REDIS_WHITELIST_KEY)
                .collectList()
                .map(list -> matchesIpList(ip, new HashSet<>(list)))
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    /**
     * 检查 IP 是否匹配列表中的任一规则（支持 CIDR）
     */
    private boolean matchesIpList(String ip, Set<String> ipList) {
        for (String rule : ipList) {
            if (xyz.kip.gateway.util.IpUtil.matchesCidr(ip, rule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 IP 列表配置
     */
    private Set<String> parseIpList(String ipListStr) {
        if (ipListStr == null || ipListStr.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(ipListStr.split(",")));
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "UNKNOWN";
    }

    /**
     * 处理被阻止的请求
     */
    private Mono<Void> handleBlockedRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");

        String errorBody = String.format(
                "{\"code\":403,\"message\":\"%s\",\"traceId\":\"%s\"}",
                message, TraceIdUtil.getTraceId()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -95; // 在认证之前执行
    }
}
