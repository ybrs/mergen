package com.github.mergen.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

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
	public void subscribe(MessageEvent e, Object[] args) {
		Controller controller = this.base.getPubSubList().get(this.base.getIdentifier());		
		if (controller == null) {			
			this.base.getPubSubList().put(this.base.getIdentifier(), this);
		}					
		
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();
		
		for (int i = 1; i < args.length; i++) {
			Object object  = args[i];
			String channelname = new String((byte[]) object);
			this.base.subscriptioncnt = this.base.subscriptioncnt + 1;
	        ITopic topic = this.base.client.getTopic(channelname);
	        topic.addMessageListener(this.base);
	        mr.addString("subscribe");
	        mr.addString(channelname);
		}				
		mr.addInt(this.base.subscriptioncnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));		
	}

	@RedisCommand(cmd = "UNSUBSCRIBE")
	public void unsubscribe(MessageEvent e, Object[] args) {
		Controller controller = this.base.getPubSubList().get(this.base.getIdentifier());		
		if (controller == null) {			
			this.base.getPubSubList().put(this.base.getIdentifier(), this);
		}					
		
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();
		
		for (int i = 1; i < args.length; i++) {
			Object object  = args[i];
			String channelname = new String((byte[]) object);
			
			this.base.subscriptioncnt = this.base.subscriptioncnt - 1;
			if (this.base.subscriptioncnt<0){ this.base.subscriptioncnt = 0; }
	        			
			ITopic topic = this.base.client.getTopic(channelname);
	        topic.removeMessageListener(this.base);	        
	        
	        mr.addString("unsubscribe");
	        mr.addString(channelname);
		}				
		mr.addInt(this.base.subscriptioncnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
		
		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));		
	}
	
	
	@RedisCommand(cmd = "SUBSCRIBERS", returns = "OK")
	public void showsubscribers(MessageEvent e, Object[] args) {
		System.out.println("subscribers are: ...");
		for (String k : this.base.getPubSubList().keySet()) {
			System.out.println(">>>>" + k);
		}
	}
	
	@RedisCommand(cmd = "PUBLISH", returns = "OK")
	public void publish(MessageEvent e, Object[] args) {
		String v = new String((byte[]) args[2]);
		String channelname = new String((byte[]) args[1]);
        ITopic topic = this.base.client.getTopic(channelname);
        TopicMessage msg = new TopicMessage(v, this.base.getIdentifier(), "message", channelname);
        topic.publish(msg);        

	}

	
	
}
