package com.github.nedis.codec;

import com.github.nedis.RedisException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import static com.github.nedis.codec.StmDecoder.State.*;
import static com.github.nedis.codec.StmDecoder.*;
import static com.github.nedis.codec.StmDecoder.State.READ_ERROR;

/**
 * User: roger
 * Date: 12-3-31 下午4:10
 */
public class StmDecoder extends ReplayingDecoder<State> {

    private static final byte CR = 13;
    private static final byte LF = 10;
    private static final char ZERO = '0';

    public static enum State {
        READ_INITIAL,
        READ_INTEGER,
        READ_STATUS,
        READ_ERROR,
        READ_BULK,
        READ_BULK_CONTENT,
        READ_MULTIBULK,
        READ_MULTIBULK_CONTENT


    }

    private Reply reply;
    private MultiBulkReply multiReply;
    private int num;
    private int size;

    private Object reset() {
        checkpoint(READ_INITIAL);
        return reply;
    }

    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer buffer, State state) throws Exception {
        if(multiReply != null) {
            multiReply.read(null, buffer);
            Reply ret = multiReply;
            multiReply = null;
            return ret;
        }

        if(state == READ_INITIAL) {
            byte code = buffer.readByte();
            state = getReplyType(code);
            checkpoint(state);

        }

        switch (state) {
            case READ_ERROR : {
                reply = new ErrorReply(readLine(buffer));
                return reset();
            }

            case READ_STATUS: {
                reply = new StatusReply(readLine(buffer));
                return reset();
            }

            case READ_INTEGER: {
                reply = new IntegerReply(readInteger(buffer));
                return reset();
            }

            case READ_BULK: {
                //reply = new BulkReply(readInteger(buffer));
                checkpoint(READ_BULK_CONTENT);
            }

            case READ_BULK_CONTENT: {
                BulkReply bulkReply = (BulkReply)reply;
                //bulkReply.set(readBytes(buffer, bulkReply.getSize()));
                return reset();
            }

            case READ_MULTIBULK: {
                multiReply = new MultiBulkReply(readInteger(buffer));
                checkpoint(READ_MULTIBULK_CONTENT);
            }

            case READ_MULTIBULK_CONTENT: {


            }
        }

        return null;
    }

    public static byte[] readBytes(ChannelBuffer is,int size) {

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

    
    private State getReplyType(byte code) {

        switch (code) {
            case '+': return READ_STATUS;
            case '-': return READ_ERROR;
            case '$': return READ_BULK;
            case ':': return READ_INTEGER;
            case '*': return READ_MULTIBULK;
            default:  throw new RedisException("unknown redis response type code: "+code);
        }
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

}
