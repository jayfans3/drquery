package com.asiainfo.billing.drquery.process.core.request;

import com.asiainfo.billing.drquery.utils.BeanToMap;

import java.util.Map;


/**
 * 常用字段查询处理参数
 * 
 * @author tianyi
 */
public class CommonDRProcessRequest extends DRProcessRequest {

	public Map<String, Object> ToMap(){
		return BeanToMap.copyProperties(this);
	}
	
	public String getProcessType(){
		String interfaceType = this.getInterfaceType();
		String processType = null;
		if("F11".equals(interfaceType) || "F12".equals(interfaceType)){
			processType = "GPRSFLOW";	//对应busiMapping里modelId
		}
		return processType;
	}
}
