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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// import com.github.nedis.pubsub.RedisListener;
import com.github.nedis.codec.*;

import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

import com.beust.jcommander.JCommander;
import java.lang.Class;
import java.util.*;

public class Server {

    private final String host;
    private final int port;
    private DefaultChannelGroup channelGroup;
    private ServerChannelFactory serverFactory;

    private ChannelGroup channels;
    private Timer timer;

    private ServerBootstrap bootstrap;
    private HazelcastInstance client;    
    private CommandDispatcher dispatcher;

    private ServerCommandLineArguments jct;

    public Server(ServerCommandLineArguments jct) {
        this.host = jct.host;
        this.port = jct.port;
        this.jct = jct;
    }

    public boolean start() {
        channels = new DefaultChannelGroup();
        timer    = new HashedWheelTimer();
        this.serverFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                                                               Executors.newCachedThreadPool());
        this.channelGroup = new DefaultChannelGroup(this + "-channelGroup");


//        ClientConfig clientConfig = new ClientConfig();        
//        for (String hzi: jct.hzcluster){            
//            clientConfig.addAddress(hzi);
//        }
        
//        client = HazelcastClient.newHazelcastClient(clientConfig);    

        Config cfg = new Config();
        
        NetworkConfig network = cfg.getNetworkConfig();
        Join join = network.getJoin();
        join.getMulticastConfig().setEnabled(true);                
        join.getTcpIpConfig()
        	.addMember("127.0.0.1:5701")
        	.addMember("127.0.0.1:5702")
        	.setEnabled(true);
        
        client = Hazelcast.newHazelcastInstance(cfg);
        
        /*
        * We build up the dispatcher now !
        * Wish java had mixins
        */        
        List< Class< ? > > klasses = new ArrayList< Class< ? > >();
        klasses.add(ServerCommands.class);
        klasses.add(MapCommands.class);
        dispatcher = new CommandDispatcher(klasses);                    

        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {

                ServerHandler handler = new ServerHandler(channelGroup);
                handler.setClient(client);
                handler.setDispatcher(dispatcher);

                ChannelPipeline pipeline = Channels.pipeline();
                // pipeline.addLast("encoder", Encoder.getInstance());
                
                // pipeline.addLast("encoder", Command);
                pipeline.addLast("decoder", new RedisDecoder());
                pipeline.addLast("handler",  handler);
                return pipeline;
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap(this.serverFactory);
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setPipelineFactory(pipelineFactory);
        try{        	
        	Channel channel = bootstrap.bind(new InetSocketAddress(this.host, this.port));
            if (!channel.isBound()) {
                this.stop();
                return false;
            }

            this.channelGroup.add(channel);
            
        } catch (org.jboss.netty.channel.ChannelException e){
        	return false;
        }
        
        return true;
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

        System.out.println("listening on: "+ jct_.host + ":" + jct_.port +"");
        System.out.println("Connecting to hazelcast servers "+ jct_.hzcluster);

        final Server server = new Server(jct_);
        
        if (!server.start()) {
            System.out.println("!!!!!! couldnt start server !!!!!");
            // TODO: this should fallback to standby server mode, 
            // if the listening server fails, this should take over imho.
            return; 
        }

        System.out.println("Server started...");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }
}
