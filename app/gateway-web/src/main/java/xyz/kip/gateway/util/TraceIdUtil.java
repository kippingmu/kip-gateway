package xyz.kip.gateway.util;

import java.util.UUID;

/**
 * 链路ID生成和传递工具
 * 用于追踪完整的请求链路
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
public class TraceIdUtil {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 生成新的TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 设置TraceId
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 获取当前TraceId
     */
    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null) {
            traceId = generateTraceId();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    /**
     * 清理TraceId
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}

