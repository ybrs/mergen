package com.github.mergen.server;

import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Transaction;

import org.jboss.netty.channel.MessageEvent;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class PubSubCommands extends Controller {

	@RedisCommand(cmd = "IDENTIFY")
	public void identify(MessageEvent e, Object[] args) {
		System.out.println(">>>>>>>>>>>>>>>>>>>>>" + args.length);
		if (args.length == 1) {
			ServerReply sr = new ServerReply();
			ServerReply.MultiReply mr = sr.startMultiReply();
			mr.addString(this.base.clientIdentifier);
			mr.finish();
			e.getChannel().write(mr.getBuffer());
		} else {
			String v = new String((byte[]) args[1]);
			this.base.clientIdentifier = v;
			ServerReply sr = new ServerReply();
			e.getChannel().write(sr.replyOK());
		}
	}

	private String popWaitingMessage(String channelname) {

		IList<String> list = this.base.client.getList("HZ-PSQUEUE-" + channelname);
		Transaction txn1 = base.client.getTransaction();

		String v = null;
		txn1.begin();
		try {
			v = list.get(0);
			list.remove(0);
		} catch (IndexOutOfBoundsException exc) {
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
			Object object = args[i];
			String channelname = new String((byte[]) object);
			this.base.subscriptioncnt = this.base.subscriptioncnt + 1;
			ITopic topic = this.base.client.getTopic(channelname);
			topic.addMessageListener(this.base);

			this.base.subscribedToChannel(channelname);

			IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-" + channelname);

			kvstore.set(this.base.getClientName(), this.base.clientIdentifier, 0, TimeUnit.SECONDS);

			mr.addString("subscribe");
			mr.addString(channelname);
			// publish this to events channel
			this.base.publish("HZ-EVENTS", "{'eventtype':'subscribed', 'channel':'" + channelname + "', "
					+ "'clientname':'" + this.base.getClientName() + "'," + "'identifier':'"
					+ this.base.clientIdentifier + "'}");

			PubSubChannel chan = base.getChannelProps(channelname);
			chan.addClient(base.getUniqueChannelName());
			base.saveChanProps(channelname, chan);

			/**
			 * if we have waiting messages for this queue/channel deliver them
			 * 
			 * TODO: if its roundrobin ??
			 * 
			 */
			if (channelname.endsWith("-reliable") || base.getChannelProps(channelname).mustDeliver) {
				String v;
				while (true) {
					v = this.popWaitingMessage(channelname);
					if (v == null) {
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
			Object object = args[i];
			String channelname = new String((byte[]) object);

			this.base.subscriptioncnt = this.base.subscriptioncnt - 1;
			if (this.base.subscriptioncnt < 0) {
				this.base.subscriptioncnt = 0;
			}

			ITopic topic = this.base.client.getTopic(channelname);
			topic.removeMessageListener(this.base);

			mr.addString("unsubscribe");
			mr.addString(channelname);

			// remove from hash
			IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-" + channelname);
			kvstore.remove(this.base.getClientName() + "-" + this.base.getIdentifier());

			PubSubChannel chan = base.getChannelProps(channelname);
			chan.removeClient(base.getUniqueChannelName());
			base.saveChanProps(channelname, chan);

			this.base.publish("HZ-EVENTS", "{'eventtype':'unsubscribed', " + "'channel':'" + channelname + "', "
					+ ",'clientname':'" + this.base.getClientName() + "'," + "'identifier':'"
					+ this.base.clientIdentifier + "'}");

		}
		mr.addInt(this.base.subscriptioncnt);
		mr.finish();
		e.getChannel().write(mr.getBuffer());

		// System.out.println(mr.getBuffer().toString(Charset.defaultCharset()));
	}

	@RedisCommand(cmd = "CHANNELPROPERTIES")
	public void channelProperties(MessageEvent e, Object[] args) {
		String channelname = new String((byte[]) args[1]);

		PubSubChannel chan = base.getChannelProps(channelname);

		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		mr.addString("mustdeliver");
		if (chan.mustDeliver) {
			mr.addString("true");
		} else {
			mr.addString("false");
		}
		mr.addString("requireack");
		if (chan.requireAcknowledgement) {
			mr.addString("true");
		} else {
			mr.addString("false");
		}

		mr.addString("deliverymethod");
		mr.addString(chan.deliveryMethod);
		mr.finish();
		e.getChannel().write(mr.getBuffer());
	}

	@RedisCommand(cmd = "MODIFYCHANNEL", returns = "OK")
	public void modifychannel(MessageEvent e, Object[] args) {
		String channelname = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);

		/**
		 * TODO: check arguments...
		 */
		PubSubChannel chan = base.getChannelProps(channelname);
		if (k.equalsIgnoreCase("mustdeliver")) {
			chan.mustDeliver = v.equalsIgnoreCase("true");
		} else if (k.equalsIgnoreCase("requireack")) {
			chan.requireAcknowledgement = v.equalsIgnoreCase("true");
		} else if (k.equalsIgnoreCase("deliverymethod")) {
			// TODO: check params
			chan.deliveryMethod = v;
		}

		base.saveChanProps(channelname, chan);
	}

	@RedisCommand(cmd = "SUBSCRIBERS")
	public void showsubscribers(MessageEvent e, Object[] args) {
		Object object = args[1];
		String channelname = new String((byte[]) object);

		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		IMap<String, String> kvstore = base.client.getMap("HZ-SUBSCRIBERS-" + channelname);

		Set<String> keys = kvstore.keySet();

		for (String k : keys.toArray(new String[0])) {
			mr.addString(k);
			mr.addString(kvstore.get(k));
		}

		mr.finish();
		e.getChannel().write(mr.getBuffer());
	}

	@RedisCommand(cmd = "ROUTING", returns = "OK")
	public void routing(MessageEvent e, Object[] args) {
		String channelname = new String((byte[]) args[1]);

		PubSubChannel chan = base.getChannelProps(channelname);
		ServerReply sr = new ServerReply();
		ServerReply.MultiReply mr = sr.startMultiReply();

		for (Entry<String, String> entry : chan.routing.entrySet()) {
			mr.addString(entry.getKey() + " : " + entry.getValue());
		}

		mr.finish();
		e.getChannel().write(mr.getBuffer());
	}

	/**
	 * sets a key bound to the client, if the client disconnects the key will be
	 * removed.
	 * 
	 * @param e
	 * @param args
	 * @throws Exception
	 */
	@RedisCommand(cmd = "BHSET", returns = "OK")
	public void chanset(MessageEvent e, Object[] args) throws Exception {
		String map = new String((byte[]) args[1]);
		String k = new String((byte[]) args[2]);
		String v = new String((byte[]) args[3]);
		IMap<String, String> kvstore = base.client.getMap(map);
		kvstore.set(k, v, 0, TimeUnit.SECONDS);
		try {
			this.base.addBoundKey(map, k);
		} catch (Exception exc) {
			System.out.println("exception on BHSET - " + exc.getMessage());
			throw exc;
		}
	}

	@RedisCommand(cmd = "PUBLISH", returns = "OK")
	public void publish(MessageEvent e, Object[] args) {
		String v = new String((byte[]) args[2]);
		String channelname = new String((byte[]) args[1]);

		/**
		 * is it persistent or a normal pubsub message ? if channelname ends
		 * with -reliable then we store the message until it gets delivered.
		 * 
		 * if there is no one listening to channel, just save it and return dont
		 * care about the delivery method anymore
		 * 
		 */
		IMap<String, String> kvstore = this.base.client.getMap("HZ-SUBSCRIBERS-" + channelname);

		if (channelname.endsWith("-reliable") || base.getChannelProps(channelname).mustDeliver) {
			// do we have any subscribers to this channel now.
			if (kvstore.size() == 0) {
				// store the message until it gets delivered.
				IList<String> list = this.base.client.getList("HZ-PSQUEUE-" + channelname);
				list.add(v);
				return;
			}
		}

		PubSubChannel chan = base.getChannelProps(channelname);
		if (chan.deliveryMethod.equals("broadcast")) {
			this.base.publish(channelname, v);
		} else if (chan.deliveryMethod.equals("roundrobin")) {
			// if its round robin and we have X clients, relay the message to
			// the next client
			String nextClient = chan.nextClient();
			if (nextClient == null) {
				return;
			}

			// we have to save this everytime, so distributed round robin
			// is a little bit hard work
			// we first save channel props then publish
			base.saveChanProps(channelname, chan);
			//
			this.base.publish(nextClient, v, channelname);
		} else if (chan.deliveryMethod.equals("sticky")) {
			// if its sticky, always relay the same origin to the same client
			String dest = chan.getDestination(this.base.getUniqueChannelName());
			if (dest == null) {
				dest = chan.mapClient(this.base.getUniqueChannelName());
				base.saveChanProps(channelname, chan);
				System.out.println("destination: " + dest);
			}
			this.base.publish(dest, v, channelname);
		}

	}

}
