import com.asiainfo.billing.drquery.cache.redis.*;
import com.asiainfo.billing.drquery.cache.support.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lile3
 * Date: 13-12-13
 * Time: 下午4:38
 * To change this template use File | Settings | File Templates.
 */
public class RedisTest {

    	public static void main(String[] args){
		test1();
	}

	public static void test1(){
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:drquery.service/spring/*.xml");
		RedisCache cache = (RedisCache)context.getBean("redisCache");
		RedisSwitch redisSwitch = (RedisSwitch)context.getBean("redisSwitch");
		redisSwitch.checkRedisStatus();
//		String key = "privTest";
//		Map map1 = Collections.singletonMap("8001", "test1");
//		Map map2 = Collections.singletonMap("8002", "test2");
//		Map map3 = Collections.singletonMap("8003", "test3");
//		cache.putData2Cache(key, map1, 30);
//		cache.putData2Cache(key, map2, 30);
//		cache.putData2Cache(key, map3, 30);
//		System.out.println("done!");

        Map<String,Object> map = cache.getHashValue2Map("DIM_APP");
        for(Iterator<String> it = map.keySet().iterator();it.hasNext();){
           String key = it.next();
           List<Map<String,String>> list = (List)map.get(key);
           for(int i=0; i<list.size();i++){
               Map<String,String> valueMap =list.get(i);
               StringBuffer buf = new StringBuffer();
               for(Iterator<String> iter = valueMap.keySet().iterator();iter.hasNext();){
                 String key2 = iter.next();
                 String value2  =  valueMap.get(key2);
                 buf.append(key2).append(":").append(value2).append(",");
               }
               System.out.println(buf.toString());
           }
        }
	}
}
