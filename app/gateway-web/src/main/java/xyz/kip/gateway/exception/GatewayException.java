package xyz.kip.gateway.exception;

/**
 * 网关业务异常
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
public class GatewayException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Integer code;

    public GatewayException(String message) {
        super(message);
        this.code = 500;
    }

    public GatewayException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public GatewayException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}

