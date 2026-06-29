package com.yzj.ai.exception;

/**
 * AI SDK 基础异常类
 */
public class AIException extends RuntimeException {

    private final int code;

    public AIException(String message) {
        super(message);
        this.code = -1;
    }

    public AIException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public AIException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}