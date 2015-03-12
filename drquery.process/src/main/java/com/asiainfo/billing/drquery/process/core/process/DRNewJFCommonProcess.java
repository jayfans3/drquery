package com.asiainfo.billing.drquery.process.core.process;

import com.asiainfo.billing.drquery.Constants;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.exception.DrqueryRuntimeException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.DRCommonProcess;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.utils.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class DRNewJFCommonProcess extends DRCommonProcess{
	
	private final static Log log = LogFactory.getLog(DRNewJFCommonProcess.class);	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DRProcessDTO process(CommonDRProcessRequest request, MetaModel busiMeta) throws ProcessException, BusinessException {		
		 List<Map<String, String>> modelList = new ArrayList<Map<String, String>>();
		
		modelList =null;//loadData(request, busiMeta, false);
		
		Object[] result = null;
		if("F21".equals(request.getInterfaceType())){
			result = processData(modelList, request, busiMeta);
		}
		
		DRProcessDTO dto = new DRProcessDTO();	
		
		dto.setStats(new HashMap());
		dto.setdRModels((List)result[0]);
		
		return dto;
	}


	

	/**
	 * 此方法用途：
	 * 		1. 转化成统一结构体
	 * 		2. 剔重、合并、汇总
	 * @param retArr
	 * @param request
	 * @param isGbp	是否是集团详单
	 * @return
	 */
	@SuppressWarnings({"rawtypes" })
	public Object[] processData(List<Map<String, String>> retArr, CommonDRProcessRequest request, MetaModel busiMeta){
		long beginTime = System.currentTimeMillis();
		
		
		long endTime = System.currentTimeMillis();
		log.info("sort records token: " + (endTime - beginTime) + "ms");
		Map<String, Integer> repeatCheckMap = new HashMap<String, Integer>();
		Map<String, Integer> mergeCheckMap = new HashMap<String, Integer>();
		List<Map<String, String>> retData = new ArrayList<Map<String, String>>();
		
		List sumList = new ArrayList();
		
		int index = 0;
		try {
			beginTime = System.currentTimeMillis();
			for(int i=0; i < retArr.size(); i++){
				Map<String, String> record = retArr.get(i);		
				
				boolean isDisincted = distinct.distinct(busiMeta, retData, record, repeatCheckMap, index);
				boolean isMerged = false;
				if(!isDisincted){
					isMerged = merge.merge(busiMeta, retData, record, mergeCheckMap, index);
					if(isMerged){
						LogUtils.debug(log, "merge record: " + record);
					}
				}else{
					LogUtils.debug(log, "distinct record: " + record);
				}
				if(!isDisincted && !isMerged){
					applyExtendValue(record);
					retData.add(record);
					index ++;
				}
			}
			
			endTime = System.currentTimeMillis();
			
			log.info("distinct and merge token: " + (endTime - beginTime) + "ms");
			log.info("After distinct and merge, there is " + retData.size() + " records remain.");
			
			if(StringUtils.isNotEmpty(request.getSortName())){
				sortByFieldName(retData, request.getSortName(), request.getSortDir());
			}
			
			retArr = null;
		} catch(Exception e){
			throw new DrqueryRuntimeException(e);
	    }
		if(LogUtils.isDebug()){
			LogUtils.debug(log, "below is sum info: ");
		}
		
		Object[] result = new Object[]{retData, sumList};
		return result;
	}
	
	
	//加入额外字段信息
	public void applyExtendValue(Map<String, String> record){
		record.put(Constants.ONLIE_TIME, "");
		record.put(Constants.OFFLIE_TIME, "");
		record.put(Constants.FLOW_FEE, "");
		record.put(Constants.DISCOUNT_PLAN_CODE, "");
		record.put(Constants.DURATION, "");
	}
	
	
	public void sortByFieldName(List<Map<String, String>> records, final String sortName, String sortDir){
		final int  asc = "ASC".equals(sortDir.toUpperCase()) ? 1 : -1;
		Collections.sort(records, new Comparator<Map<String, String>>(){

			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				if(!o1.containsKey(sortName) || !o2.containsKey(sortName)){
					log.warn("sort fieldName: '" + sortName + "' is not exsit in the result.");
					return 0;
				}
				
				String value1 = o1.get(sortName);
				String value2 = o2.get(sortName);
				
				int maxLen = Math.max(value1.length(), value2.length());
				
				if(value1.length() < maxLen){
					value1 = formatStringWithZero(value1, maxLen);
				}else if(value2.length() < maxLen){
					value2 = formatStringWithZero(value2, maxLen);
				}
				
				return value1.compareTo(value2) * asc;
			}
			
		});
	}
    
	
	public String formatStringWithZero(String str, int len){
		for(int i = str.length(); i <= len; i++){
			str = "0" + str;
		}
		return str;
	}

//    private List<Map> filterIndtifyRecodeId(List<Map> modelList, String identifyRecordCode) {
//      List<String[]> identifyRecordList = new ArrayList<String[]>();
//      if(identifyRecordCode == null || identifyRecordCode.trim().equals("")) return modelList;
//      for(List<Map> oneOnlineRecord:modelList){
//          String currRecordCode = oneOnlineRecord[1];
//          if(currRecordCode.equals(identifyRecordCode)){
//              identifyRecordList.add(oneOnlineRecord);
//          }
//      }
//      return identifyRecordList;
//    }
}
