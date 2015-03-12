package com.asiainfo.billing.drquery.process.core;

import java.util.Map;

import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.request.DRQueryTypesProcessRequest;

/**
 * 用户业务类型处理类
 * 
 * @author Rex Wong
 *
 * @version
 */
public interface BussinessTypesProcess {
	/**
	 * 返回用户业务类型
	 * @return
	 */
	Map<String,String> queryTypes(DRQueryTypesProcessRequest request) throws ProcessException;
}
