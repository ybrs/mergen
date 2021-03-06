package com.github.mergen.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.net.InetSocketAddress;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// import com.github.nedis.pubsub.RedisListener;
import com.github.mergen.persistence.DummyStore;
import com.github.nedis.codec.*;
import com.github.nedis.pubsub.RedisListener;

import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.query.SqlPredicate;

import com.beust.jcommander.JCommander;

import java.io.FileNotFoundException;
import java.lang.Class;
import java.util.*;

public class Server {

	private final String host;
	private int port;
	private DefaultChannelGroup channelGroup;
	private ServerChannelFactory serverFactory;

	private ChannelGroup channels;
	private Timer timer;

	private long cnt = 0;

	private ServerBootstrap bootstrap;
	private HazelcastInstance client;
	private CommandDispatcher dispatcher;

	private ServerCommandLineArguments jct;
	private ChannelPipelineFactory pipelineFactory;

	// public List<RedisListener> listeners;

	public Server(ServerCommandLineArguments jct) {
		this.host = jct.host;
		this.port = jct.port;
		this.jct = jct;
		System.out.println("init");
	}

	public void prepareHazelcastCluster() {

	}

	public void configurePersistence(Config cfg) {
		/** persistence related config **/
		MapConfig mapConfig = new MapConfig("*");
		MapStoreConfig mapstoreconfig = mapConfig.getMapStoreConfig();
		if (mapstoreconfig == null) {
			mapstoreconfig = new MapStoreConfig();
		}
		mapstoreconfig.setFactoryClassName(this.jct.persistence_class)
				.setEnabled(true);
		mapstoreconfig.setWriteDelaySeconds(this.jct.persistence_write_delay);
		mapstoreconfig.setProperty("servers", this.jct.persistence_servers);
		mapConfig.setMapStoreConfig(mapstoreconfig);
		cfg.addMapConfig(mapConfig);
	}

