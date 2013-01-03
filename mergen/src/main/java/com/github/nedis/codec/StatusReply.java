package com.github.nedis.codec;

/**
 * User: roger
 * Date: 12-3-31 下午2:01
 */
public class StatusReply extends Reply<String> {
    public static final char MARKER = '+';
    public StatusReply(String value) {
        super(MARKER, value);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("status reply:").append(value);
        return sb.toString();
    }
}
