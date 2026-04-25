package xyz.kip.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.JwtUtil;
import xyz.kip.gateway.util.RedisKeyUtil;
import xyz.kip.gateway.util.TraceIdUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Global auth filter for Spring Cloud Gateway.
 * - Validates Bearer token via JWT
 * - Confirms latest-token in Redis matches request token (single-login)
 * - Loads user context from Redis and enriches downstream headers
 */
@RefreshScope
@Component
@ConditionalOnClass(GlobalFilter.class)
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(GatewayAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HDR_USER_ID = "X-User-Id";
    private static final String HDR_USERNAME = "X-Username";
    private static final String HDR_USER_EMAIL = "X-User-Email";
    private static final String HDR_USER_PHONE = "X-User-Phone";
    private static final String HDR_USER_ROLES = "X-User-Roles";
    private static final List<String> BUILTIN_WHITELIST_PREFIXES = List.of(
            "/kip-auth/api/auth/login",
            "/kip-auth/api/auth/register",
            "/kip-auth/api/auth/health",
            "/kip-auth/actuator/"
    );

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final List<String> whitelistPaths;
    private final List<String> userWhitelist;

    public GatewayAuthFilter(
            JwtUtil jwtUtil,
            ReactiveStringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${gateway.auth.whitelist:/actuator/**,/gateway/**,/api/public/**,/api/auth/**,/api/auth/login,/api/auth/register,/api/auth/health}") String whitelistCsv,
            @Value("${gateway.auth.user-whitelist:}") String userWhitelistCsv
    ) {
        this.jwtUtil = jwtUtil;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.whitelistPaths = Arrays.stream(whitelistCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        this.userWhitelist = Arrays.stream(userWhitelistCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String path = uri.getPath();

        if (isWhitelisted(path)) {
            logger.debug("traceId={}, Path {} is whitelisted, skipping authentication", TraceIdUtil.getTraceId(), path);
            return chain.filter(exchange);
        }

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith(BEARER_PREFIX)) {
            logger.warn("traceId={}, Missing or invalid Authorization header", TraceIdUtil.getTraceId());
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = auth.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        String identity = jwtUtil.getUsernameFromToken(token);
        if (!StringUtils.hasText(userId)) {
            logger.warn("traceId={}, Token missing userId", TraceIdUtil.getTraceId());
            return unauthorized(exchange, "Token missing userId");
        }
        String userInfoKey = RedisKeyUtil.userInfoKey(userId);
        if (isUserWhitelisted(identity)) {
            return redis.opsForValue().get(userInfoKey)
                    .flatMap(cachedUserJson -> {
                        UserContext context = parseUserContext(cachedUserJson, userId, identity);
                        if (context == null) {
                            return unauthorized(exchange, "User session not found");
                        }
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header(HDR_USER_ID, safe(context.userId()))
                                .header(HDR_USERNAME, safe(context.identity()))
                                .header(HDR_USER_EMAIL, safe(context.email()))
                                .header(HDR_USER_PHONE, safe(context.phone()))
                                .header(HDR_USER_ROLES, safe(context.roleCodesCsv()))
                                .build();
                        return chain.filter(exchange.mutate().request(mutated).build());
                    })
                    .switchIfEmpty(unauthorized(exchange, "User session not found"));
        }

        String tokenKey = RedisKeyUtil.userTokenKey(userId);
        return redis.opsForValue().get(tokenKey)
                .flatMap(cachedToken -> {
                    String normalizedToken = normalizeCachedToken(cachedToken);
                    if (!StringUtils.hasText(normalizedToken) || !normalizedToken.equals(token)) {
                        return unauthorized(exchange, "Token revoked or not latest");
                    }
                    return redis.opsForValue().get(userInfoKey)
                            .flatMap(cachedUserJson -> {
                                UserContext context = parseUserContext(cachedUserJson, userId, identity);
                                if (context == null) {
                                    return unauthorized(exchange, "User session not found");
                                }
                                ServerHttpRequest mutated = exchange.getRequest().mutate()
                                        .header(HDR_USER_ID, safe(context.userId()))
                                        .header(HDR_USERNAME, safe(context.identity()))
                                        .header(HDR_USER_EMAIL, safe(context.email()))
                                        .header(HDR_USER_PHONE, safe(context.phone()))
                                        .header(HDR_USER_ROLES, safe(context.roleCodesCsv()))
                                        .build();
                                return chain.filter(exchange.mutate().request(mutated).build());
                            })
                            .switchIfEmpty(unauthorized(exchange, "User session not found"));
                })
                .switchIfEmpty(unauthorized(exchange, "Token revoked or not latest"));
    }

    private boolean isUserWhitelisted(String identity) {
        return StringUtils.hasText(identity) && userWhitelist.contains(identity);
    }

    private boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : BUILTIN_WHITELIST_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
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

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String body = "{\"success\":false,\"code\":401,\"message\":\"" + escape(message) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private String normalizeCachedToken(String cachedToken) {
        if (!StringUtils.hasText(cachedToken)) {
            return cachedToken;
        }
        String normalized = cachedToken.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private UserContext parseUserContext(String cachedUserJson, String fallbackUserId, String fallbackIdentity) {
        if (!StringUtils.hasText(cachedUserJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(cachedUserJson);
            String userId = firstText(root, "userId", fallbackUserId);
            if (!StringUtils.hasText(userId)) {
                return null;
            }
            String email = firstText(root, "email", null);
            String phone = firstText(root, "phone", null);
            return new UserContext(
                    userId,
                    resolveIdentity(email, phone, fallbackIdentity),
                    email,
                    phone,
                    roleCodesCsv(root)
            );
        } catch (Exception e) {
            logger.warn("traceId={}, Failed to parse cached user info", TraceIdUtil.getTraceId(), e);
            return null;
        }
    }

    private String resolveIdentity(String email, String phone, String fallbackIdentity) {
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        return fallbackIdentity;
    }

    private String firstText(JsonNode root, String fieldName, String fallback) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return fallback;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String roleCodesCsv(JsonNode root) {
        return String.join(",", extractRoleCodes(root.get("roleCodes")));
    }

    private List<String> extractRoleCodes(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (!StringUtils.hasText(value)) {
                return List.of();
            }
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        if (node.isArray()) {
            if (looksLikeTypedCollectionWrapper(node)) {
                return extractRoleCodes(node.get(1));
            }
            List<String> values = new ArrayList<>();
            node.forEach(item -> values.addAll(extractRoleCodes(item)));
            return values;
        }
        return List.of();
    }

    private boolean looksLikeTypedCollectionWrapper(JsonNode node) {
        return node.size() == 2
                && node.get(0).isTextual()
                && node.get(1).isArray()
                && node.get(0).asText().contains(".");
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record UserContext(String userId, String identity, String email, String phone, String roleCodesCsv) {
    }
}
