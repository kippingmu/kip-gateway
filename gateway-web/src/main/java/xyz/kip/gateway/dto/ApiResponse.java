package xyz.kip.gateway.dto;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
public class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 响应码 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private Long timestamp;

    /** 请求追踪ID */
    private String traceId;

    // 无参构造器
    public ApiResponse() {
    }

    // 全参构造器
    public ApiResponse(Integer code, String message, T data, Long timestamp, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.traceId = traceId;
    }

    // Getter / Setter
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    // Builder implementation
    public static class Builder<T> {
        private Integer code;
        private String message;
        private T data;
        private Long timestamp;
        private String traceId;

        public Builder<T> code(Integer code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder<T> traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(code, message, data, timestamp, traceId);
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（默认500错误）
     */
    public static <T> ApiResponse<T> fail(String message) {
        return fail(500, message);
    }
}
