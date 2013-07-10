package com.github.mergen.server;

import java.io.Serializable;

public class PubSubChannel implements Serializable {
	String channelname;
	// maybe like chat messages etc..
	boolean mustDeliver = false;
	// this fits for queues...
	boolean requireAcknowledgement = false;
	// this can be, 'broadcast', 'roundrobin', 'sticky'...
	String deliveryMethod = "broadcast";
}
