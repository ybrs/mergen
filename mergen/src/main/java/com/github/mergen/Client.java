package com.github.mergen.server;
import com.hazelcast.core.Hazelcast;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;


public class Client {

    public static void main(String[] args) {


        ClientConfig clientConfig = new ClientConfig();        
        // for (String hzi: jct.hzcluster){            
        //     clientConfig.addAddress(hzi);
        // }
        clientConfig.addAddress("localhost:5701");

        HazelcastClient client = HazelcastClient.newHazelcastClient(clientConfig);    
        Map<String, String> mymap = client.getMap("__kvstore");
        Set<String> keys = mymap.keySet();

        System.out.println("=========================================");
        System.out.println(keys);
        // Collection<String> colCustomers = mapCustomers.values();

    }
}
