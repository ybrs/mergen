package com.github.nedis.codec;

/**
 * User: roger
 * Date: 12-3-31 下午2:05
 */
public class BulkReply extends Reply<byte[]> {
    public static final char MARKER = '$';
    public BulkReply(byte[] value) {
        super(MARKER, value);
    }

    public String getString() {
        return new String(value);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        return sb.toString();
    }
}