	public void prepare() {
		System.out.println("prepare");
		channels = new DefaultChannelGroup();
		timer = new HashedWheelTimer();
		this.serverFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		this.channelGroup = new DefaultChannelGroup(this + "-channelGroup");

//		MapConfig mapConfig = new MapConfig("HZ-CHANNELS");
//		NearCacheConfig nearCacheConfig = new NearCacheConfig();
//		mapConfig.setNearCacheConfig(nearCacheConfig);

		Config cfg;

		if (this.jct.configpath != "") {
			System.out.println("reading config from " + this.jct.configpath);
			try {
				cfg = new XmlConfigBuilder(this.jct.configpath).build();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out
						.println("couldnt find config file, proceeding with defaults !!!");
				cfg = new Config();
			}
			System.out.println("reading config from " + this.jct.configpath
					+ " DONE ");
		} else {
			cfg = new Config();
		}

		if (this.jct.persistence.equals("true")
				|| this.jct.persistence.equals("on")) {
			configurePersistence(cfg);
		}

		NetworkConfig network = cfg.getNetworkConfig();

		if (this.jct.publicipaddress != "") {
			System.out.println("setting public ip address: "
					+ this.jct.publicipaddress);
			network.setPublicAddress(this.jct.publicipaddress);
		}

		Join join = network.getJoin();

		if ((this.jct.multicastgroup != "") && (this.jct.multicastport > 0)) {
			join.getMulticastConfig()
					.setMulticastGroup(this.jct.multicastgroup)
					.setMulticastPort(this.jct.multicastport).setEnabled(true);
		} else {
			join.getMulticastConfig().setEnabled(false);
		}

		for (String ns : this.jct.hzcluster) {
			join.getTcpIpConfig().addMember(ns);
		}

		join.getTcpIpConfig().setEnabled(true);
		client = Hazelcast.newHazelcastInstance(cfg);
		
		final IMap<String, String> clusterLocks = client.getMap("HZ-CLUSTER-LOCK");
		if (clusterLocks.get("clusterlock") == null){
			// we are putting a key, if not exists just to lock further
			clusterLocks.set("clusterlock", "1", 0, TimeUnit.SECONDS);
		}
		
		Cluster hzcluster = client.getCluster();
		hzcluster.addMembershipListener(new MembershipListener(){

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
		        System.out.println("MemberAdded " + membershipEvent);				
			}

			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
		        System.out.println("MemberRemoved" + membershipEvent);
		        // on disconnect we have to make sure bound keys are
		        // cleaned up and subscribers list are still correct.
		        // so we start a cleanup process
		        String uuid = membershipEvent.getMember().getUuid();
		        Base.sremoveBoundkeys(uuid, client);
		        // we need a new base here because we publish disconnected
		        // event to other subscribers.
		        Base publisher = new Base(new HZClient(client));
		        Base.sremoveAllListeners(uuid, client, publisher);
			}
			
		});
		
		/*
		 * We build up the dispatcher now ! Wish java had mixins
		 */
		List<Class<?>> klasses = new ArrayList<Class<?>>();
		klasses.add(ServerCommands.class);
		klasses.add(MapCommands.class);
		klasses.add(ListCommands.class);
		klasses.add(MapObjectCommands.class);
		klasses.add(PubSubCommands.class);
		dispatcher = new CommandDispatcher(klasses);

		final Map<String, Controller> subscriptions = new ConcurrentHashMap<String, Controller>();

		pipelineFactory = new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {

				cnt = cnt + 1;

				if (cnt > Long.MAX_VALUE - 1) {
					cnt = 0;
				}

				System.out.println("client connecting");
				boolean acquireLock = clusterLocks.tryLock("clusterlock", 10, TimeUnit.SECONDS);
				if (acquireLock){
					// we unlock immediately, since we dont care about the lock,
					// we just check if there is a lock...
					System.out.println("lock acquired - unlocking");
					clusterLocks.unlock("clusterlock");
				} else {
					// this is to prevent race conditions.
					// eg. client sets bound keys, and connected mergen instance crashed
					// another mergen instance clears those bound keys, since the client 
					// is disconnected, meanwhile the client reconnects
					// and sets its keys again. so we have a chaos.
					// we wait for 10 seconds and wait for the cleanup
					// or else throw an exception
					System.out.println("cluster is locked !!!");
					throw new Exception("cluster is locked");
				}
				System.out.println("client connected");
				
				ServerHandler handler = new ServerHandler(channelGroup);
				handler.setClient(client);
				handler.setDispatcher(dispatcher);
				handler.setPubSubList(subscriptions, cnt);

				ChannelPipeline pipeline = Channels.pipeline();
				// pipeline.addLast("encoder", Encoder.getInstance());
				// pipeline.addLast("encoder", Command);
				pipeline.addLast("decoder", new RedisDecoder());
				pipeline.addLast("handler", handler);
				return pipeline;
			}
		};

	}

	private XmlConfigBuilder XmlConfigBuilder(String configpath) {
		// TODO Auto-generated method stub
		return null;
	}

	public void start() {
		ServerBootstrap bootstrap = new ServerBootstrap(this.serverFactory);
		bootstrap.setOption("reuseAddress", true);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setPipelineFactory(pipelineFactory);

		Channel channel = bootstrap.bind(new InetSocketAddress(this.host,
				this.port));
		// if (!channel.isBound()) {
		// this.stop();
		// }

		this.channelGroup.add(channel);

	}

	public void stop() {
		if (this.channelGroup != null) {
			this.channelGroup.close();
		}
		if (this.serverFactory != null) {
			this.serverFactory.releaseExternalResources();
		}
	}

	public static void main(String[] args) {

		ServerCommandLineArguments jct_ = new ServerCommandLineArguments();
		new JCommander(jct_, args);

		System.out.println("listening on " + jct_.host + ":" + jct_.port + "");

		final Server server = new Server(jct_);

		server.prepare();

		final long timeToWait = 1000;

		while (true) {
			try {
				server.start();
				break;
			} catch (Exception e) {
				server.port = server.port + 1;
				try {
					Thread.sleep(timeToWait);
				} catch (InterruptedException i1) {
					// pass
				}
			}
		}

		System.out.println("Mergen Server listening for commands on "
				+ server.port);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.stop();
			}
		});
	}
}
