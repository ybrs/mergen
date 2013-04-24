package com.github.mergen.persistence;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;

import java.util.List;
import java.util.Properties;

public class PersistaStoreFactory implements MapStoreFactory {

    public MapLoader newMapStore(String mapName, Properties properties) {
    	System.out.println(properties);
    	
    	String servers = properties.getProperty("servers");
    	String[] serverList = servers.split(";");
    	
    	for (int i = 0; i < serverList.length; i++) {
    		System.out.println("=============>>>>");
			System.out.println(serverList[i]);
		}
    	
    	PersistaStore store = new PersistaStore();
    	store.setServers(serverList);
    	store.connect();
    	
        return store;
    }
}