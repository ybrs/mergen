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

class Base {
	/**
	 * we init this once for every connection, so you can use it like a session,
	 * see auth
	 * 
	 */
	public boolean authenticated = false;
	public HazelcastInstance client;
	private Map<String, Set<ChannelHandlerContext>> pubsublist;
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

	public Map<String, Set<ChannelHandlerContext>> getPubSubList(){
		return this.pubsublist;
	}
	
	public void setPubSubList(Map<String, Set<ChannelHandlerContext>> subscriptions) {
		this.pubsublist = subscriptions;		
	}

	public void setIdentifier(int cnt){		
		this.identifier = Integer.toString(cnt);
	}
	
	public String getIdentifier() {
		return this.identifier;
	}

}