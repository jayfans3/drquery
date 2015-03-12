package com.asiainfo.billing.drquery.cache;

import java.util.List;
import java.util.Map;

import com.asiainfo.billing.drquery.cache.support.CacheParameters.Range;


/**
 * 
 * @author Rex Wong
 * @version
 */
public interface ICache {
	
	/**
	 * 分页查询缓存中的数据
	 * @return
	 * @throws KeyValueDataAccessException
	 */
	<V> List<V> getValue(String key, Range range) throws CacheException;
	
	/**
	 * 存入缓存，存入数据为Redis map形式
	 * @param key 存入缓存数据的key
	 * @param value 存入缓存数据
	 * @param isTimeout 是否设置缓存超时。<code>true</code>:设置超时时间，当超过指定时间该key值下的缓存将被清除。
	 * @throws CacheException
	 */
	void putData2Cache(String key,Map<String,Object> value,boolean isTimeout) throws CacheException;
	
	void putData2Cache(String key,Map<String,Object> value,long timeout) throws CacheException;
	
	/**
	 * 存入缓存，存入数据为Redis set形式 
	 * @param key
	 * @param value
	 * @param isTimeout 是否设置缓存超时
	 */
	<V> void putData2Cache(String key,V value,boolean isTimeout) throws CacheException;
	/**
	 * @param key
	 * @param hashKey
	 * @return
	 */
	<V> V getValue(String key, String hashKey) throws CacheException;
	/**
	 * @param key
	 * @return
	 */
	<V> V getValue(String key) throws CacheException;
	
	void removeByKey(String key) throws CacheException;
	
	boolean hasKey(String key) throws CacheException;
	
	/**
	 * 存入缓存<p/>
	 * 最终存入的是Redis map数据，其中HashKey值是List的索引
	 * @param key 存入缓存数据的key
	 * @param value 存入缓存数据，List类型数据，最终将其的index作为hashkey,以HashMap的形式存储缓存
	 * @param isTimeout 是否设置缓存超时。<code>true</code>:设置超时时间，当超过指定时间该key值下的缓存将被清除。
	 * @throws CacheException
	 */
	<V> void putData2Cache(String key, List<V> value,boolean isTimeout) throws CacheException;
	
	/**
	 * 得到map所有数据</p>
	 * 注意：该方法需慎用，因为redis会遍历该map，期间不响应其他请求，严重影响性能
	 * @param key
	 * @return
	 * @throws CacheException
	 */
	Map<String, Object> getHashValue2Map(String key) throws CacheException;
	
	<V> List<V> getHashValue2Collection(String key) throws CacheException;
	
	/**
	 * 重置key值
	 * 
	 * @param oldKey
	 * @param newKey
	 */
	void rename(String oldKey, String newKey);
}
