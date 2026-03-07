package xyz.kip.gateway.config;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪配置
 * 集成 Micrometer Tracing + Zipkin
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    private static final Logger logger = LoggerFactory.getLogger(TracingConfig.class);

    /**
     * 自定义追踪过滤器，添加额外的 Span 标签
     */
    @Bean
    public GlobalFilter tracingEnhancementFilter(Tracer tracer) {
        return (exchange, chain) -> {
            var span = tracer.currentSpan();
            if (span != null) {
                // 添加自定义标签
                span.tag("gateway.path", exchange.getRequest().getPath().value());
                span.tag("gateway.method", exchange.getRequest().getMethod().name());
                span.tag("gateway.remote-addr",
                    exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown");

                // 添加路由信息
                exchange.getAttributes().forEach((key, value) -> {
                    if (key.contains("route")) {
                        span.tag("gateway." + key, String.valueOf(value));
                    }
                });
            }

            return chain.filter(exchange)
                .doOnSuccess(v -> {
                    if (span != null) {
                        span.tag("gateway.status", String.valueOf(exchange.getResponse().getStatusCode()));
                    }
                })
                .doOnError(error -> {
                    if (span != null) {
                        span.tag("gateway.error", error.getClass().getSimpleName());
                        span.tag("gateway.error.message", error.getMessage());
                    }
                });
        };
    }
}
