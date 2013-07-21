package com.github.mergen.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IList;
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
import java.net.UnknownHostException;
import java.util.concurrent.*;

import com.github.nedis.codec.CommandArgs;

class BoundKey {
	public String map;
	public String key;
	
	public BoundKey(String map, String key){
		this.map = map;
		this.key = key;
	}
}

class Base implements MessageListener<TopicMessage> {
	/**
	 * we init this once for every connection, so you can use it like a session,
	 * see auth
	 * 
	 */
	public boolean authenticated = false;
	public HZClient client;
	private Map<String, Controller> pubsublist;
	private String identifier;
	public List<String> subscribedchannels;
	public String clientIdentifier;
	public int subscriptioncnt = 0;
	
	public String namespace = "";

	public Map<String, BoundKey> boundkeys;

	
	public void addBoundKey(String map, String key){
		if (this.boundkeys == null){
			this.boundkeys = new ConcurrentHashMap<String, BoundKey>();
		}
		
		String mkey = "map:::" + map + ":::" + key;
		if (!this.boundkeys.containsKey(mkey)){
			this.boundkeys.put(mkey, new BoundKey(map, key));
		}
	}
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
		this.client.setNamespace(namespace);
	}

	public Base(HZClient client) {
		/**
		 * TODO: NOTE: this complicates lock ownership, all our clients are the
		 * lock owners since we are a proxy, imho, practically this doesnt
		 * change anything, revisit after pub/sub/topic...
		 **/
		this.client = client;
		this.subscribedchannels = new ArrayList<String>();
		this.clientIdentifier = UUID.randomUUID().toString();
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

	public void publish(String channelname, String message, String relay){
        ITopic topic = this.client.getTopic(channelname);
        TopicMessage msg = new TopicMessage(message, this.getIdentifier(), "message", relay);
        topic.publish(msg);
    }

	
	public void publish(String channelname, String message){		
        ITopic topic = this.client.getTopic(channelname);
        TopicMessage msg = new TopicMessage(message, this.getIdentifier(), "message", channelname);
        topic.publish(msg);
	}
	
	/*
	 * get internal name for client
	 */
	public String getClientName(){
		String localhostname;
		try {
			localhostname = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			localhostname = "unknown";
		}
		String clientname = localhostname + "-" + this.getIdentifier();
		return clientname;
	}

	
	public void removeAllListeners(){
		/*
		 * this only works on disconnect
		 */
		for (Object k: this.subscribedchannels.toArray()){
			System.out.println("Disconnected - removing listener " + k.toString());
	        ITopic topic = this.client.getTopic(k.toString());
	        topic.removeMessageListener(this);
	        //
			IMap<String, String> kvstore = this.client.getMap("HZ-SUBSCRIBERS-"+k);
			kvstore.remove(this.getClientName());
			
			PubSubChannel chan = getChannelProps(k.toString());
			chan.removeClient(getUniqueChannelName());
			saveChanProps(k.toString(), chan);
			
	        this.publish("HZ-EVENTS", "{'eventtype':'disconnect', " +
	        		"'channel':'"+k+"', " +
	        		"'clientname':'"+this.getClientName()+"'," +
	      			  "'identifier':'"+this.clientIdentifier + "'}");
		}
		
		if (this.boundkeys != null){
			for (BoundKey b: this.boundkeys.values()){
				System.out.println("removing " + b.map + " : " + b.key);
				IMap<String, String> hzmap = this.client.getMap(b.map);
				hzmap.remove(b.key);
			}
		}
		
	}
	
	@Override
	public void onMessage(Message<TopicMessage> msg) {
		System.out.println("message received - " + msg);
		try {
			TopicMessage tm = msg.getMessageObject();
			Controller c = this.pubsublist.get(this.getIdentifier());
			/** these are just fallbacks, if anything gets wrong, never works though **/
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
	
	
	public PubSubChannel getChannelProps(String channelname){
		IMap<String, PubSubChannel> kvstore = this.client.getMap("HZ-CHANNELS");
		PubSubChannel chan = kvstore.get(channelname);
		if (chan == null){
			chan = new PubSubChannel();
			chan.channelname = channelname;
		}
		return chan;
	}
	
	public void saveChanProps(String channelname, PubSubChannel chan){
		IMap<String, PubSubChannel> kvstore = this.client.getMap("HZ-CHANNELS");
		kvstore.set(channelname, chan, 0, TimeUnit.SECONDS);
	}


	// hopefully returns a unique channel name for this client.
	public String getUniqueChannelName() {
		return "HZ-INTERNAL-"+this.clientIdentifier;
	}
}