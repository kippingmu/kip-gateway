package xyz.kip.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.JwtUtil;
import xyz.kip.gateway.util.RedisKeyUtil;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayAuthFilterTest {

    @Test
    void shouldSkipAuthenticationForConfiguredWhitelistedPath() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login"));
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        verifyNoInteractions(jwtUtil, redis);
    }

    @Test
    void shouldSkipAuthenticationForK8sSameDomainAuthHealthPath() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health,/actuator/**", "");

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/health"));
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        verifyNoInteractions(jwtUtil, redis);
    }

    @Test
    void shouldSkipAuthenticationForDiscoveryAuthPath() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/kip-auth/api/auth/login"));
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        verifyNoInteractions(jwtUtil, redis);
    }

    @Test
    void shouldRejectRequestWithoutAuthorizationHeader() {
        GatewayAuthFilter filter = new GatewayAuthFilter(mock(JwtUtil.class), mock(ReactiveStringRedisTemplate.class), new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/app/demo/context"));

        filter.filter(exchange, value -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("Missing or invalid Authorization header"));
    }

    @Test
    void shouldPropagateUserContextFromQuotedRedisToken() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        Claims claims = mock(Claims.class);

        when(redis.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("jwt-token")).thenReturn("user-1");
        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("alice");
        when(jwtUtil.getAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(claims.get("tenantId")).thenReturn("default");
        when(valueOperations.get(RedisKeyUtil.userTokenKey("user-1"))).thenReturn(Mono.just("\"jwt-token\""));
        when(valueOperations.get(RedisKeyUtil.userInfoKey("user-1"))).thenReturn(Mono.just("{\"userId\":\"user-1\",\"username\":\"alice\",\"tenantId\":\"default\",\"email\":\"alice@example.com\",\"phone\":\"13900000001\",\"roleCodes\":[\"ADMIN\"]}"));

        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/app/demo/context")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token")
        );
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertEquals("user-1", headers.getFirst("X-User-Id"));
        assertEquals("alice", headers.getFirst("X-Username"));
        assertEquals("default", headers.getFirst("X-Tenant-Id"));
        assertEquals("alice@example.com", headers.getFirst("X-User-Email"));
        assertEquals("13900000001", headers.getFirst("X-User-Phone"));
        assertEquals("ADMIN", headers.getFirst("X-User-Roles"));
    }

    @Test
    void shouldPropagateAdminRoleFromTypedRedisCollectionWrapper() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        Claims claims = mock(Claims.class);

        when(redis.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("jwt-token")).thenReturn("571861813143015424");
        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("15884526909");
        when(jwtUtil.getAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(claims.get("tenantId")).thenReturn("default");
        when(valueOperations.get(RedisKeyUtil.userTokenKey("571861813143015424"))).thenReturn(Mono.just("\"jwt-token\""));
        when(valueOperations.get(RedisKeyUtil.userInfoKey("571861813143015424"))).thenReturn(Mono.just(
                "{\"@class\":\"xyz.kip.auth.service.model.UserAuthModel\",\"userId\":\"571861813143015424\",\"username\":\"15884526909\",\"email\":\"kipmu@kip.xyz\",\"phone\":\"15884526909\",\"tenantId\":\"default\",\"roleCodes\":[\"java.util.ImmutableCollections$List12\",[\"ADMIN\"]]}"
        ));

        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/app/admin/navigation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token")
        );
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        assertEquals("ADMIN", captured.get().getRequest().getHeaders().getFirst("X-User-Roles"));
    }

    @Test
    void shouldRejectRequestWhenCachedUserInfoMissing() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        Claims claims = mock(Claims.class);

        when(redis.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("jwt-token")).thenReturn("user-1");
        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("alice");
        when(jwtUtil.getAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(claims.get("tenantId")).thenReturn("default");
        when(valueOperations.get(RedisKeyUtil.userTokenKey("user-1"))).thenReturn(Mono.just("\"jwt-token\""));
        when(valueOperations.get(RedisKeyUtil.userInfoKey("user-1"))).thenReturn(Mono.empty());

        GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, redis, new ObjectMapper(), "/api/auth/login,/api/auth/register,/api/auth/health", "");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/app/demo/context")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token")
        );

        filter.filter(exchange, value -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        assertTrue(body.contains("User session not found"));
    }

    @Test
    void shouldAllowWhitelistedUserToBypassLatestTokenComparison() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        Claims claims = mock(Claims.class);

        when(redis.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.validateToken("jwt-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("jwt-token")).thenReturn("user-1");
        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("kipmu@kip.xyz");
        when(jwtUtil.getAllClaimsFromToken("jwt-token")).thenReturn(claims);
        when(claims.get("tenantId")).thenReturn("default");
        when(valueOperations.get(RedisKeyUtil.userInfoKey("user-1"))).thenReturn(Mono.just("{\"userId\":\"user-1\",\"username\":\"kipmu@kip.xyz\",\"tenantId\":\"default\",\"email\":\"kipmu@kip.xyz\",\"phone\":\"13900000001\"}"));

        GatewayAuthFilter filter = new GatewayAuthFilter(
                jwtUtil,
                redis,
                new ObjectMapper(),
                "/api/auth/login,/api/auth/register,/api/auth/health",
                "kipmu@kip.xyz"
        );
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/kip/translate/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token")
        );
        GatewayFilterChain chain = value -> {
            captured.set(value);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(captured.get());
        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertEquals("user-1", headers.getFirst("X-User-Id"));
        assertEquals("kipmu@kip.xyz", headers.getFirst("X-Username"));
    }
}
