package com.github.nedis.codec;


import com.github.nedis.NettyRedisClientImpl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * User: roger
 * Date: 12-3-7 11:20
 */
public class AutoReconnectHandler extends SimpleChannelUpstreamHandler implements TimerTask {
    private final static Logger logger = Logger.getLogger(AutoReconnectHandler.class.getName());
    private NettyRedisClientImpl redisImpl;
    private Channel channel;
    private ChannelGroup channels;
    private Timer timer;

    private int attempts;

    public AutoReconnectHandler(NettyRedisClientImpl redisImpl, ChannelGroup channels, Timer timer) {
        this.redisImpl = redisImpl;
        this.channels  = channels;
        this.timer     = timer;
    }

    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = ctx.getChannel();
        channels.add(channel);
        attempts = 0;
        ctx.sendUpstream(e);
    }

    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

        if (attempts < 12) attempts++;
        int timeout = 2 << attempts;
        logger.info("after " + timeout + " milliseconds will reconnect");
        timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.fine("exception : " + e);
        ctx.getChannel().close();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        logger.info("try to reconnect");
        redisImpl.reconnect();
    }
}
