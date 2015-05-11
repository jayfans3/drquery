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
//		String interfaceType = this.getInterfaceType();
//		String processType = null;
//		if("F6".equals(interfaceType)){
//			processType = "GPRSFLOW_SUM_VIEW";	//对应viewMapping里modelId
//		} else if("F12".equals(interfaceType)){
//            processType = "GPRSFLOW_DETAIL_VIEW";	//对应viewMapping里modelId
//        } else if("appUpdate".equals(interfaceType) || "detailUpdate".equals(interfaceType)){
//            processType = "GPRSFLOW_UPDATE";	//对应viewMapping里modelId
//        }else {
//            throw new RuntimeException("interfaceType type invalid, require type is F11 or F12, F13, appUpdate, detailUpdate, but found: " + interfaceType);
//        }
//		return processType;
        return this.getInterfaceType();
	}
}
