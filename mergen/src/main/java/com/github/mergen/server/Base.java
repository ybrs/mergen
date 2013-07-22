package com.github.mergen.server;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

class BoundKey implements Serializable {

	public String map;
	public String key;
	public String db;

	public BoundKey(String db, String map, String key) {
		this.map = map;
		this.key = key;
		this.db = db;
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
	public IList<String> subscribedchannels;
	public String clientIdentifier;
	public int subscriptioncnt = 0;
	public String namespace = "default";
	public IMap<String, BoundKey> boundkeys;

	public void addBoundKey(String map, String key) {
		if (this.boundkeys == null) {
			// these shouldn't be namespaced
			String boundKeysMapName = "HZ-BOUNDKEYS-"
					+ this.client.getClient().getCluster().getLocalMember().getUuid();
			
			System.out.println("boundkeymap - " + boundKeysMapName);

			this.boundkeys = this.client.getClient().getMap(boundKeysMapName);
			IMap<String, String> clusterLocks = this.client.getClient().getMap("HZ-CLUSTER-LOCK");
			clusterLocks.set(boundKeysMapName, "1", 0, TimeUnit.SECONDS);
		}

		String mkey = "map:::" + this.getNamespace() + ":::" + map + ":::" + key;
		if (!this.boundkeys.containsKey(mkey)) {
			this.boundkeys.put(mkey, new BoundKey(this.getNamespace(), map, key));
		}
	}

	public String getNamespace() {
		return this.namespace;
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
		this.subscribedchannels = this.client.getList("HZ-SUBSCRIBED-CHANNELS-"
				+ this.client.getCluster().getLocalMember().getUuid());
		this.clientIdentifier = UUID.randomUUID().toString();
		this.setNamespace("default");
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public Map<String, Controller> getPubSubList() {
		return this.pubsublist;
	}

	public void setPubSubList(Map<String, Controller> subscriptions) {
		this.pubsublist = subscriptions;
	}

	public void setIdentifier(long cnt) {
		this.identifier = Long.toString(cnt);
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public void publish(String channelname, String message, String relay) {
		ITopic topic = this.client.getTopic(channelname);
		TopicMessage msg = new TopicMessage(message, this.getIdentifier(),
				"message", relay);
		topic.publish(msg);
	}

	public void publish(String channelname, String message) {
		ITopic topic = this.client.getTopic(channelname);
		TopicMessage msg = new TopicMessage(message, this.getIdentifier(),
				"message", channelname);
		topic.publish(msg);
	}

	/*
	 * get internal name for client
	 */
	public String getClientName() {
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

	public void clientDisconnected(){
		System.out.println("Client Disconnected - starting to cleanup ");
		this.removeAllListeners();
		this.removeBoundKeys();
	}
	
	static public void sremoveBoundkeys(String uuid, HazelcastInstance client2){
		System.out.println("removing bound keys");
		// we dont want these maps to be namespaced.
		String boundKeysMapName = "HZ-BOUNDKEYS-" + uuid;
		IMap<String, BoundKey> boundkeys = client2.getMap(boundKeysMapName);
		IMap<String, String> clusterLocks = client2.getMap("HZ-CLUSTER-LOCK");
		
		if (clusterLocks.tryLock(boundKeysMapName)){
			System.out.println("acquired lock - " + boundKeysMapName);
			for (BoundKey b : boundkeys.values()) {
				System.out.println("removing " + b.db + ":" + b.map + " : " + b.key);
				System.out.println("keymapname [" + b.db + "::" + b.map + "]");
				IMap<String, String> hzmap = client2.getMap(b.db + "::" + b.map);
				System.out.println("keys::: ---- " + hzmap.keySet());
				hzmap.remove(b.key);
			}
			clusterLocks.unlock(boundKeysMapName);
		} else {
			System.out.println("couldnt acquire lock on - " + boundKeysMapName);
		}
	}
	
	public void removeBoundKeys(){
		System.out.println("remove bound keys");
		Base.sremoveBoundkeys(this.client.getCluster().getLocalMember().getUuid(), this.client.getClient());
	}
	
	public void removeAllListeners() {
		/*
		 * this only works on disconnect
		 */

		for (Object k : this.subscribedchannels) {
			System.out.println("Disconnected - removing listener "
					+ k.toString());
			ITopic topic = this.client.getTopic(k.toString());
			topic.removeMessageListener(this);
			//
			IMap<String, String> kvstore = this.client.getMap("HZ-SUBSCRIBERS-"
					+ k);
			kvstore.remove(this.getClientName());

			PubSubChannel chan = getChannelProps(k.toString());
			chan.removeClient(getUniqueChannelName());
			saveChanProps(k.toString(), chan);

			this.publish("HZ-EVENTS",
					"{'eventtype':'disconnect', " + "'channel':'" + k + "', "
							+ "'clientname':'" + this.getClientName() + "',"
							+ "'identifier':'" + this.clientIdentifier + "'}");
		}
	}

	@Override
	public void onMessage(Message<TopicMessage> msg) {
		System.out.println("message received - " + msg);
		try {
			TopicMessage tm = msg.getMessageObject();
			Controller c = this.pubsublist.get(this.getIdentifier());
			/**
			 * these are just fallbacks, if anything gets wrong, never works
			 * though
			 **/
			if (c == null) {
				System.out.println(this.getIdentifier()
						+ " is not connected anymore stop sending topics here");
				ITopic topic = this.client.getTopic(tm.getChannel());
				topic.removeMessageListener(this);
				return;
			}

			if (tm.getChannel() == null) {
				System.out.println(this.getIdentifier()
						+ " is not connected anymore removing and cleaning up");
				this.pubsublist.remove(this.getIdentifier());
				return;
			}

			if (c.context.getChannel() == null) {
				System.out.println(this.getIdentifier()
						+ " is not connected anymore removing and cleaning up");
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
			if (c.context.getChannel().isWritable()) {
				c.context.getChannel().write(mr.getBuffer());
			} else {
				System.out.println(this.getIdentifier()
						+ " is not connected anymore removing and cleaning up");
				this.pubsublist.remove(this.getIdentifier());
			}
		} catch (Exception e) {
			System.out.println("we have connection leak. - 1");
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public PubSubChannel getChannelProps(String channelname) {
		IMap<String, PubSubChannel> kvstore = this.client.getMap("HZ-CHANNELS");
		PubSubChannel chan = kvstore.get(channelname);
		if (chan == null) {
			chan = new PubSubChannel();
			chan.channelname = channelname;
		}
		return chan;
	}

	public void saveChanProps(String channelname, PubSubChannel chan) {
		IMap<String, PubSubChannel> kvstore = this.client.getMap("HZ-CHANNELS");
		kvstore.set(channelname, chan, 0, TimeUnit.SECONDS);
	}

	// hopefully returns a unique channel name for this client.
	public String getUniqueChannelName() {
		return "HZ-INTERNAL-" + this.clientIdentifier;
	}
}
