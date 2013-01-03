package com.github.nedis.codec;

import com.github.nedis.RedisException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;

import java.io.IOException;
import java.util.LinkedList;



/**
 * User: roger
 * Date: 12-3-15 11:11
 */
public class RedisDecoder extends ReplayingDecoder {


    private static final byte CR = 13;
    private static final byte LF = 10;
    private static final char ZERO = '0';


    private MultiBulkReply multiReply;


    public RedisDecoder() {

    }


    @Override

    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer buffer, Enum anEnum) throws Exception {
        if(multiReply != null) {

            multiReply.read(this, buffer);

            Reply ret = multiReply;
            multiReply = null;
            return ret;
        }
        byte marker = buffer.readByte();
        switch (marker) {
            case StatusReply.MARKER: {

                return new StatusReply(readLine(buffer));
            }
            case ErrorReply.MARKER: {
                return new ErrorReply(readLine(buffer));
            }
            case IntegerReply.MARKER: {
                return new IntegerReply(readInteger(buffer));
            }
            case BulkReply.MARKER: {
                return new BulkReply(readBytes(buffer));
            }
            case MultiBulkReply.MARKER: {
                return decodeMultiBulkReply(buffer);
            }
            default: {
                throw new IOException("Unexpected character in stream: " + marker);
            }
        }
    }

    public static int readInteger(ChannelBuffer is)  {
        int size = 0;
        int sign = 1;
        int read = is.readByte();
        if (read == '-') {
            read = is.readByte();
            sign = -1;
        }
        do {
            if (read == CR) {
                if (is.readByte() == LF) {
                    break;
                }
            }
            int value = read - ZERO;
            if (value >= 0 && value < 10) {
                size *= 10;
                size += value;
            } else {
                throw new RedisException("Invalid character in integer");
            }
            read = is.readByte();
        } while (true);

        return size * sign;
    }

    public static String readLine(ChannelBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            byte nextByte = buffer.readByte();
            if (nextByte == CR) {
                nextByte = buffer.readByte();
                if (nextByte == LF) {
                    return sb.toString();
                }
            } else {
                sb.append((char) nextByte);
            }

        }
    }


    public static byte[] readBytes(ChannelBuffer is) {
        int size = readInteger(is);
        if (size == -1) {
            return null;
        }

        byte[] bytes = new byte[size];
        is.readBytes(bytes, 0, size);
        int cr = is.readByte();
        int lf = is.readByte();
        if (cr != CR || lf != LF) {
            throw new RedisException("Improper line ending: " + cr + ", " + lf);
        }
        return bytes;
    }


    private MultiBulkReply decodeMultiBulkReply(ChannelBuffer is) throws IOException {
        if (multiReply == null) {
            multiReply = new MultiBulkReply();
        }
        multiReply.read(this, is);

        MultiBulkReply ret = multiReply;
        multiReply = null;
        return ret;
    }


    public void checkpoint() {
        super.checkpoint();
    }
}
