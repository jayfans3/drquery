package com.asiainfo.billing.drquery.axis2server;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

import javax.xml.namespace.QName;

import net.sf.json.JSONObject;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import com.ailk.oci.ocnosql.client.jdbc.phoenix.PhoenixJdbcHelper;
import com.ailk.oci.ocnosql.common.util.PropertiesUtil;
import com.asiainfo.billing.drquery.utils.JsonUtils;

public class Axis2 {


	private CountDownLatch latch = new CountDownLatch(1);
	private ZooKeeper zk; 
	
	public  static void main(String args[]) throws AxisFault {
		 //RPCServiceClient是RPC方式调用
        RPCServiceClient client = new RPCServiceClient();
        Options options = client.getOptions();
        //TODO 设置调用WebService的URL
//        String address = "http://localhost:8080/drquery.service-2.0-bj/services/Axis2ServerAdaptor";
        String address = args[0];
        EndpointReference epf = new EndpointReference(address);
        options.setTo(epf);
       
        /**
        * 设置将调用的方法，http://ws.apache.org/axis2是方法z
        * 默认（没有package）命名空间，如果有包名
        * 就是http://service.hoo.com 包名倒过来即可
        * sayHello就是方法名称了
        */
        //TODO 指定调用的方法和传递参数数据
//        QName qname = new QName("http://axis2server.drquery.billing.asiainfo.com", "getAxis2Result");
        QName qname = new QName(args[1], args[2]);
//        String a="张三?@#$%^&*\\/";
        
        JSONObject joo=JsonUtils.string2JsonObject(args[6]);
        JSONObject jo=joo.getJSONObject("qryCond");
//		jo.put("qryId","1111111122");
//		jo.put("phoneNo","18310017079");
//		jo.put("startTime","20131001000000");
//		jo.put("endTime","20131030235900");
//		jo.put("groupColumnCode","appId");
//		jo.put("topNum","20");
//		jo.put("opId","11111");
//		jo.put("offset","11");
//		jo.put("opName","张三");
//		jo.put("srcSystemCode","CMOD");

		
        JSONObject joo2=JsonUtils.string2JsonObject(args[7]);
        JSONObject jo2=joo2.getJSONObject("qryCond");
//		jo2.put("qryId","1111111123");
//		jo2.put("startTime","20131001000000");
//		jo2.put("endTime","20131030235959");
//		jo2.put("appId","25006");
//		jo2.put("startIndex","43");
//        jo2.put("offset","-1");
//        jo2.put("orderColumnCode","startTime");
//        jo2.put("orderFlag","desc");
//		jo2.put("opId","11111");
//		jo2.put("opName","李四");
//		jo2.put("srcSystemCode","CMOD");
		
        PhoenixJdbcHelper pjh=new PhoenixJdbcHelper();
        ResultSet rs=null;
		try {
		rs = pjh.executeQueryRaw(args[4]);
        String number=null;
		number = rs.getString("0");
        if(StringUtils.isNotBlank(number)){
        	jo2.put("phoneNo",number);
        	jo.put("phoneNo",number);
        }} catch (SQLException e) {
			e.printStackTrace();
		}
		//TODO 调用及设置返回值的类型
        //Object[] result = client.invokeBlocking(qname, new String[] {"F11",joo.toString()}, new Class[] { String.class });
        Object[] result1 = client.invokeBlocking(qname, new String[] {args[5],joo.toString()}, new Class[] { String.class });
        
        Object[] result2 = client.invokeBlocking(qname, new String[] {args[5],joo2.toString()}, new Class[] { String.class });
        
        /**
		 *   {
				result:0,					//0表示成功，非0表示失败
				resMsg:{
					retCode:0,
					hint:"查询成功",		//错误提示信息
					errorMsg:"查询成功",	//错误堆栈信息
					errorCode:""			//错误编码
				},
				replyDisInfo:[
					 	{
					 		groupValue:"QQ",
							groupTotalFlow:"1.33334444",
			groupRecordCount:”3”
					 	},
						…	// 数据列
			{…}
				],
			stats:{
					 groupCount:"3",
					 totalFlow:"10.3333333333"
			}
			}
         */
	}
	
	public void connect() throws IOException {
		String resourceName = "client-runtime.properties";
	    String quorum = "hbase.zookeeper.quorum";
	    String clientport = "hbase.zookeeper.property.clientPort";
	    String[] urls = PropertiesUtil.getProperty(resourceName, quorum).split(",");
	    String port = PropertiesUtil.getProperty(resourceName, clientport);
	    String hosts = "";
	    int session_timeout=60000;
	    for (String url : urls) {
	      if (StringUtils.isNotBlank(url))
	        hosts = hosts + url + ":" + port + ",";
	    }
	    hosts = hosts.substring(0, hosts.length() - 1);
	    
		zk = new ZooKeeper(hosts, session_timeout, new Watcher() {
			public void process(WatchedEvent event) {
				if(event.getState() == KeeperState.SyncConnected) {
					latch.countDown();
				}
			} 
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
