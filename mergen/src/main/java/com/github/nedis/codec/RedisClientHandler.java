package com.github.nedis.codec;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * User: roger
 * Date: 12-3-15 11:11
 */
public class RedisClientHandler extends SimpleChannelHandler {

    private Logger logger = Logger.getLogger(getClass().getName());
    private BlockingQueue<Command> queue = new LinkedBlockingQueue<Command>();
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws java.lang.Exception {
        Command command = (Command)e.getMessage();
        Channels.write(ctx, e.getFuture(), command.buffer());
        queue.put(command);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws java.lang.Exception {

        Reply reply = (Reply)e.getMessage();

        Command command = queue.take();
        command.setReply(reply);
        command.complete();

        //logger.info("command:" + command + ",return " + reply);
    }

}
