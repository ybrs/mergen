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

import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class PubSubCommands extends Controller {
	
	@RedisCommand(cmd = "IDENTIFY", returns="OK")
	public void identify(MessageEvent e, Object[] args) {
		String v = new String((byte[]) args[1]);
		this.base.clientIdentifier = v;
	}
	
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

	        this.base.subscribedchannels.add(channelname);
	        
			IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-"+channelname);

			kvstore.set(this.base.getClientName(), this.base.clientIdentifier, 0, TimeUnit.SECONDS);

	        mr.addString("subscribe");
	        mr.addString(channelname);
	        // publish this to events channel
	        this.base.publish("HZ-EVENTS", "{'eventtype':'subscribed', 'channel':'"+channelname+"', " +
	        				  "'clientname':'"+this.base.getClientName()+"'," +
	        			      "'identifier':'"+this.base.clientIdentifier + "'}");
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
	        
	        // remove from hash
			IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-"+channelname);
			String localhostname;
			try {
				localhostname = java.net.InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				localhostname = "unknown";
			}
			kvstore.remove(localhostname + "-" + this.base.getIdentifier());
			
	        this.base.publish("HZ-EVENTS", "{'eventtype':'unsubscribed', " +
	        		"'channel':'"+channelname+"', " +
	        		",'clientname':'"+this.base.getClientName()+"'," +
      			  "'identifier':'"+this.base.clientIdentifier + "'}");
			
		}				
		mr.addInt(this.base.subscriptioncnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
		
		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));		
	}
	
	
	@RedisCommand(cmd = "SUBSCRIBERS")
	public void showsubscribers(MessageEvent e, Object[] args) {
		Object object  = args[1];
		String channelname = new String((byte[]) object);
		
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-"+channelname);
		
		Set<String> keys = kvstore.keySet();

		for (String k: keys.toArray(new String[0])){
			mr.addString(k);
			mr.addString(kvstore.get(k));
		}
		
		mr.finish();
		e.getChannel().write(mr.getBuffer());
	}
		
	@RedisCommand(cmd = "PUBLISH", returns = "OK")
	public void publish(MessageEvent e, Object[] args) {
		String v = new String((byte[]) args[2]);
		String channelname = new String((byte[]) args[1]);
		this.base.publish(channelname, v);
	}

	
	
}
