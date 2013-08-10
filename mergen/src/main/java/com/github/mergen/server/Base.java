package com.github.mergen.server;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
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

class SubscribedChannel implements Serializable {
	public String db;
	public String channelName;
	public String clientName;
	public String uniqueChannelName;
	public String clientIdentifier;

	public SubscribedChannel(String db, String channelName, String clientName,
			String uniqueChannelName, String clientIdentifier) {
		this.db = db;
		this.channelName = channelName;
		this.clientName = clientName;
		this.uniqueChannelName = uniqueChannelName;
		this.clientIdentifier = clientIdentifier;
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
	public IList<SubscribedChannel> subscribedchannels;
	public List<SubscribedChannel> clientSubscribedChannels;
	public String clientIdentifier;
	public int subscriptioncnt = 0;
	public String namespace = "default";
	public IMap<String, BoundKey> boundkeys;

	public void addBoundKey(String map, String key) throws Exception {
		if (this.boundkeys == null) {
			// these shouldn't be namespaced
			String boundKeysMapName = "HZ-BOUNDKEYS-" + this.client.getClient().getCluster().getLocalMember().getUuid();
			this.boundkeys = this.client.getClient().getMap(boundKeysMapName);
			
			IMap<String, String> clusterLocks = this.client.getClient().getMap("HZ-CLUSTER-LOCK");
			clusterLocks.set(boundKeysMapName, "1", 0, TimeUnit.SECONDS);
		}

		String mkey = "map:::" + this.getNamespace() + ":::" + map + ":::" + key;
		if (!this.boundkeys.containsKey(mkey)) {
			this.boundkeys.put(mkey, new BoundKey(this.getNamespace(), map, key));
		}
	}

	public void subscribedToChannel(String channelName) {
		System.out.println("subscribed to channel - " + this.getClientName() + " - "+ channelName);
		SubscribedChannel sc = new SubscribedChannel(this.getNamespace(),
				channelName, this.getClientName(), this.getUniqueChannelName(),
				this.clientIdentifier);
		this.subscribedchannels.add(sc);
		this.clientSubscribedChannels.add(sc);
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
		this.client.setNamespace(namespace);
	}

	public Base(HZClient client) {
		this.client = client;
		
		// we hold all the clients subscribed channels on this,
		// if this instance goes down, another instance in the cluster
		// will clear bound keys and subscribed channels, so this shouldn't be namespaced
		String chanName = "HZ-SUBSCRIBED-CHANNELS-" + this.client.getCluster().getLocalMember().getUuid();
		System.out.println("---->>>> subscribed chan name " + chanName);
		this.subscribedchannels = this.client.getClient().getList(chanName);
		//
		this.clientSubscribedChannels = new ArrayList<SubscribedChannel>();
		//
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

		String n = ManagementFactory.getRuntimeMXBean().getName();

		String clientname = localhostname + "-" + n + "-"
				+ this.getIdentifier();
		return clientname;
	}
	
	public void clientDisconnected() {
		System.out.println(">>>> >>>> Client Disconnected");
//		this.removeAllListeners();
//		this.removeBoundKeys();
		
		// we remove this clients channels
		for (SubscribedChannel subscribedChannel: clientSubscribedChannels){
			// this is namespaced.
			IMap<String, String> kvstore = client.getMap("HZ-SUBSCRIBERS-" + subscribedChannel.channelName);
			for (String s: kvstore.keySet()){
				System.out.println(">>> ---- " + s);
			}
			kvstore.remove(subscribedChannel.clientName);
			System.out.println("removed ------>>>>> " + subscribedChannel.clientName);
		}
	}

	/**
	 * this is triggered when another instance of hazelcast 
	 * in the cluster disconnects, so we remove every client
	 * connected to it.
	 * @param uuid
	 * @param client
	 * @param publisher
	 */
	static public void sremoveAllListeners(String uuid, HazelcastInstance client, Base publisher) {
		IMap<String, String> clusterLocks = client.getMap("HZ-CLUSTER-LOCK");
		System.out.println("cluster locked - 2 - removing listeners for - " + uuid);
		if (clusterLocks.tryLock("clusterlock")) {
			try {
				IList<SubscribedChannel> subscribedchannels = client.getList("HZ-SUBSCRIBED-CHANNELS-" + uuid);
				for (SubscribedChannel subscribedChannel : subscribedchannels) {
					System.out.println("Disconnected - removing listener ["+ subscribedChannel.db + "::" + subscribedChannel.channelName + " - "+uuid+"]");
					
					
					// this is namespaced.
					IMap<String, String> kvstore = client.getMap(subscribedChannel.db + "::" + "HZ-SUBSCRIBERS-" + subscribedChannel.channelName);
					for (String s: kvstore.keySet()){
						System.out.println(">>> ---- " + s);
					}
					kvstore.remove(subscribedChannel.clientName);
					System.out.println("removed ------>>>>> " + subscribedChannel.clientName);
					
					// this is also namespaced
					IMap<String, PubSubChannel> chanstoremap = client.getMap(subscribedChannel.db + "::HZ-CHANNELS");
					PubSubChannel chan = chanstoremap.get(subscribedChannel.channelName);
					if (chan != null) {
						chan.removeClient(subscribedChannel.uniqueChannelName);
						chanstoremap.set(subscribedChannel.channelName, chan, 0,
								TimeUnit.SECONDS);
					}

					publisher.publish("HZ-EVENTS",
							"{'eventtype':'disconnect', " + "'channel':'"
									+ subscribedChannel.channelName + "', " + "'clientname':'"
									+ subscribedChannel.clientName + "'," + "'identifier':'"
									+ subscribedChannel.clientIdentifier + "'}");
				}
				// client is disconnected so nothing left subscribed...
				subscribedchannels.clear();
			} catch (Exception exc) {
				System.out.println("exception on remove all listeners");
				exc.printStackTrace();
			}

			System.out.println("cluster unlocked 2 !!!!");
			clusterLocks.unlock("clusterlock");
		}
	}

	
	/**
	 * this is triggered, when another hazelcast instance in the cluster
	 * gets disconnected, we cleanup every client that is connected to it
	 * @param uuid
	 * @param client
	 */
	static public void sremoveBoundkeys(String uuid, HazelcastInstance client) {
		System.out.println("removing bound keys");
		// we dont want these maps to be namespaced.
		String boundKeysMapName = "HZ-BOUNDKEYS-" + uuid;
		IMap<String, BoundKey> boundkeys = client.getMap(boundKeysMapName);
		IMap<String, String> clusterLocks = client.getMap("HZ-CLUSTER-LOCK");

		System.out.println("cluster locked - 1");
		if (clusterLocks.tryLock("clusterlock")) {
			try {
				// we lock the whole cluster here, because
				// until we cleanup the bound keys, the client shouldnt
				// set the bound keys again.
				if (clusterLocks.tryLock(boundKeysMapName)) {
					System.out.println("acquired lock - " + boundKeysMapName);
					for (BoundKey b : boundkeys.values()) {
						IMap<String, String> hzmap = client.getMap(b.db + "::"
								+ b.map);
						hzmap.remove(b.key);
					}
					clusterLocks.unlock(boundKeysMapName);
				} else {
					System.out.println("couldnt acquire lock on - "
							+ boundKeysMapName);
				}
			} catch (Exception exc) {
				System.out.println("exception on removing bound keys");
				exc.printStackTrace();
			}
			clusterLocks.unlock("clusterlock");
			System.out.println("cluster unlocked !!!");
		}
	}

	public void removeBoundKeys() {
		System.out.println("remove bound keys");
		Base.sremoveBoundkeys(this.client.getCluster().getLocalMember()
				.getUuid(), this.client.getClient());
	}

	public void removeAllListeners() {
		System.out.println("remove listeners...");
		Base.sremoveAllListeners(this.client.getCluster().getLocalMember()
				.getUuid(), this.client.getClient(), this);
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
