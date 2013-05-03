package com.github.mergen.persistence;
import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MongoStore implements MapLoader, MapStore {
	
    public Set loadAllKeys() {
        System.out.println("Loader.loadAllKeys ");
        Set keys = new HashSet();
        keys.add("key");
        return keys;
    }

    public Object load(Object key) {
        System.out.println("Loader.load " + key);
        return "loadedvalue";
    }

    public Map loadAll(Collection keys) {
        System.out.println("Loader.loadAll keys " + keys);
        return null;
    }

    public void store(Object key, Object value) {
        System.out.println("Store.store key=" + key + ", value=" + value);
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

	public void setServers(String[] serverList) {
		// TODO Auto-generated method stub
		
	}

	public void connect() {
		// TODO Auto-generated method stub
		
	}
    
}


