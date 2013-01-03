package com.github.nedis.codec;

import com.github.nedis.RedisCommandInterruptedException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: roger
 * Date: 12-3-15 11:16
 */
public class Command {
    private static final byte[] CRLF = "\r\n".getBytes();

    private CommandType commandType;
    private CountDownLatch latch;

    private CommandArgs commandArgs;
    private ChannelBuffer buffer;

    private Reply reply;
    public Command(CommandType type, CommandArgs commandArgs) {
        this.commandType = type;

        this.commandArgs = commandArgs;
        this.latch  = new CountDownLatch(1);
        this.buffer = ChannelBuffers.dynamicBuffer();

        this.encode();
    }

    public void complete() {
        latch.countDown();
    }


    private void encode() {
        buffer.writeByte('*');
        writeInt(1 +  (commandArgs != null ? commandArgs.count() : 0));
        buffer.writeBytes(CRLF);
        buffer.writeByte('$');
        writeInt(commandType.value.length);
        buffer.writeBytes(CRLF);
        buffer.writeBytes(commandType.value);
        buffer.writeBytes(CRLF);
        if(commandArgs != null) {
            buffer.writeBytes(commandArgs.buffer());
        }
    }

    public ChannelBuffer buffer() {
        return buffer;
    }


    private void writeInt(int length) {
        buffer.writeBytes(String.valueOf(length).getBytes());
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(commandType.name());
        return sb.toString();
    }


    public void setReply(Reply reply) {
        this.reply = reply;
    }
    
    public Reply getOutput() {

        return reply;
    }


    public boolean await(long timeout, TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }
    }

    public boolean cancel(boolean ignored) {
        boolean cancelled = false;
        if (latch.getCount() == 1) {
            latch.countDown();
            reply = null;
            cancelled = true;
        }
        return cancelled;
    }

}
