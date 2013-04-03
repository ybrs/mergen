package com.github.mergen.server;

import java.io.Serializable;

public class TopicMessage implements Serializable{
	private String str;
	private String listenerid;
	private String messagetype;
	private String channel;
	
	public TopicMessage(String str, String listenerid, String messagetype, String channel) {
		this.setStr(str);
		this.listenerid = listenerid;
		this.messagetype = messagetype;
		this.channel = channel;
	}

	public String getChannel(){
		return channel;
	}
	
	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}
	
	
}
