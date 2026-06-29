package com.yzj.ai.exception;

/**
 * 限流异常 - 请求过于频繁
 */
public class RateLimitException extends AIException {

    public RateLimitException(String message) {
        super(429, message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(429, message, cause);
    }
}