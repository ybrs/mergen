package com.github.nedis.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * User: roger
 * Date: 12-3-15
 */
public class CommandArgs {
    private static final byte[] CRLF = "\r\n".getBytes();

    private ChannelBuffer buffer;
    private int count;
    public CommandArgs() {
        this.buffer = ChannelBuffers.dynamicBuffer();
    }

    public CommandArgs addKey(String key) {
        write(key.getBytes());
        return this;
    }

    public CommandArgs add(String v) {
        write(v.getBytes());
        return this;
    }

    public CommandArgs add(int v) {
        buffer.writeByte(':');
        writeInt(v);
        buffer.writeBytes(CRLF);
        return this;
    }

    public CommandArgs addKeys(String...keys) {
        for(String key : keys) {
            write(key.getBytes());
        }
        return this;
    }

    public CommandArgs addCommandType(CommandType commandType) {
        write(commandType.value);
        return this;
    }

    public CommandArgs addValue(String value) {
        write(value.getBytes());
        return  this;
    }

    public CommandArgs addValue(byte[] bytes) {
        write(bytes);
        return this;
    }

    private void write(byte[] bytes) {
        buffer.writeByte('$');
        writeInt(bytes.length);
        buffer.writeBytes(CRLF);
        buffer.writeBytes(bytes);
        buffer.writeBytes(CRLF);

        count++;
    }

    private void writeInt(int value) {
        buffer.writeBytes(String.valueOf(value).getBytes());
    }
    public ChannelBuffer buffer() {
        return buffer;
    }

    public int count() {
        return count;
    }
}
