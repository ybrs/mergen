package com.github.mergen.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
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
	Hashtable<String, String> routing;	
	Hashtable<String, Integer> routingCounts;	
	
	public PubSubChannel(){
		this.routing = new Hashtable<String, String>();
		this.routingCounts = new Hashtable<String, Integer>();
		this.clients = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public void AddRouting(String source, String destination){
		this.routing.put(source, destination);
		this.routingCounts.put(destination, 0);
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

	public String getDestination(String source) {
		String dest = this.routing.get(source);
		// TODO: this should check if destination is still there...
		return dest;
	}

	public String mapClient(String source) {
		// try to find if there is a client not mapped yet..
		String foundWorker = null;
		int lastCnt = Integer.MAX_VALUE;
		for (int i = 0; i < this.clients.size(); i++) {
			Integer cnt = this.routingCounts.get(this.clients.get(i)); 
			if (cnt == null){
				// this ones free.
				foundWorker = this.clients.get(i);
				break;
			} else {
				if (cnt < lastCnt){
					foundWorker = this.clients.get(i);
				}
			}
			lastCnt = cnt;
		}
		//
		this.routing.put(source, foundWorker);
		// TODO: increment
		this.routingCounts.put(foundWorker, 0);
		return foundWorker;
	}
	
}
