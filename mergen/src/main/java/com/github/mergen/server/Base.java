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

class Base implements MessageListener<TopicMessage> {
	/**
	 * we init this once for every connection, so you can use it like a session,
	 * see auth
	 * 
	 */
	public boolean authenticated = false;
	public HazelcastInstance client;
	private Map<String, Controller> pubsublist;
	private String identifier;
	public int subscriptioncnt = 0;

	public Base(HazelcastInstance client) {
		/**
		 * TODO: NOTE: this complicates lock ownership, all our clients are the
		 * lock owners since we are a proxy, imho, practically this doesnt
		 * change anything, revisit after pub/sub/topic...
		 **/
		this.client = client;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public Map<String, Controller> getPubSubList(){
		return this.pubsublist;
	}
	
	public void setPubSubList(Map<String, Controller> subscriptions) {
		this.pubsublist = subscriptions;		
	}

	public void setIdentifier(long cnt){		
		this.identifier = Long.toString(cnt);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public void onMessage(Message<TopicMessage> msg) {
		System.out.println("message received - " + msg);
		try {
			TopicMessage tm = msg.getMessageObject();
			Controller c = this.pubsublist.get(this.getIdentifier());
			
			if (c==null){
				System.out.println(this.getIdentifier() + " is not connected anymore stop sending topics here");
				
		        ITopic topic = this.client.getTopic(tm.getChannel());
		        topic.removeMessageListener(this);
		
				return;
			}
			
			if (tm.getChannel() == null){
				System.out.println(this.getIdentifier() + " is not connected anymore removing and cleaning up");
				this.pubsublist.remove(this.getIdentifier());
				return;
			}
			
			if (c.context.getChannel() == null){
				System.out.println(this.getIdentifier() + " is not connected anymore removing and cleaning up");
				this.pubsublist.remove(this.getIdentifier());
				return;
			}
			
			
			System.out.println("pushing to >>>>" + this.getIdentifier());

			ServerReply sr = new ServerReply();
			ServerReply.MultiReply mr = sr.startMultiReply();
			mr.addString("message");
			mr.addString(tm.getChannel());
			mr.addString(tm.getStr());
			mr.finish();
			if (c.context.getChannel().isWritable()){
				c.context.getChannel().write(mr.getBuffer());	
			} else {
				System.out.println(this.getIdentifier() + " is not connected anymore removing and cleaning up");
				this.pubsublist.remove(this.getIdentifier());
			}
		} catch (Exception e){
			System.out.println("we have connection leak. - 1");
			System.out.println(e);
			e.printStackTrace();
		}
	}

	
}