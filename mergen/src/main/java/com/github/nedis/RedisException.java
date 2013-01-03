package com.github.nedis;

/**
 * User: roger
 * Date: 12-3-8 11:12
 */
public class RedisException extends RuntimeException {
    public RedisException(String msg) {
        super(msg);
    }

    public RedisException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
