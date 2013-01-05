package com.github.mergen.server;

import java.io.Serializable;
import java.util.HashMap;

public class DataHolder implements Serializable {
	private HashMap<String, Object> map;

	public DataHolder() {
		this.map = new HashMap<String, Object>();
	}

	public void setValue(String k, Object v){
		this.map.put(k, v);
	}

	public Object getValue(String k){
		return this.map.get(k);
	}
	
}