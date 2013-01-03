package com.github.nedis.pubsub;

/**
 * User: roger
 * Date: 12-4-9 上午10:54
 */
public interface RedisListener {
    public void message(String channel, String message);
    public void message(String pattern, String channel, String message);
}
