package com.github.mergen.server;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.hazelcast.config.Config;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.ClientService;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISemaphore;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Instance;
import com.hazelcast.core.InstanceListener;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.core.MultiMap;
import com.hazelcast.core.Transaction;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.partition.PartitionService;


/**
 * this is just a proxy class around HazelcastInstance
 * we use this mainly for namespaces...
 * @author aybarsbadur
 */
public class HZClient implements HazelcastInstance{
	private HazelcastInstance hzclient;
	private String namespace;

	public HazelcastInstance getClient(){
		return this.hzclient;
	}
	
	public HZClient(HazelcastInstance hzclient){
		this.hzclient = hzclient;
	}
	
	public String getNamespaced(String k){
		String namespaced = this.namespace + "::" + k;
		return namespaced;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String getName() {
		return this.hzclient.getName();
	}

	@Override
	public <E> IQueue<E> getQueue(String name) {
		return this.hzclient.getQueue(this.getNamespaced(name));
	}

	@Override
	public <E> ITopic<E> getTopic(String name) {
		return this.hzclient.getTopic(this.getNamespaced(name));
	}

	@Override
	public <E> ISet<E> getSet(String name) {
		return this.hzclient.getSet(this.getNamespaced(name));
	}

	@Override
	public <E> IList<E> getList(String name) {
		return this.hzclient.getList(this.getNamespaced(name));
	}

	@Override
	public <K, V> IMap<K, V> getMap(String name) {
		return this.hzclient.getMap(this.getNamespaced(name));
	}

	@Override
	public <K, V> MultiMap<K, V> getMultiMap(String name) {
		return this.hzclient.getMultiMap(this.getNamespaced(name));
	}

	@Override
	public ILock getLock(Object key) {
		return this.hzclient.getLock(key);
	}

	@Override
	public Cluster getCluster() {
		return this.hzclient.getCluster();
	}

	@Override
	public ExecutorService getExecutorService() {
		return this.hzclient.getExecutorService();
	}

	@Override
	public ExecutorService getExecutorService(String name) {
		return this.hzclient.getExecutorService();
	}

	@Override
	public Transaction getTransaction() {
		return this.hzclient.getTransaction();
	}

	@Override
	public IdGenerator getIdGenerator(String name) {
		return this.hzclient.getIdGenerator(name);
	}

	@Override
	public AtomicNumber getAtomicNumber(String name) {
		return this.hzclient.getAtomicNumber(name);
	}

	@Override
	public ICountDownLatch getCountDownLatch(String name) {
		return this.hzclient.getCountDownLatch(name);
	}

	@Override
	public ISemaphore getSemaphore(String name) {
		return this.hzclient.getSemaphore(name);
	}

	@Override
	public void shutdown() {
		this.hzclient.shutdown();
	}

	@Override
	public void restart() {
		this.hzclient.restart();
	}

	@Override
	public Collection<Instance> getInstances() {
		return this.hzclient.getInstances();
	}

	@Override
	public void addInstanceListener(InstanceListener instanceListener) {
		this.hzclient.addInstanceListener(instanceListener);		
	}

	@Override
	public void removeInstanceListener(InstanceListener instanceListener) {
		this.hzclient.removeInstanceListener(instanceListener);
	}

	@Override
	public Config getConfig() {
		return this.hzclient.getConfig();
	}

	@Override
	public PartitionService getPartitionService() {
		return this.hzclient.getPartitionService();
	}

	@Override
	public ClientService getClientService() {
		return this.hzclient.getClientService();
	}

	@Override
	public LoggingService getLoggingService() {
		return this.hzclient.getLoggingService();
	}

	@Override
	public LifecycleService getLifecycleService() {
		return this.hzclient.getLifecycleService();
	}
	
	
}
