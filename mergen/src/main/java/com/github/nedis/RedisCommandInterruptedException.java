package com.github.nedis;

/**
 * User: roger
 * Date: 12-4-9 上午9:41
 */
public class RedisCommandInterruptedException extends RedisException {
    public RedisCommandInterruptedException(Throwable e) {
        super("Command interrupted", e);
    }
}
