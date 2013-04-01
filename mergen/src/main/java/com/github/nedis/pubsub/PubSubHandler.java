package com.github.nedis.pubsub;

import com.github.nedis.codec.Command;
import com.github.nedis.codec.MultiBulkReply;
import com.github.nedis.codec.Reply;
import org.jboss.netty.channel.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * User: roger
 * Date: 12-4-9 上午11:06
 */
public class PubSubHandler extends SimpleChannelHandler {
    private Logger logger = Logger.getLogger(getClass().getName());
    private List<RedisListener> listeners;
    public PubSubHandler(List<RedisListener> listeners) {
        this.listeners = listeners;
    }
    
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws java.lang.Exception {

        Command command = (Command)e.getMessage();
        Channels.write(ctx, e.getFuture(), command.buffer());
    }
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws java.lang.Exception {

        Reply reply = (Reply)e.getMessage();
        if(reply instanceof MultiBulkReply) {
            MultiBulkReply multiReply = (MultiBulkReply)reply;
            Object[] values = multiReply.get();
            if(values != null) {
                String type = new String((byte[])values[0]);
                if("message".equals(type)) {
                    if(listeners != null) {
                        for(RedisListener listener : listeners) {
                            listener.message(new String((byte[])values[1]), new String((byte[])values[2]));
                        }
                    }
                }
            }
        }


        //logger.info("command:" + command + ",return " + reply);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws java.lang.Exception {
        logger.fine("exception : " + e);

    }


}
