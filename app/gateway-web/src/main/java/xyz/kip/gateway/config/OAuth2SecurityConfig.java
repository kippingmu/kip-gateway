package xyz.kip.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.beans.factory.annotation.Value;

/**
 * OAuth2 安全配置
 * 支持 OAuth2 资源服务器和客户端模式
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@RefreshScope
@Configuration
@EnableWebFluxSecurity
public class OAuth2SecurityConfig {


    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${gateway.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    /**
     * 配置安全过滤链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (!oauth2Enabled) {
            http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
            return http.build();
        }

        http.authorizeExchange(exchanges -> exchanges
            // 放行健康检查和管理接口
            .pathMatchers("/actuator/**", "/gateway/**").permitAll()
            // 放行白名单路径
            .pathMatchers("/api/public/**", "/api/auth/**").permitAll()
            // 其他请求需要认证
            .anyExchange().authenticated()
        ).oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> {
                // 配置 JWT 解码器
            })
        );

        return http.build();
    }

    /**
     * JWT 解码器
     */
    @Bean
    @ConditionalOnProperty(name = "gateway.oauth2.enabled", havingValue = "true")
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
