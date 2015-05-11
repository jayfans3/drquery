package com.asiainfo.billing.drquery.cache.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import redis.clients.jedis.exceptions.JedisConnectionException;

import com.asiainfo.billing.drquery.cache.CacheException;
import com.asiainfo.billing.drquery.cache.ICache;
import com.asiainfo.billing.drquery.cache.support.CacheParameters.Range;
import com.asiainfo.billing.drquery.utils.PropertiesUtil;

public class RedisCache  implements ICache,InitializingBean {
	
	private static final Log log = LogFactory.getLog(RedisCache.class);
	
	private RedisTemplate<String, Object> redisTemplate;
	private BlockingQueue<Map<String, Object>> mapQueue = 
			new ArrayBlockingQueue<Map<String, Object>>(Integer.parseInt(PropertiesUtil.getProperty("drquery.service/runtime.properties","redis.map.queue.size","100")));
	CopyOnWriteArrayList<String> cacheKeys = new CopyOnWriteArrayList<String>();
	
	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public RedisTemplate<String, Object> getRedisTemplate() {
		return redisTemplate;
	}


	private long timeout=-1;
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
    private void expire(String key, boolean isTimeout){
    	if(timeout!=-1&&isTimeout){
    		try{
    			redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
			}
			catch(Exception e){
				handlerException(e);
			}
    	}
        redisTemplate.opsForHash().entries("");
    }
    
    private void expire(String key, long timeout){
    	if(timeout != -1){
    		try{
    			redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
			}
			catch(Exception e){
				handlerException(e);
			}
    	}
    }
    
	public <V> List<V> getValue(String key, Range range) throws CacheException {
		Map<String,Object> map = getDataFromQueue(key);
		List<V> retList = new ArrayList<V>();
		if(map!=null){
			Collection<Object> keys = range.getLimitKey();
			for(Object o:keys){
				//JSONObject obj = JSONObject.fromObject(map.get(o));
				retList.add((V) map.get(o));
			}
			return retList;
		}
		Collection<V> retCollection = null;
		try{
			if (log.isDebugEnabled()) {
				log.debug("start query data from Redis|key-" + key);
			}
			HashOperations<String, Object, V> hashOperations = redisTemplate.opsForHash();
			if (!redisTemplate.hasKey(key)) {
				if(log.isWarnEnabled()){
					log.warn("Radis cant query data via key[" + key + "]");
				}
				return null;
			}
			retCollection =  hashOperations.multiGet(key,range.getLimitKey());
			Iterator<V> iter = retCollection.iterator();
			while(iter.hasNext()){
				//JSONObject obj = JSONObject.fromObject(iter.next());
				retList.add(iter.next());
			}
			
		}
		catch(Exception e){
			handlerException(e);
		}
		return (List<V>) retList;
	}
    
	public void putData2Cache(String key, Map<String, Object> value,boolean isTimeout) throws CacheException{
		Map<String, Object> target = new HashMap<String, Object>();
		target.putAll(value);
		Map<String,Object> metaMapData = new HashMap<String,Object>();
		metaMapData.put(key+"^|^"+(isTimeout == true ? timeout : -1) ,target);
		if(cacheKeys.contains(key)){
			if(log.isDebugEnabled()){
				log.debug("Input cache key["+key+"] contain cacheKeys,so needn't set in mapQueue");
			}
		}
		else{
			boolean ifInsertCache = mapQueue.offer(metaMapData);
			if(ifInsertCache==false){
				log.warn("can't insert cache data to queue,maybe the queue is full?");
			}
			else{
				cacheKeys.addIfAbsent(key);
			}
		}
		
		
//		redisTemplate.opsForHash().putAll(key, value);
//		expire(key,isTimeout);
	}
	
	@Override
	public void putData2Cache(String key, Map<String, Object> value, long timeout) throws CacheException {
//		Map<String, Object> target = new HashMap<String, Object>();
//		target.putAll(value);
//		Map<String,Object> metaMapData = new HashMap<String,Object>();
//		metaMapData.put(key + "|" + timeout, target);
//		if(cacheKeys.contains(key)){
//			if(log.isDebugEnabled()){
//				log.debug("Input cache key["+key+"] contain cacheKeys,so needn't set in mapQueue");
//			}
//		}
//		else{
//			boolean ifInsertCache = mapQueue.offer(metaMapData);
//			if(ifInsertCache==false){
//				log.warn("can't insert cache data to queue,maybe the queue is full?");
//			}
//			else{
//				cacheKeys.addIfAbsent(key);
//			}
//		}
		redisTemplate.opsForHash().putAll(key, value);
		expire(key,timeout);
	}
	
	public <V> void putData2Cache(String key, List<V> value,boolean isTimeout) throws CacheException{
		Map<String,V> map = new HashMap<String,V>();
		int index=1;
		for(int i =0;i<value.size();i++){
			V metaValue=value.get(i);
			map.put(index + "", metaValue);
			index++;
		}
		HashMap<String,Object> metaMapData = new HashMap<String,Object>();
		metaMapData.put(key+"^|^"+(isTimeout == true ? timeout : -1),map);
		if(cacheKeys.contains(key)){
			if(log.isDebugEnabled()){
				log.debug("Input cache key["+key+"] contain cacheKeys,so needn't set in mapQueue");
			}
		}else{
			boolean ifInsertCache = mapQueue.offer(metaMapData);
			if(ifInsertCache==false){
				log.warn("can't insert cache data to queue,maybe the queue is full?");
			}
			else{
				cacheKeys.addIfAbsent(key);
			}
		}
//		redisTemplate.opsForHash().putAll(key, map);
//		expire(key,isTimeout);
	}
	
