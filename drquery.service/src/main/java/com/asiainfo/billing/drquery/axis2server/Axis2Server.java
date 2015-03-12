/**
 * 
 */
package com.asiainfo.billing.drquery.axis2server;

import net.sf.json.JSONObject;

import com.asiainfo.billing.drquery.service.WebServiceQuery;

/**
 * @author liujs3
 *
 */
public class Axis2Server extends Axis2ServerAdaptor {

	//用户流量消费行为单维度汇总
	//
	/**
	 * @param qryId 一次查询
	 * @param phoneNo 手机号码十一位手机号码格式
	 * @param startTime 开始时间YYYYMMDDHH24MMSS
	 * @param endTime 结束时间
	 * @param groupColumnCode 分类字段  按照应用名称分类，dvaop则其值为appName;按照一级域名分类，则其值为mainDomain
	 * @param topNum Top数 
	 * @param opId 操作员工号
	 * @param opName 操作员名称
	 * @param srcSystemCode 源系统编码 eg.CMOD
	 * @return groupValue groupTotalFlow 分类的列对应的值。如果按照应用分类：则其值为各个应用的名称。如：微信如果按照一级域名分类：则其值为具体的一级域名。如：qq.comgroupTotalFlow: 如果按照应用分类，则表示一个应用消耗的流量和。如果按照一级域名分类，则表示一个网站的流量和。单位：KB groupCount:当前分类下，流量消费行为数据的记录数表示查询的结果又多少个分类。如按应用分类，则表示有多少个应用产生了流量。totalFlow:用户在查询时段内总共使用的流量。单位：KB
	 * 本接口根据双方约定的需求进行制定。需求简述为：数据提供方向数据接收方提供用户流量消费行为按照应用名称或者一级域名汇总的服务接口，供数据接收方查询调用；数据源为Gn口经过分析增强的GPRS用户流量消费行为详单数据。
     * 数据提供方不要求数据接收方访问接口服务时进行鉴权和认证。
     * 数据提供方记录数据接收方的查询请求。记录信息包含有：查询入参参数（见第2.1.2章节“入参参数列表”）、查询请求时间、查询返回时间、查询结果状态、总返回条数、	分页返回条数。
     */
	public String getDimensionSummary(String json){
		JSONObject jo=JSONObject.fromObject(json);
//		JSONObject joo=jo.getJSONObject("qryCond");
		String rs=WebServiceQuery.query(jo.toString(), "F11");
//		String jsonStr = "{\n" +
//        "    \"qryId\": \"1111111122\",\n" +
//        "    \"phoneNo\": \"18601134210\",\n" +
//        "    \"startTime\": \"20131011200000\",\n" +
//        "    \"endTime\": \"20131011225959\",\n" +
//        "    \"groupColumnCode\": \"appName\",\n" +
//        "    \"topNum\": \"20\",\n" +
//        "    \"opId\": \"11111\",\n" +
//        "    \"opName\": \"张三\",\n" +
//        "    \"srcSystemCode\": \"CMOD\"\n" +
//        "}";
//		String json=WebServiceQuery.query(jsonStr, "F11");
		return rs;
	}
	
	//用户流量消费行为详单查询接口
	/**
	 * @param qryId
	 * @param phoneNo
	 * @param startTime
	 * @param endTime
	 * @param appName
	 * @param mainDomain
	 * @param startIndex
	 * @param offset
	 * @param orderColumnCode
	 * @param orderFlag
	 * @param opId
	 * @param opName
	 * @param srcSystemCode
	 * @return termModeName:终端型号名称 startTime appName:应用的名称 appType:应用的类别。直接返回可在前台展示的结果。 mainDomain:qq.com fullDomain:conf.3g.qq.com detailURL:URL flow:KB
	 * 本接口根据双方约定的需求进行制定。需求简述为：数据提供方向数据接收方提供用户流量消费行为查询服务接口，供数据接收方查询调用；数据源为Gn口经过分析增强的GPRS用户流量消费行为详单数据。
     * 数据提供方不要求数据接收方访问接口服务时进行鉴权和认证。
     * 数据提供方记录数据接收方的查询请求。记录信息包含有：查询入参参数（见第2.2.2章节“入参参数列表”）、查询请求时间、查询返回时间、查询结果状态、总返回条数、分页返回条数。
     */
	public String getDetail(String json){
		JSONObject jo=JSONObject.fromObject(json);
//		JSONObject joo=jo.getJSONObject("qryCond");
//		jo.put("qryId",qryId);
//		jo.put("phoneNo",phoneNo);
//		jo.put("startTime",startTime);
//		jo.put("endTime",endTime);
//		jo.put("appName",appName);
//		jo.put("mainDomain",mainDomain);
//		jo.put("startIndex",startIndex);
//		jo.put("offset",offset);
//		jo.put("orderColumnCode",orderColumnCode);
//		jo.put("orderFlag",orderFlag);
//		jo.put("opId",opId);
//		jo.put("opName",opName);
//		jo.put("srcSystemCode",srcSystemCode);
	String rs=WebServiceQuery.query(jo.toString(), "F12");
//	System.out.println(rs.toString());
//	String jsonStr = "{\n" +
//			"    \"qryId\": \"1111111122\",\n" +
//			"    \"phoneNo\": \"18601134210\",\n" +
//			"    \"startTime\": \"20131011200000\",\n" +
//			"    \"endTime\": \"20131011225959\",\n" +
//			"    \"groupColumnCode\": \"appName\",\n" +
//			"    \"topNum\": \"20\",\n" +
//			"    \"opId\": \"11111\",\n" +
//			"    \"opName\": \"张三\",\n" +
//			"    \"srcSystemCode\": \"CMOD\"\n" +
//			"}";
//	String json=WebServiceQuery.query(jsonStr, "F11");
	return rs;
	}  
}
