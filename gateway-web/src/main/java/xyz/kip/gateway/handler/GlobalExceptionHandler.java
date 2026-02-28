package xyz.kip.gateway.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.exception.GatewayException;
import xyz.kip.gateway.util.TraceIdUtil;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理
 * 捕获网关中发生的所有异常
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 设置响应类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 默认错误码和信息
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = "Internal server error";
        String traceId = TraceIdUtil.getTraceId();

        // 处理不同类型的异常
        if (ex instanceof GatewayException) {
            GatewayException gatewayEx = (GatewayException) ex;
            statusCode = gatewayEx.getCode() != null ? gatewayEx.getCode() : 500;
            message = gatewayEx.getMessage();
            response.setStatusCode(HttpStatus.valueOf(statusCode));
        } else if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            org.springframework.web.server.ResponseStatusException statusEx =
                    (org.springframework.web.server.ResponseStatusException) ex;
            // 使用状态码的数字值
            statusCode = 500;
            String reason = statusEx.getReason();
            message = reason != null ? reason : "Request failed";
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            log.error("traceId={}, Unexpected error occurred: {}", traceId, ex.getMessage(), ex);
        }

        // 构建错误响应JSON
        String errorJson = buildErrorJson(statusCode, message, traceId);

        DataBufferFactory bufferFactory = response.bufferFactory();
        return response.writeWith(Mono.just(bufferFactory.wrap(errorJson.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * 构建错误响应JSON
     */
    private String buildErrorJson(int code, String message, String traceId) {
        return String.format(
                "{\"code\":%d,\"message\":\"%s\",\"traceId\":\"%s\",\"timestamp\":%d}",
                code,
                escapeJson(message),
                traceId,
                System.currentTimeMillis()
        );
    }

    /**
     * 转义JSON特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

