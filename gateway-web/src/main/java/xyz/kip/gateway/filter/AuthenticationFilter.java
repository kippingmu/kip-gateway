package xyz.kip.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.exception.GatewayException;
import xyz.kip.gateway.util.TraceIdUtil;

import java.util.Arrays;
import java.util.List;

/**
 * 身份认证全局过滤器
 * 验证JWT Token或API密钥
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    /**
     * 不需要认证的路径（白名单）
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/health",
            "/actuator",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/auth/login",
            "/api/auth/register"
    );

    /**
     * 执行认证过滤器
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 检查是否在白名单中
        if (isWhitelist(path)) {
            return chain.filter(exchange);
        }

        try {
            // 获取请求头中的Token
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || authHeader.isEmpty()) {
                logger.warn("traceId={}, No authentication token provided for path: {}",
                        TraceIdUtil.getTraceId(), path);
                return handleAuthenticationError(exchange, "Missing authentication token");
            }

            // 验证Token格式
            if (!authHeader.startsWith("Bearer ")) {
                logger.warn("traceId={}, Invalid token format for path: {}",
                        TraceIdUtil.getTraceId(), path);
                return handleAuthenticationError(exchange, "Invalid token format");
            }

            String token = authHeader.substring(7);

            // 验证Token（这里简化处理，实际应该验证JWT签名）
            if (!validateToken(token)) {
                logger.warn("traceId={}, Invalid token for path: {}",
                        TraceIdUtil.getTraceId(), path);
                return handleAuthenticationError(exchange, "Invalid or expired token");
            }

            // Token验证成功，继续处理请求
            logger.debug("traceId={}, Authentication successful for path: {}",
                    TraceIdUtil.getTraceId(), path);

            // 将用户信息添加到请求属性中，供下游过滤器使用
            exchange.getAttributes().put("userId", "user123");
            exchange.getAttributes().put("token", token);

            return chain.filter(exchange);

        } catch (Exception e) {
            logger.error("traceId={}, Authentication filter error: {}",
                    TraceIdUtil.getTraceId(), e.getMessage(), e);
            return handleAuthenticationError(exchange, "Authentication failed");
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelist(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 简单的Token验证
     * 实际应该使用JWT库验证签名和过期时间
     */
    private boolean validateToken(String token) {
        // 简单检查Token长度和格式
        if (token == null || token.isEmpty() || token.length() < 20) {
            return false;
        }

        // 这里可以添加更复杂的JWT验证逻辑
        // 例如：验证签名、检查过期时间等
        // JwtUtils.validateToken(token);

        return true;
    }

    /**
     * 处理认证错误
     */
    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");

        String errorBody = String.format(
                "{\"code\":401,\"message\":\"%s\",\"traceId\":\"%s\"}",
                message, TraceIdUtil.getTraceId()
        );

        return exchange.getResponse().writeWith(
                reactor.core.publisher.Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    /**
     * 设置过滤器优先级（在日志过滤器之后执行）
     */
    @Override
    public int getOrder() {
        return -99;
    }
}

