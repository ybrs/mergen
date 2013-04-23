package com.github.mergen.persistence;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;

import java.util.Properties;

public class DummyStoreFactory implements MapStoreFactory {

    public MapLoader newMapStore(String mapName, Properties properties) {
    	System.out.println(properties);
        return new DummyStore();
    }
}