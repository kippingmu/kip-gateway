package xyz.kip.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 请求缓存过滤器
 * 使用 Redis 缓存 GET 请求的响应
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Component
public class RequestCacheFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestCacheFilter.class);

    private static final String CACHE_KEY_PREFIX = "gateway:cache:";
    private static final String CACHE_HIT_HEADER = "X-Cache-Hit";

    @Value("${gateway.cache.enabled:false}")
    private boolean enabled;

    @Value("${gateway.cache.ttl:300}")
    private long cacheTtl; // 缓存过期时间（秒）

    @Value("${gateway.cache.paths:}")
    private String cachePaths; // 需要缓存的路径，逗号分隔

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RequestCacheFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled || !shouldCache(exchange)) {
            return chain.filter(exchange);
        }

        String cacheKey = generateCacheKey(exchange);

        // 尝试从缓存获取
        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(cachedResponse -> {
                    logger.debug("traceId={}, Cache hit for key: {}", TraceIdUtil.getTraceId(), cacheKey);
                    return writeCachedResponse(exchange, cachedResponse);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    logger.debug("traceId={}, Cache miss for key: {}", TraceIdUtil.getTraceId(), cacheKey);
                    return cacheResponse(exchange, chain, cacheKey);
                }));
    }

    /**
     * 判断请求是否应该被缓存
     */
    private boolean shouldCache(ServerWebExchange exchange) {
        // 只缓存 GET 请求
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }

        String path = exchange.getRequest().getPath().value();

        // 检查路径是否在缓存列表中
        Set<String> cachePathSet = parseCachePaths();
        if (cachePathSet.isEmpty()) {
            return false;
        }

        for (String cachePath : cachePathSet) {
            if (path.startsWith(cachePath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getURI().getQuery();
        String key = path + (query != null ? "?" + query : "");
        return CACHE_KEY_PREFIX + key;
    }

    /**
     * 写入缓存的响应
     */
    private Mono<Void> writeCachedResponse(ServerWebExchange exchange, String cachedResponse) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(CACHE_HIT_HEADER, "true");
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 缓存响应
     */
    private Mono<Void> cacheResponse(ServerWebExchange exchange, GatewayFilterChain chain, String cacheKey) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ResponseCacheDecorator decoratedResponse = new ResponseCacheDecorator(originalResponse, cacheKey);

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .then(Mono.defer(() -> {
                    String responseBody = decoratedResponse.getResponseBody();
                    if (responseBody != null && !responseBody.isEmpty()) {
                        return redisTemplate.opsForValue()
                                .set(cacheKey, responseBody, Duration.ofSeconds(cacheTtl))
                                .doOnSuccess(v -> logger.debug("traceId={}, Cached response for key: {}",
                                        TraceIdUtil.getTraceId(), cacheKey))
                                .then();
                    }
                    return Mono.empty();
                }));
    }

    /**
     * 解析缓存路径配置
     */
    private Set<String> parseCachePaths() {
        if (cachePaths == null || cachePaths.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(cachePaths.split(",")));
    }

    @Override
    public int getOrder() {
        return -80; // 在认证之后，业务处理之前
    }

    /**
     * 响应缓存装饰器
     */
    private class ResponseCacheDecorator extends org.springframework.http.server.reactive.ServerHttpResponseDecorator {

        private final String cacheKey;
        private final StringBuilder responseBody = new StringBuilder();

        public ResponseCacheDecorator(ServerHttpResponse delegate, String cacheKey) {
            super(delegate);
            this.cacheKey = cacheKey;
        }

        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            if (body instanceof Flux) {
                Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                return super.writeWith(fluxBody.map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);

                    responseBody.append(new String(content, StandardCharsets.UTF_8));
                    return getDelegate().bufferFactory().wrap(content);
                }));
            }
            return super.writeWith(body);
        }

        public String getResponseBody() {
            return responseBody.toString();
        }
    }
}
