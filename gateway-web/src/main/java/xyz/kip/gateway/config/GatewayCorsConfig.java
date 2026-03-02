package xyz.kip.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * 网关CORS跨域配置
 *
 * 使用 WebFluxConfigurer 实现 Spring Cloud Gateway 的 CORS 配置
 * 完全显式化，避免使用任何隐式配置或 Lombok 注解
 *
 * @author xiaoshichuan
 * @version 2026-03-02
 */
@Configuration
public class GatewayCorsConfig implements WebFluxConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(GatewayCorsConfig.class);

    /**
     * 配置 CORS 跨域资源共享
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        logger.info("Initializing CORS configuration for Spring Cloud Gateway");

        // 配置所有路径的 CORS 规则
        registry.addMapping("/**")
                // 允许跨域的源
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://localhost:5173",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:8080",
                        "http://127.0.0.1:5173"
                )
                // 允许的 HTTP 方法
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS",
                        "HEAD"
                )
                // 允许的请求头
                .allowedHeaders("*")
                // 是否允许发送 Cookie/Authorization 等凭证
                .allowCredentials(true)
                // 预检请求的缓存有效期（秒）
                .maxAge(3600L)
                // 暴露给前端的响应头
                .exposedHeaders(
                        "X-Trace-Id",
                        "X-Total-Count",
                        "X-Page-Number",
                        "X-Page-Size",
                        "X-Request-Id",
                        "Content-Type",
                        "Content-Length"
                );

        logger.info("CORS configuration initialized successfully");
    }
}
