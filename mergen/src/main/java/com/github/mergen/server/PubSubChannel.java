package com.github.mergen.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
		this.clients = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public String nextClient(){
		int size = clients.size();
		if (size == 0){
			return null;
		}
		return clients.get(this.incrementClient() % size);
	}
	
	public void addClient(String clientId){
		clients.add(clientId);
		System.out.println(clients);
		System.out.println("adding client - " + clientId + " size:" + clients.size());
	}
	
	public void removeClient(String clientId){
		clients.remove(clientId);
	}
	
	
	public int incrementClient(){
		return this.clientNumber++;
	}
	
}
