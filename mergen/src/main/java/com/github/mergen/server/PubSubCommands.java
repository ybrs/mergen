package com.github.mergen.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import java.util.*;
import java.lang.annotation.*;
import java.util.concurrent.*;

import com.github.nedis.codec.CommandArgs;

import java.nio.charset.Charset;

public class PubSubCommands extends Controller {
	
	@RedisCommand(cmd = "SUBSCRIBE")
	public void hset(MessageEvent e, Object[] args) {

		Set<ChannelHandlerContext> contexts = this.base.getPubSubList().get(this.base.getIdentifier());		
		if (contexts == null) {
			contexts = new HashSet<ChannelHandlerContext>();
			this.base.getPubSubList().put(this.base.getIdentifier(), contexts);
		}					
		
		// do nothing for now...
		int listenercnt = 1;
		String channelname = new String((byte[]) args[1]);
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();
		mr.addString("subscribe");
		mr.addString(channelname);
		mr.addInt(listenercnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
		
		System.out.println("subscribers are: ...");
		for (String k : this.base.getPubSubList().keySet()) {
			System.out.println(">>>>" + k);
		}
		
	}


}
