package xyz.kip.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * OAuth2 认证过滤器
 * 验证 OAuth2 Access Token
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Component
@ConditionalOnProperty(name = "gateway.oauth2.enabled", havingValue = "true")
public class OAuth2AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFilter.class);

    @Value("${gateway.oauth2.whitelist:/actuator/**,/gateway/**,/api/public/**,/api/auth/**}")
    private String whitelist;

    private final ReactiveJwtDecoder jwtDecoder;

    public OAuth2AuthenticationFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 检查是否在白名单中
        if (isWhitelisted(path)) {
            logger.debug("traceId={}, Path {} is whitelisted, skipping OAuth2 authentication",
                    TraceIdUtil.getTraceId(), path);
            return chain.filter(exchange);
        }

        // 获取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("traceId={}, Missing or invalid Authorization header",
                    TraceIdUtil.getTraceId());
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // 验证 JWT Token
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    logger.debug("traceId={}, OAuth2 token validated successfully for subject: {}",
                            TraceIdUtil.getTraceId(), jwt.getSubject());

                    // 将用户信息添加到请求头
                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", jwt.getSubject())
                                    .header("X-User-Name", jwt.getClaimAsString("name"))
                                    .header("X-User-Email", jwt.getClaimAsString("email"))
                            )
                            .build();

                    return chain.filter(modifiedExchange);
                })
                .onErrorResume(e -> {
                    logger.error("traceId={}, OAuth2 token validation failed: {}",
                            TraceIdUtil.getTraceId(), e.getMessage());
                    return handleUnauthorized(exchange, "Invalid or expired token");
                });
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        Set<String> whitelistSet = parseWhitelist();
        for (String pattern : whitelistSet) {
            if (matchesPattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 路径匹配（支持通配符）
     */
    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    /**
     * 解析白名单配置
     */
    private Set<String> parseWhitelist() {
        if (whitelist == null || whitelist.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(whitelist.split(",")));
    }

    /**
     * 处理未授权请求
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");

        String errorBody = String.format(
                "{\"code\":401,\"message\":\"%s\",\"traceId\":\"%s\"}",
                message, TraceIdUtil.getTraceId()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -97; // 在 JWT 认证之后执行
    }
}
