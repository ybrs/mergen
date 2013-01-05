package com.github.mergen.server;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import java.security.SecureRandom;
import java.util.*;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import com.github.nedis.codec.MultiBulkReply;
import com.github.nedis.codec.*;


public class ServerHandler extends SimpleChannelUpstreamHandler {

    private final ChannelGroup channelGroup;    
    private HazelcastInstance client;

    
    private CommandDispatcher dispatcher;
    private Controller controller;
    public Base base;

    public ServerHandler(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;        
        // System.out.println(">>> handler");
    }

    public void setClient(HazelcastInstance client){
        this.client = client;        
        this.base = new Base(client);
    }

    public void setDispatcher(CommandDispatcher dispatcher){
        this.dispatcher = dispatcher; 
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.channelGroup.add(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof MultiBulkReply){
            Object[] args = ((MultiBulkReply) e.getMessage()).get();
            int size = ((MultiBulkReply) e.getMessage()).getSize();

            // which controller we need
            String cmd = new String((byte[])args[0]);
            Class klass = dispatcher.getClassForCommand(cmd);

            if (klass==null){
                ServerReply sr = new ServerReply();
                e.getChannel().write(sr.replyError("method not implemented"));
                return;
            }

            Constructor co = klass.getConstructor();
            controller = (Controller)co.newInstance();
            controller.base = base;

            if (size > 0){
                // System.out.println(args[0]);
                dispatcher.dispatch(controller, e, args);
            } else {
                // TODO: ??? 
                ServerReply sr = new ServerReply();
                e.getChannel().write(sr.replyOK());
            }
            // e.getChannel().write(replyOK());
        } else {
            ServerReply sr = new ServerReply();
            e.getChannel().write(sr.replyOK());
        }
    }
}
