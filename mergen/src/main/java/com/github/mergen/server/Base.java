package com.github.mergen.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;
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

	public Base(HazelcastInstance client) {
		/**
		 * TODO: NOTE: this complicates lock ownership, all our clients are the
		 * lock owners since we are a proxy, imho, practically this doesnt
		 * change anything, revisit after pub/sub/topic...
		 **/
		
		System.out.println("new base...");
		
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
		System.out.println("message received " + msg);
		
		TopicMessage tm = msg.getMessageObject();
		Controller c = this.pubsublist.get(this.getIdentifier());
		System.out.println("pushing to >>>>" + this.getIdentifier());
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();
		mr.addString("type");
		mr.addString("pattern");
		mr.addString("channel");
		mr.addString(tm.getStr());
		mr.finish();
		c.context.getChannel().write(mr.getBuffer());
		
	}

	
}