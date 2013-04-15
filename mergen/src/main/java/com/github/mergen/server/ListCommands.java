package com.github.mergen.server;

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
import java.lang.annotation.*;
import java.util.concurrent.*;

import com.github.nedis.codec.CommandArgs;

import java.nio.charset.Charset;

public class ListCommands extends Controller {

	@RedisCommand(cmd = "LCLEAR", returns = "OK")
	public void clear(MessageEvent e, Object[] args) {
		String listname = new String((byte[]) args[1]);
		IList<String> list = base.client.getList(listname);
		list.clear();
	}
	
	
	@RedisCommand(cmd = "RPUSH", returns = "OK")
	public void rpush(MessageEvent e, Object[] args) {
		String listname = new String((byte[]) args[1]);
		String v = new String((byte[]) args[2]);
		IList<String> list = base.client.getList(listname);
		list.add(v);
	}

	@RedisCommand(cmd = "LPUSH", returns = "OK")
	public void lpush(MessageEvent e, Object[] args) {
		String listname = new String((byte[]) args[1]);
		String v = new String((byte[]) args[2]);
		IList<String> list = base.client.getList(listname);
		list.add(0, v);
	}
	
	@RedisCommand(cmd = "LPOP")
	public void rpop(MessageEvent e, Object[] args) {
		String listname = new String((byte[]) args[1]);
		IList<String> list = base.client.getList(listname);
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
		
		if (v == null) {
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyNone());
		} else {
			CommandArgs c = new CommandArgs();
			c.add((String) v);
			e.getChannel().write(c.buffer());
		}
	}

	
	
	@RedisCommand(cmd = "LGETALL")
	public void lrange(MessageEvent e, Object[] args) {
		String listname = new String((byte[]) args[1]);
		IList<String> list = base.client.getList(listname);		
		
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		Iterator<String> it = list.iterator();
		while (it.hasNext()) { 
		    String val = (String) it.next(); 
			if (val == null) {
				mr.addNull();
			} else {
				mr.addString(val);
			}
		}
		mr.finish();
		e.getChannel().write(mr.getBuffer());		
	}
	
	
}