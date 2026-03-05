package xyz.kip.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.util.TraceIdUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 请求日志记录全局过滤器
 * 记录所有进入网关的请求信息和响应信息
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Logger accessLog = LoggerFactory.getLogger("xyz.kip.gateway.filter.RequestLoggingFilter");

    /**
     * 执行过滤器
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 生成或获取 TraceId
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdUtil.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdUtil.generateTraceId();
        }
        TraceIdUtil.setTraceId(traceId);

        // 添加TraceId到响应头
        exchange.getResponse().getHeaders().add(TraceIdUtil.TRACE_ID_HEADER, traceId);

        // 记录请求信息
        long startTime = System.currentTimeMillis();
        final String clientIp = getClientIp(exchange);
        exchange.getRequest().getMethod();
        final String method = exchange.getRequest().getMethod().name();
        final String path = exchange.getRequest().getPath().value();
        final String queryString = exchange.getRequest().getQueryParams().isEmpty() ? "" : "?" + exchange.getRequest().getQueryParams();
        final String finalTraceId = traceId;

        logger.info("==> REQUEST START: traceId={}, method={}, path={}, clientIp={}",
                traceId, method, path, clientIp);

        // 访问日志
        accessLog.info("{} {} {} - {} {}",
                method, path, queryString, clientIp, traceId);

        // 继续执行过滤器链
        return chain.filter(exchange).doFinally(signalType -> {
            // 记录响应信息
            long duration = System.currentTimeMillis() - startTime;
            int status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 500;

            logger.info("<== REQUEST END: traceId={}, status={}, duration={}ms",
                    finalTraceId, status, duration);

            // 访问日志
            accessLog.info("{} {} {} - {} {} - {}ms",
                    method, path, queryString, clientIp, status, duration);

            // 清理ThreadLocal
            TraceIdUtil.clear();
        });
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // 从X-Forwarded-For头获取（代理情况）
        String xForwardedFor = headers.getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // 从X-Real-IP头获取（Nginx代理）
        String xRealIp = headers.getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // 从请求连接获取
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }

        return "UNKNOWN";
    }

    /**
     * 设置过滤器优先级（越小越先执行）
     */
    @Override
    public int getOrder() {
        return -100;
    }
}

