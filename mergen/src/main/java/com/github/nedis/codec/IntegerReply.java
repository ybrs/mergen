package com.github.nedis.codec;

/**
 * User: roger
 * Date: 12-3-31 下午2:02
 */
public class IntegerReply extends Reply<Integer> {
    public static final char MARKER = ':';
    public IntegerReply(int value) {
        super(MARKER, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("integer reply").append(value);
        return sb.toString();
    }

}
