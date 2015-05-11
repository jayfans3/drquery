package com.asiainfo.billing.drquery.axis2server;
import javax.xml.namespace.QName;

import net.sf.json.JSONObject;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.rpc.client.RPCServiceClient;

public class Axis2 {

	public  static void main(String args[]) throws AxisFault {
		 //RPCServiceClient是RPC方式调用
        RPCServiceClient client = new RPCServiceClient();
        Options options = client.getOptions();
        //TODO 设置调用WebService的URL
        options.setTimeOutInMilliSeconds(1000000);
//        String address = "http://10.25.124.111:8081/drquery.service-2.0-hb/services/Axis2ServerAdaptor";
        String address = "http://127.0.0.1:8080/drquery.service-2.0-bj/services/Axis2ServerAdaptor";
        EndpointReference epf = new EndpointReference(address);
        options.setTo(epf);
       
        /**
        * 设置将调用的方法，http://ws.apache.org/axis2是方法
        * 默认（没有package）命名空间，如果有包名
        * 就是http://service.hoo.com 包名倒过来即可
        * sayHello就是方法名称了
        */
        //TODO 指定调用的方法和传递参数数据
        QName qname = new QName("http://axis2server.drquery.billing.asiainfo.com", "getAxis2Result");
//        String a="张三?@#$%^&*\\/";
        JSONObject joo=new JSONObject();
        JSONObject jo=new JSONObject();
		jo.put("qryId","1111111122");
//		jo.put("phoneNo","13407290650");
        jo.put("phoneNo","13627115351");
		jo.put("startTime","20140301000000");
		jo.put("endTime","20140331235959");
		jo.put("groupColumnCode","appId");
//        jo.put("groupColumnCode","mainDomain");
		jo.put("topNum","10");
		jo.put("opId","11111");
		jo.put("offset","11");
		jo.put("opName","张三");
		jo.put("srcSystemCode","CMOD");
		joo.put("qryCond", jo);

        JSONObject joo2=new JSONObject();
        JSONObject jo2=new JSONObject();
		jo2.put("qryId","1111111123");
//		jo2.put("phoneNo","13407290650");
        jo2.put("phoneNo","15871080171");
//        jo2.put("phoneNo","15871080000");
		jo2.put("startTime","20140201000000");
		jo2.put("endTime","20140331235959");
//		jo2.put("mainDomain","shouji.com.cn");
//        if(args.length == 4){
//          jo2.put("appId",args[3]);
//        }
        jo2.put("appId","");
		jo2.put("startIndex","101");
        jo2.put("offset","100");
//        jo2.put("orderColumnCode","flow");
//        jo2.put("orderColumnCode","startTime");
//        jo2.put("orderFlag","asc");
		jo2.put("opId","11111");
		jo2.put("opName","李四");
		jo2.put("srcSystemCode","CMOD");
		joo2.put("qryCond", jo2);



		//TODO 调用及设置返回值的类型
        Object[] result = client.invokeBlocking(qname, new String[] {"F11",joo.toString()}, new Class[] { String.class });
//        Object[] result = client.invokeBlocking(qname, new String[] {"F12",joo2.toString()}, new Class[] { String.class });
        System.out.println(result[0]);
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

}
