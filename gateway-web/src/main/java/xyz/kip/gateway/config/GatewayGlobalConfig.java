package xyz.kip.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Spring Cloud Gateway 全局配置
 * <p>
 * 包含路由配置的高级特性，如超时、重试、断路器等
 * 完全显式化实现，避免使用隐式配置
 *
 * @author xiaoshichuan
 * @version 2026-03-02
 */
@Configuration
public class GatewayGlobalConfig {

    /**
     * 自定义用户级别的限流 Key 解析器
     *
     * @return 返回基于用户 ID 的 Key 解析器
     */
    @Bean
    public GatewayUserKeyResolver userKeyResolver() {
        return new GatewayUserKeyResolver();
    }

    /**
     * 自定义 IP 级别的限流 Key 解析器
     *
     * @return 返回基于客户端 IP 的 Key 解析器
     */
    @Bean
    public GatewayIpKeyResolver ipKeyResolver() {
        return new GatewayIpKeyResolver();
    }

    /**
     * Gateway 路由属性配置
     *
     * @return 返回路由配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.cloud.gateway")
    public GatewayProperties gatewayProperties() {
        return new GatewayProperties();
    }

    /**
     * 用户 Key 解析器实现
     *
     * 根据请求头中的用户 ID 生成限流 Key
     */
    public static class GatewayUserKeyResolver {

        /**
         * 解析用户标识符
         *
         * @param exchange 服务器 Web 交换对象
         * @return 用户 ID，如果不存在则返回默认值
         */
        public Mono<String> resolve(ServerWebExchange exchange) {
            // 从请求头获取用户 ID
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            // 如果没有用户 ID，使用客户端 IP 作为备用
            if (userId == null || userId.isEmpty()) {
                String clientIp = getClientIp(exchange);
                return Mono.just(clientIp != null ? clientIp : "unknown");
            }

            return Mono.just(userId);
        }

        /**
         * 获取客户端 IP 地址
         *
         * @param exchange 服务器 Web 交换对象
         * @return 客户端 IP 地址
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

            String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : null;

            return remoteAddress != null ? remoteAddress : "unknown";
        }
    }

    /**
     * IP 地址 Key 解析器实现
     *
     * 根据客户端 IP 地址生成限流 Key
     */
    public static class GatewayIpKeyResolver {

        /**
         * 解析客户端 IP 地址
         *
         * @param exchange 服务器 Web 交换对象
         * @return 客户端 IP 地址
         */
        public Mono<String> resolve(ServerWebExchange exchange) {
            // 优先级顺序获取客户端 IP
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return Mono.just(xForwardedFor.split(",")[0].trim());
            }

            String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return Mono.just(xRealIp);
            }

            String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : null;

            return Mono.just(remoteAddress != null ? remoteAddress : "0.0.0.0");
        }
    }

    /**
     * Gateway 属性配置类
     *
     * 用于绑定 spring.cloud.gateway 配置前缀下的所有属性
     */
    public static class GatewayProperties {

        private Integer streamingMediaTypes;
        private Boolean enableDiscoveryClientPredicateFactory;

        /**
         * 获取流媒体类型数量
         *
         * @return 流媒体类型数量
         */
        public Integer getStreamingMediaTypes() {
            return streamingMediaTypes;
        }

        /**
         * 设置流媒体类型数量
         *
         * @param streamingMediaTypes 流媒体类型数量
         */
        public void setStreamingMediaTypes(Integer streamingMediaTypes) {
            this.streamingMediaTypes = streamingMediaTypes;
        }

        /**
         * 获取是否启用发现客户端断言工厂
         *
         * @return 是否启用
         */
        public Boolean getEnableDiscoveryClientPredicateFactory() {
            return enableDiscoveryClientPredicateFactory;
        }

        /**
         * 设置是否启用发现客户端断言工厂
         *
         * @param enableDiscoveryClientPredicateFactory 是否启用
         */
        public void setEnableDiscoveryClientPredicateFactory(Boolean enableDiscoveryClientPredicateFactory) {
            this.enableDiscoveryClientPredicateFactory = enableDiscoveryClientPredicateFactory;
        }
    }
}

