package com.github.mergen.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PubSubChannel implements Serializable {
	String channelname;
	// maybe like chat messages etc..
	boolean mustDeliver = false;
	// this fits for queues...
	boolean requireAcknowledgement = false;
	// this can be, 'broadcast', 'roundrobin', 'sticky'...
	String deliveryMethod = "broadcast";
	int clientNumber = 0;
	List <String> clients;
	
	public PubSubChannel(){
		this.clients = new ArrayList<String>();
	}
	
	public String nextClient(){
		return clients.get(this.incrementClient() % clients.size());
	}
	
	public void addClient(String clientId){
		System.out.println("adding client - " + clientId + " size:" + clients.size());
		clients.add(clientId);
	}
	
	public int incrementClient(){
		return this.clientNumber++;
	}
	
}
