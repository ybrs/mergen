package com.github.mergen.persistence;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class PersistaStore implements MapLoader, MapStore {
	
	private String[] servers;
	private List<JedisPool> connections = new ArrayList<JedisPool>();
	
	public String[] getServers(){
		return this.servers;
	}
	
	public void setServers(String[] servers){
		this.servers = servers;
	}

	public void connect(){
		
		JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Maximum active connections to Redis instance
        poolConfig.setMaxActive(10);
        // Tests whether connection is dead when connection
        // retrieval method is called
        poolConfig.setTestOnBorrow(true);        		
        // Minimum number of idle connections to Redis
        // These can be seen as always open and ready to serve
        poolConfig.setMinIdle(1);
        // Tests whether connections are dead during idle periods
        poolConfig.setTestWhileIdle(true);
        // Maximum number of connections to test in each idle check
        poolConfig.setNumTestsPerEvictionRun(10);
        // Idle connection checking period
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        
		for (int i = 0; i < this.servers.length; i++) {
			String server = this.servers[i];
			String[] hostport = server.split(":");
			String host = hostport[0];
			int port = Integer.parseInt(hostport[1]);
			this.connections.add(new JedisPool(poolConfig, host, port));			
		}
	}
	
	private int lastconn = 0;
	
	private Jedis getConnection(){
		return this.connections.get(lastconn).getResource();
	}
	
	
    public Set loadAllKeys() {
        System.out.println("Loader.loadAllKeys ");
        Set keys = new HashSet();
        keys.add("key");
        return keys;
    }

    public Object load(Object key) {
        System.out.println("Loader.load " + key);
        Jedis j = this.getConnection();
        try {
        	return j.get((String) key).toString();
        } finally {
        	this.connections.get(lastconn).returnResource(j);
        }               
    }

    public Map loadAll(Collection keys) {
        System.out.println("Loader.loadAll keys " + keys);
        return null;
    }

    public void store(Object key, Object value) {
        System.out.println("Store.store key=" + key + ", value=" + value);
        Jedis j = this.getConnection();
        try {
        	j.set((String) key, (String) value);
        } finally {
        	this.connections.get(lastconn).returnResource(j);
        }               
        
    }

    public void storeAll(Map map) {
        System.out.println("Store.storeAll " + map.size());
    }

    public void delete(Object key) {
        System.out.println("Store.delete " + key);
    }

    public void deleteAll(Collection keys) {
        System.out.println("Store.deleteAll " + keys);
    }
}

