package com.github.mergen.server;

import java.io.Serializable;

public class TopicMessage implements Serializable{
	private String str;
	private String listenerid;
	
	public TopicMessage(String str, String listenerid) {
		this.setStr(str);
		this.listenerid = listenerid;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}
	
	
}
