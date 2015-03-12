import com.asiainfo.billing.drquery.service.*;
import com.asiainfo.billing.drquery.utils.*;
import net.sf.json.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * Created by IntelliJ IDEA.
 * User: lile3
 * Date: 13-12-9
 * Time: 下午4:47
 * To change this template use File | Settings | File Templates.
 */
public class WebServiceQueryTest {

    public static void main(String[] args) {
        //BeanFactory context = new ClassPathXmlApplicationContext("classpath*:drquery.service/spring/*.xml");
        //ServiceLocator.beanFactory = context;

        JSONObject joo=new JSONObject();
        JSONObject jo=new JSONObject();
		jo.put("qryId","1111111122");
		jo.put("phoneNo","14701225913");
		jo.put("startTime","20131001000000");
		jo.put("endTime","20131030235900");
		jo.put("groupColumnCode","appId");
		jo.put("topNum","20");
		jo.put("opId","11111");
		jo.put("offset","11");
		jo.put("opName","张三");
		jo.put("srcSystemCode","CMOD");
		joo.put("qryCond", jo);
        String jsonStr = joo.toString();
        //String resultJson = WebServiceQuery.query(jsonStr, "F11");
        System.out.println("F11 return json str = " + jsonStr);


        JSONObject joo2=new JSONObject();
        JSONObject jo2=new JSONObject();
		jo2.put("qryId","1111111123");
		jo2.put("phoneNo","14701225913");
		jo2.put("startTime","20131001000000");
		jo2.put("endTime","20131030235959");
		jo2.put("appId","0");
		jo2.put("startIndex","1");
        jo2.put("offset","5");
        jo2.put("orderColumnCode","startTime");
        jo2.put("orderFlag","desc");
		jo2.put("opId","11111");
		jo2.put("opName","李四");
		jo2.put("srcSystemCode","CMOD");
		joo2.put("qryCond", jo2);
        jsonStr = joo2.toString();
        //resultJson = WebServiceQuery.query(jsonStr, "F12");
        System.out.println("F12 return json str = " + jsonStr);
    }
}
