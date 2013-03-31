package com.github.mergen.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.query.SqlPredicate;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.ObjectMapper;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import java.util.*;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.concurrent.*;

import com.github.nedis.codec.CommandArgs;

import java.nio.charset.Charset;

public class MapObjectCommands extends Controller {
	/**
	 * this is basically the same thing with maps but
	 * stores and retrieves json objects - and you can query on
	 * them.
	 * 
	 * 	
	 * remember hashes/maps are not ordered lists (so its like that in redis too)
	 * 
	 * 
	 */

	@RedisCommand(cmd = "OHDUMMY", returns = "OK")
	public void ohdummy(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);
		ObjectMapper mapper = new ObjectMapper();
		try {
			HashMap<String,Object> jsonData = mapper.readValue(v, HashMap.class);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
	@RedisCommand(cmd = "OHDUMMY2", returns = "OK")
	public void ohdummy2(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);
	}

	
	@RedisCommand(cmd = "OHSET", returns = "OK")
	public void hset(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);
		ObjectMapper mapper = new ObjectMapper();
		try {
			HashMap<String,Object> jsonData = mapper.readValue(v, HashMap.class);
			DataHolder dh = new DataHolder(jsonData);			
			IMap<String, DataHolder> kvstore = base.client.getMap(map);
			kvstore.set(k, dh, 0, TimeUnit.SECONDS);			
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	@RedisCommand(cmd = "OHQUERY")
	public void hmquery(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String q = new String((byte[]) args[2]);
		IMap<String, DataHolder> kvstore = base.client.getMap(map);
		Set<DataHolder> r = (Set<DataHolder>) kvstore.values(new SqlPredicate(q));

		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		ObjectMapper mapper = new ObjectMapper();
		
		for (DataHolder val : r) {
			
			try {
				String s;
				s = mapper.writeValueAsString(val.getHashmap());
				mr.addString(s);
			} catch (JsonGenerationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		
		mr.finish();
		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));
		e.getChannel().write(mr.getBuffer());
		// ServerReply sr = new ServerReply();
		// e.getChannel().write(sr.replyMulti(array));
	}
	
	
	
	@RedisCommand(cmd = "OHSETEX", returns = "OK")
	public void hsetex(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[4]);
		int ttl = Integer.parseInt(new String((byte[]) args[3]));

		IMap<String, String> kvstore = base.client.getMap(map);
		kvstore.set(k, v, ttl, TimeUnit.SECONDS);
	}

	@RedisCommand(cmd = "OHGET")
	public void get(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);

		IMap<String, DataHolder> kvstore = base.client.getMap(map);
		DataHolder v = kvstore.get(k);
		
		if (v == null) {
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyNone());
		} else {
			try {
				CommandArgs c = new CommandArgs();
				ObjectMapper mapper = new ObjectMapper();
				
				String s = mapper.writeValueAsString(v.getHashmap());
				
				
				c.add((String) s);
				e.getChannel().write(c.buffer());
				return;
			} catch (JsonGenerationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// TODO: we failed ?? 
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyNone());
			
		}
	}

	@RedisCommand(cmd = "OHMGET")
	public void hmget(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		IMap<String, String> kvstore = base.client.getMap(map);

		Set<String> keys = new LinkedHashSet<String>(); // we need order

		for (int i = 2; i < args.length; i++) {
			String k = new String((byte[]) args[i]);
			System.out.println("adding >" + k);
			keys.add(k);
		}

		Map<String, String> r = kvstore.getAll(keys);

		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		for (String val : keys) {
			System.out.println(">>>>> " + val + " : " + r.get(val));
			if (r.get(val) == null) {
				mr.addNull();
			} else {
				mr.addString(r.get(val));
			}

		}
		mr.finish();
		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));
		e.getChannel().write(mr.getBuffer());
		// ServerReply sr = new ServerReply();
		// e.getChannel().write(sr.replyMulti(array));
	}

	@RedisCommand(cmd = "OHDEL")
	public void del(MessageEvent e, Object[] args) {
		int delcnt = 0;
		String map = new String((byte[]) args[1]);
		IMap<String, String> kvstore = base.client.getMap(map);

		for (int i = 2; i < args.length; i++) {
			String k = new String((byte[]) args[i]);
			Object v = kvstore.remove(k);
			if (v != null) {
				delcnt++;
			}
		}
		ServerReply sr = new ServerReply();
		e.getChannel().write(sr.replyInt(delcnt));
	}

	@RedisCommand(cmd = "OHEXISTS")
	public void exists(MessageEvent e, Object[] args) {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		IMap<String, String> kvstore = base.client.getMap(map);
		if (kvstore.containsKey(k)) {
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyInt(1));
		} else {
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyInt(0));
		}
	}

	@RedisCommand(cmd = "OHKEYS")
	public void hkeys(MessageEvent e, Object[] args) {
		// this is half baked
		String map = new String((byte[]) args[1]);
		IMap<String, String> kvstore = base.client.getMap(map);
		Set<String> keys = kvstore.keySet();
		String[] array = keys.toArray(new String[0]);
		System.out.println(Arrays.toString(array));
		ServerReply sr = new ServerReply();
		e.getChannel().write(sr.replyMulti(array));
	}

	@RedisCommand(cmd = "OHLEN")
	public void hlen(MessageEvent e, Object[] args) {
		// this is half baked
		String map = new String((byte[]) args[1]);
		IMap<String, String> kvstore = base.client.getMap(map);
		ServerReply sr = new ServerReply();
		e.getChannel().write(sr.replyInt(kvstore.size()));
	}

}
