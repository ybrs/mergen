package com.github.nedis;

import com.github.nedis.codec.*;
import com.github.nedis.pubsub.PubSubHandler;
import com.github.nedis.pubsub.RedisListener;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * User: roger
 * Date: 12-3-15 11:03
 */
public class NettyRedisClientImpl implements RedisClient {
    private Logger logger = Logger.getLogger(getClass().getName());
    private ClientBootstrap bootstrap;
    private Channel channel;
    private Timer timer;
    private ChannelGroup channels;

    private String host;
    private int port;
    private ChannelPipeline pipeline;

    private long timeout;

    private TimeUnit unit;

    private boolean pipelineMode;

    private List<Command> pipelineCommands = new ArrayList<Command>();

    private List<RedisListener> listeners = new CopyOnWriteArrayList<RedisListener>();

    public void startPipeline() {
        pipelineMode = true;
    }

    public void endPipeline() {
        pipelineMode = false;
    }

    public NettyRedisClientImpl(String host) {
        this("localhost", 6379);
    }

    synchronized ChannelPipeline getPipeline() {

        return pipeline;
    }

    public NettyRedisClientImpl(String host, int port) {
        this.host = host;
        this.port = port;

        channels = new DefaultChannelGroup();
        timer    = new HashedWheelTimer();

        ClientSocketChannelFactory factory =  new  NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ClientBootstrap(factory);

        pipeline = Channels.pipeline(new AutoReconnectHandler(this, channels, timer), new RedisDecoder(), new RedisClientHandler());

        bootstrap.setPipeline(pipeline);

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        channel = future.awaitUninterruptibly().getChannel();

        timeout = 60;
        unit = TimeUnit.SECONDS;

    }

    public void reconnect() {
        logger.info("reconnect");
        bootstrap.setPipeline(pipeline);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        channel = future.awaitUninterruptibly().getChannel();
    }

    public String ping() {
        Command command = new Command(CommandType.PING,  null);
        channel.write(command);

        Reply reply = getReply(command);
        return reply == null ? null : ((StatusReply)reply).get();

    }

    public void set(String key, String value) {
        Command set = new Command(CommandType.SET, new CommandArgs().addKey(key).addValue(value));
        channel.write(set);
    }

    public void setSafeBinary(String key, byte[] value) {

    }

    public String get(String key) {
        Command cmd = new Command(CommandType.GET, new CommandArgs().addKey(key));
        channel.write(cmd);

        Reply reply =  getReply(cmd);

        return reply == null ? null : ((BulkReply)reply).getString();

    }


    private Reply getReply(Command cmd) {
        if(pipelineMode) {
            pipelineCommands.add(cmd);
            return null;
        }
        if (!cmd.await(timeout, unit)) {
            cmd.cancel(true);
            throw new RedisException("Command timed out");
        }
        Reply reply =  cmd.getOutput();
        if(reply instanceof  ErrorReply) throw new RedisException(((ErrorReply)reply).get());

        return reply;
    }
    public byte[] getSafeBinary(String key) {
        return new byte[0];
    }

    public void addListener(RedisListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RedisListener listener) {
        listeners.remove(listener);
    }

    public synchronized void subscribe(String... channels) {
        ChannelHandler handler = channel.getPipeline().getLast();

        if(handler instanceof RedisClientHandler) {
            channel.getPipeline().replace(RedisClientHandler.class, "pubsub", new PubSubHandler(listeners));
        }

        Command cmd = new Command(CommandType.SUBSCRIBE, args(channels));
        channel.write(cmd);

    }

    public void publish(String pchannel, String message) {
        ChannelHandler handler = channel.getPipeline().getLast();

        if(handler instanceof RedisClientHandler) {
            channel.getPipeline().replace(RedisClientHandler.class, "pubsub", new PubSubHandler(listeners));
        }

        Command cmd = new Command(CommandType.PUBLISH, new CommandArgs().add(pchannel).addValue(message));
        channel.write(cmd);

    }

    private CommandArgs args(String... strings) {
        CommandArgs args = new CommandArgs();
        for (String c : strings) {
            args.add(c);
        }
        return args;
    }

    public static void main(String[] args) throws  Exception {
        final RedisClient redis = new NettyRedisClientImpl("localhost");
        redis.subscribe("mq1");
        redis.addListener(new RedisListener() {
            @Override
            public void message(String channel, String message) {
                System.out.println("on channel " + channel +", receive message : " + message);
            }

            @Override
            public void message(String pattern, String channel, String message) {

            }
        });
        final RedisClient rr = new NettyRedisClientImpl("localhost");
        rr.publish("mq1", "你是谁啊");


    }
}
