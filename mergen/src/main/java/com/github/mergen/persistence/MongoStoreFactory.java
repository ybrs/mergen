package com.github.mergen.persistence;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;
import java.util.Properties;

public class MongoStoreFactory {
	
    public MapLoader newMapStore(String mapName, Properties properties) {
    	String servers = properties.getProperty("servers");
    	String[] serverList = servers.split(";");
    	
    	for (int i = 0; i < serverList.length; i++) {
			System.out.println(serverList[i]);
		}
    	
    	MongoStore store = new MongoStore();
    	store.setServers(serverList);
    	store.connect();
    	
        return store;    	       
    }	
}
