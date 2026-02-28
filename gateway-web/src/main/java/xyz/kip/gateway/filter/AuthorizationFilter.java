package xyz.kip.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.util.Arrays;
import java.util.List;

/**
 * 权限检查全局过滤器
 * 验证用户是否有访问指定资源的权限
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Slf4j
@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {

    /**
     * 权限检查规则（简化示例）
     * 实际应该从配置中心或数据库加载
     */
    private static final List<String> ADMIN_PATHS = Arrays.asList(
            "/api/admin",
            "/api/system",
            "/api/config"
    );

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/user/profile",
            "/api/product"
    );

    /**
     * 执行权限检查过滤器
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String userId = (String) exchange.getAttributes().get("userId");

        // 如果没有userId，说明认证未通过（已在前置过滤器处理）
        if (userId == null) {
            log.debug("traceId={}, Skipping authorization check for path: {}",
                    TraceIdUtil.getTraceId(), path);
            return chain.filter(exchange);
        }

        try {
            // 检查权限
            if (!hasPermission(userId, path)) {
                log.warn("traceId={}, User {} has no permission for path: {}",
                        TraceIdUtil.getTraceId(), userId, path);
                return handleAuthorizationError(exchange, "You don't have permission to access this resource");
            }

            log.debug("traceId={}, User {} authorized for path: {}",
                    TraceIdUtil.getTraceId(), userId, path);

            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("traceId={}, Authorization filter error: {}",
                    TraceIdUtil.getTraceId(), e.getMessage(), e);
            return handleAuthorizationError(exchange, "Authorization check failed");
        }
    }

    /**
     * 检查用户是否有访问权限
     * 这里是简化的权限检查逻辑
     * 实际应该查询权限配置或数据库
     */
    private boolean hasPermission(String userId, String path) {
        // 简单的权限检查逻辑
        // 在实际应用中，应该从配置中心或数据库加载用户权限

        // Admin用户可以访问所有路径
        if ("admin".equals(userId)) {
            return true;
        }

        // 检查是否是公开路径
        if (isPublicPath(path)) {
            return true;
        }

        // 检查是否是受保护的路径
        if (isAdminPath(path)) {
            // 只有admin用户可以访问
            return "admin".equals(userId);
        }

        return true;
    }

    /**
     * 检查是否是公开路径
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 检查是否是受限路径（需要admin权限）
     */
    private boolean isAdminPath(String path) {
        return ADMIN_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 处理权限不足错误
     */
    private Mono<Void> handleAuthorizationError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");

        String errorBody = String.format(
                "{\"code\":403,\"message\":\"%s\",\"traceId\":\"%s\"}",
                message, TraceIdUtil.getTraceId()
        );

        return exchange.getResponse().writeWith(
                reactor.core.publisher.Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    /**
     * 设置过滤器优先级（在认证过滤器之后执行）
     */
    @Override
    public int getOrder() {
        return -98;
    }
}