	public <V> void putData2Cache(String key, V value,boolean isTimeout) throws CacheException {
		//redisTemplate.opsForSet().add(key, value);
		redisTemplate.opsForValue().set(key, value);
		expire(key,isTimeout);
	}

	@SuppressWarnings("unchecked")
	public <V> V getValue(String key, String hashKey) throws CacheException {
		Map<String,Object> map = getDataFromQueue(key);
		if(map!=null){
			try{
				return (V) map.get(hashKey);
			}
			catch(Exception e){
				handlerException(e);
			}
			
		}
		HashOperations<String,String,V> hashOperations = redisTemplate.opsForHash();
		return hashOperations.get(key, hashKey);
	}
	
	
    private void handlerException(Exception e){
    	Throwable throwable = e.getCause();
		if(throwable!=null&&(throwable instanceof JedisConnectionException)){
			/*String host = PropertiesUtil.getProperty("runtime.properties","redis.host");
			String port = PropertiesUtil.getProperty("runtime.properties","redis.port");
			log.error("cant get redis connection:host["+host+"],port["+port+"]");*/
			log.error("can't get current redis connection");
			throw new JedisConnectionException(" current Redis connection failture");
			
		}
    }
    
    
	@SuppressWarnings("unchecked")
	public <V> V getValue(String key) throws CacheException {
		return (V) redisTemplate.opsForValue().get(key);
		
	}
	public Map<String, Object> getHashValue2Map(String key) throws CacheException {
		Map<String,Object> map = getDataFromQueue(key);
		if(map==null){
			try{
				HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
				map =  hashOperations.entries(key);
			}
			catch(Exception e){
				handlerException(e);
			}
		}
		return map;
		
	}
	public <V> List<V> getHashValue2Collection(String key) throws CacheException {
		Map<String,Object> cacheData = getDataFromQueue(key);
		if(cacheData!=null){
			Map<String,V> map = (Map<String,V>) cacheData;
			List<V> list = new ArrayList<V>();
			for(Entry<String,V> entry:map.entrySet()){
				list.add(entry.getValue());
			}
			return list;
		}
		else{
			HashOperations<String, String, V> hashOperations = redisTemplate.opsForHash();
			return (List<V>) hashOperations.values(key);
		}
	}
	public void removeByKey(String key) throws CacheException {
		redisTemplate.delete(key);
	}

	public boolean hasKey(String key)throws CacheException {
		return redisTemplate.hasKey(key);
	}
	private Map<String,Object> getDataFromQueue(String key){
		if(mapQueue.isEmpty()){
			return null;
		}
		Iterator<Map<String, Object>> iterator = mapQueue.iterator();
		while(iterator.hasNext()){
			Map<String, Object> meta = iterator.next();
			Object cacheData = meta.get(key);
			if(cacheData==null||!(cacheData instanceof Map)){
				continue;
			}
			Map<String,Object> map = (Map<String,Object>) cacheData;
			return map;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private class CacheInputHandler extends Thread{
		public void run() {
			do{
				if(mapQueue.isEmpty()){
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						throw new CacheException("handler cache map queue interrupted exception",e);
					}
				}
				putMap2Cache();
			}
			while(true);
		}
		private void putMap2Cache(){
			Map<String, Object> metaMap = mapQueue.peek(); //在不移除的情况下，返回对头 
			if(metaMap==null||metaMap.isEmpty()){
				return;
			}
			for(Entry<String,Object> entry:metaMap.entrySet()){
				if(log.isDebugEnabled()){
					log.debug("insert cache key:"+entry.getKey());
				}
				String[] keys=StringUtils.splitByWholeSeparator(entry.getKey(),"^|^");
				String key = keys[0];
				String timeout= keys[1];
				Map<Object,Object> value = (Map<Object, Object>) entry.getValue();
				try{
					redisTemplate.opsForHash().putAll(key, value);
				}
				catch(Exception e){
					handlerException(e);
					break;
				}
				if(!timeout.equals("-1")){
					expire(key, Long.valueOf(timeout));
				}
				cacheKeys.remove(key);
				if(log.isDebugEnabled()){
					log.debug("remove cache key["+key+"] from cacheKeys container");
				}
			}
			mapQueue.remove(metaMap);//删除队列头部元素
			
		}
	}
	public void rename(String oldKey, String newKey){
		boolean hasKey = redisTemplate.hasKey(oldKey);
		if(!hasKey){
			throw new CacheException("oldKey["+oldKey+"] not contain redis cache");
		}
		redisTemplate.rename(oldKey, newKey);
	}
	public void afterPropertiesSet() throws Exception {
		Thread t2 = new Thread(new CacheInputHandler());
		t2.setName("DRCache-PUTMAP");
		t2.setDaemon(true);
		t2.start();
	}

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        if(args.length < 3) {
            System.err.println("usage: <hostname> <port> <table> [<key>]");
        }
        RedisTemplate redisTemp = new RedisTemplate();
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(args[0]);
        jedisConnectionFactory.setPort(Integer.parseInt(args[1]));
        jedisConnectionFactory.afterPropertiesSet();
        redisTemp.setConnectionFactory(jedisConnectionFactory);
        redisTemp.afterPropertiesSet();
        if(args.length == 3) {
            Map<Object, Object> kvs = redisTemp.opsForHash().entries(args[2]);
            for(Entry<Object, Object> entry : kvs.entrySet()) {
                String key = (String) entry.getKey();
                Object val = entry.getValue();
                System.out.println(key + ": " + val);
            }
        } else {
            Object val = redisTemp.opsForHash().get(args[2], args[3]);
            System.out.println(val);
        }

    }

}
