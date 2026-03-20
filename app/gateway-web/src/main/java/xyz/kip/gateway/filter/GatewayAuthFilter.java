package xyz.kip.gateway.filter;

// Global auth filter: authentication and user context propagation

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.JwtUtil;
import xyz.kip.gateway.util.RedisKeyUtil;
import xyz.kip.gateway.util.TraceIdUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Global auth filter for Spring Cloud Gateway.
 * - Validates Bearer token via JWT
 * - Confirms latest-token in Redis matches request token (single-login)
 * - Enriches downstream headers with user context
 * <p>
 * Note: This bean is only activated when Spring Cloud Gateway is on classpath
 * (via @ConditionalOnClass). Place into your Gateway application to use.
 * @author xiaoshichuan
 */
@RefreshScope
@Component
@ConditionalOnClass(GlobalFilter.class)
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(GatewayAuthFilter.class);

    /**
     * Auth prefix and downstream user context header names
      */
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HDR_USER_ID = "X-User-Id";
    private static final String HDR_USERNAME = "X-Username";
    private static final String HDR_TENANT_ID = "X-Tenant-Id";

    /**
     * Dependencies: JWT utility and reactive Redis client
     */
    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redis;

    /**
     * Auth whitelist paths (bypass)
     */
    private final List<String> whitelistPaths;

    /**
     * Inject whitelist from configuration (comma-separated)
     * @param jwtUtil JWT utility
     * @param redis Redis client
     * @param whitelistCsv comma-separated list of paths
     */
    public GatewayAuthFilter(
            JwtUtil jwtUtil,
            ReactiveStringRedisTemplate redis,
            @Value("${gateway.auth.whitelist:/actuator/**,/gateway/**,/api/public/**,/api/auth/**}") String whitelistCsv
    ) {
        this.jwtUtil = jwtUtil;
        this.redis = redis;
        this.whitelistPaths = Arrays.stream(whitelistCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Filter execution order (smaller runs earlier).
     * @return the order value
     */
    @Override
    public int getOrder() {
        // Execute early, before business filters
        return -100;
    }

    /**
     * Main auth flow:
     *      whitelist pass-through,
     *      JWT validation,
     *      Redis latest-token check,
     *      and user headers propagation.
     * @param exchange current request exchange
     * @param chain filter chain
     * @return async completion signal
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Main flow: whitelist, JWT, Redis latest-token, propagate headers
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String path = uri.getPath();

        // Bypass if matched in whitelist
        if (isWhitelisted(path)) {
            logger.debug("traceId={}, Path {} is whitelisted, skipping authentication", TraceIdUtil.getTraceId(), path);
            return chain.filter(exchange);
        }

        // Read and validate Authorization header (must be Bearer)
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            logger.warn("traceId={}, Missing or invalid Authorization header", TraceIdUtil.getTraceId());
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Validate JWT signature and expiration
        String token = auth.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // Extract user and tenant info from token
        String userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String tenantId = extractClaim(token);
        if (userId == null) {
            logger.warn("traceId={}, Missing or invalid Authorization header", TraceIdUtil.getTraceId());
            return unauthorized(exchange, "Token missing userId");
        }

        // Ensure token matches the latest one in Redis (single-login)
        String tokenKey = RedisKeyUtil.userTokenKey(userId);
        return redis.opsForValue().get(tokenKey)
                .flatMap(cachedToken -> {
                    if (cachedToken == null || !cachedToken.equals(token)) {
                        return unauthorized(exchange, "Token revoked or not latest");
                    }
                    // Propagate user context to downstream services
                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header(HDR_USER_ID, userId)
                            .header(HDR_USERNAME, username != null ? username : "")
                            .header(HDR_TENANT_ID, tenantId != null ? tenantId : "")
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    /**
     * Check whether the request path matches the whitelist.
     * Supports prefixes like "/**", "/*" and exact matches.
     * @param path request path
     * @return true if whitelisted
     */
    private boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        for (String p : whitelistPaths) {
            if (p.equals("/*")) {
                return true;
            }
            if (p.endsWith("/**")) {
                String prefix = p.substring(0, p.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (p.endsWith("/*")) {
                String prefix = p.substring(0, p.length() - 2);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Write a 401 Unauthorized JSON response.
     * @param exchange current request exchange
     * @param message error message
     * @return async write completion
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String body = "{\"success\":false,\"code\":401,\"message\":\"" + escape(message) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * Escape backslashes and double quotes in a JSON string value.
     * @param s original string
     * @return escaped string
     */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Extract a claim string from JWT claims.
     *
     * @param token JWT token
     * @return claim value as string or null
     */
    private String extractClaim(String token) {
        var claims = jwtUtil.getAllClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object v = claims.get("tenantId");
        return v != null ? String.valueOf(v) : null;
    }
}
