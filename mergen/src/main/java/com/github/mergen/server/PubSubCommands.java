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
		
        ITopic topic = this.base.client.getTopic ("default");
        topic.addMessageListener(this.base);        
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
		
//		for (String key : this.base.getPubSubList().keySet()) {
//			Controller c = this.base.getPubSubList().get(key);
//			System.out.println("pushing to >>>>" + key);
//			ServerReply sr = new ServerReply();
//			ServerReply.MultiReply mr = sr.startMultiReply();
//			mr.addString("type");
//			mr.addString("pattern");
//			mr.addString("channel");
//			mr.addString(v);
//			mr.finish();
//			c.context.getChannel().write(mr.getBuffer());
//		}

		
        ITopic topic = this.base.client.getTopic ("default");
        TopicMessage msg = new TopicMessage(v, this.base.getIdentifier());
        topic.publish(msg);        

	}

}
