package com.github.mergen.server;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.core.Transaction;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import java.util.*;
import java.io.Serializable;
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
	
	private String popWaitingMessage(String channelname){

		IList<String> list = this.base.client.getList("HZ-PSQUEUE-"+channelname);
		Transaction txn1 = base.client.getTransaction();

		String v = null;
		txn1.begin();
		try {			
			v = list.get(0);
			list.remove(0);			
		} catch (IndexOutOfBoundsException exc){
			// pass
		} finally {
			txn1.commit();
		}

		return v;
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
	        
			PubSubChannel chan = this.getChannelProps(channelname);
			chan.addClient(base.getClientName());
			
	        /**
	         * if we have waiting messages for this queue/channel
	         * deliver them
	         * 
	         * TODO: if its roundrobin ??
	         * 
	         */
	        if (channelname.endsWith("-reliable") || this.getChannelProps(channelname).mustDeliver){
		        String v;
		        while (true){
		        	v = this.popWaitingMessage(channelname);
		        	if (v==null){
		        		break;
		        	}
		        	this.base.publish(channelname, v);
		        }
	        }	        
		}
		
		mr.addInt(this.base.subscriptioncnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
		
		// when we have a subscriber, we attach a unique channel to it.
		// so we can roundrobin, send events etc.
        ITopic topic = this.base.client.getTopic(this.base.getUniqueChannelName());
        topic.addMessageListener(this.base);
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
			kvstore.remove(this.base.getClientName() + "-" + this.base.getIdentifier());
			
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

	private PubSubChannel getChannelProps(String channelname){
		IMap<String, PubSubChannel> kvstore = base.client.getMap("HZ-CHANNELS");
		PubSubChannel chan = kvstore.get(channelname);
		if (chan == null){
			chan = new PubSubChannel();
			chan.channelname = channelname;
		}
		return chan;
	}
	
	@RedisCommand(cmd="CHANNELPROPERTIES")
	public void channelProperties(MessageEvent e, Object[] args) {
		String channelname = new String((byte[]) args[1]);
		
		PubSubChannel chan = this.getChannelProps(channelname);
		
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();
		
		mr.addString("mustdeliver");
		if (chan.mustDeliver){
			mr.addString("true");
		} else {
			mr.addString("false");
		}
		mr.addString("requireack");
		if(chan.requireAcknowledgement){
			mr.addString("true");
		} else {
			mr.addString("false");
		}
		
		mr.addString("deliverymethod");
		mr.addString(chan.deliveryMethod);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
	}
	
	
	@RedisCommand(cmd="MODIFYCHANNEL", returns="OK")
	public void modifychannel(MessageEvent e, Object[] args) {
		String channelname = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);

		/**
		 * TODO: check arguments...
		 */	
		PubSubChannel chan = this.getChannelProps(channelname);
		if (k.equalsIgnoreCase("mustdeliver")){
			chan.mustDeliver = v.equalsIgnoreCase("true");
		} else if (k.equalsIgnoreCase("requireack")){
			chan.requireAcknowledgement = v.equalsIgnoreCase("true");
		} else if (k.equalsIgnoreCase("deliverymethod")){
			// TODO: check params
			chan.deliveryMethod = v;
		}
		
		saveChanProps(channelname, chan);
	}
	

	private void saveChanProps(String channelname, PubSubChannel chan){
		IMap<String, PubSubChannel> kvstore = base.client.getMap("HZ-CHANNELS");
		kvstore.set(channelname, chan, 0, TimeUnit.SECONDS);
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

		/**
		 * is it persistent or a normal pubsub message ?
		 * if channelname ends with -reliable then we store the message until
		 * it gets delivered.
		 * 
		 * if there is no one listening to channel, just save it and return
		 * dont care about the delivery method anymore
		 * 
		 */
		IMap<String, String> kvstore = this.base.client.getMap("HZ-SUBSCRIBERS-"+channelname);

		if (channelname.endsWith("-reliable") || this.getChannelProps(channelname).mustDeliver){
			// do we have any subscribers to this channel now.
			if (kvstore.size() == 0){
				// store the message until it gets delivered.
				IList<String> list = this.base.client.getList("HZ-PSQUEUE-"+channelname);
				list.add(v);
				return;
			}
		} 
		
		
		PubSubChannel chan = this.getChannelProps(channelname);
		if (chan.deliveryMethod.equals("broadcast")){
			this.base.publish(channelname, v);
		} else if (chan.deliveryMethod.equals("roundrobin")) {
			// if its round robin and we have X clients, relay the message to the next client
			System.out.println("next client - "+ chan.nextClient());
			// we have to save this everytime, so distributed round robin 
			// is a little bit hard work
			saveChanProps(channelname, chan);
		} else if (chan.deliveryMethod.equals("sticky")){
			// if its sticky, always relay the same origin to the same client 
		}
		

	}

	
	
}
