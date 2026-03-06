package xyz.kip.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel 熔断降级处理器
 * 当服务被熔断或限流时，返回友好的降级响应
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Component
public class SentinelFallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(SentinelFallbackHandler.class);

    /**
     * 限流降级处理
     */
    public Mono<ServerResponse> handleBlockedRequest(Throwable throwable) {
        logger.warn("traceId={}, Request blocked by Sentinel: {}",
                TraceIdUtil.getTraceId(), throwable.getMessage());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", "系统繁忙，请稍后再试");
        result.put("traceId", TraceIdUtil.getTraceId());
        result.put("timestamp", System.currentTimeMillis());

        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(result));
    }

    /**
     * 熔断降级处理
     */
    public Mono<ServerResponse> handleCircuitBreakerFallback(Throwable throwable) {
        logger.error("traceId={}, Circuit breaker triggered: {}",
                TraceIdUtil.getTraceId(), throwable.getMessage());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，请稍后再试");
        result.put("traceId", TraceIdUtil.getTraceId());
        result.put("timestamp", System.currentTimeMillis());

        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(result));
    }

    /**
     * 通用异常降级处理
     */
    public Mono<ServerResponse> handleFallback(Throwable throwable) {
        logger.error("traceId={}, Fallback triggered: {}",
                TraceIdUtil.getTraceId(), throwable.getMessage(), throwable);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "服务异常，请稍后再试");
        result.put("traceId", TraceIdUtil.getTraceId());
        result.put("timestamp", System.currentTimeMillis());

        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(result));
    }
}
