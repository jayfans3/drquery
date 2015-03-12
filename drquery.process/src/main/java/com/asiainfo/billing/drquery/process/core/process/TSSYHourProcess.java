package com.asiainfo.billing.drquery.process.core.process;

import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * 投诉释疑处理流程
 * @author wangkai8
 *
 */
@SuppressWarnings("unchecked")
public class TSSYHourProcess extends TSSYDayProcess{

    private final static Log log = LogFactory.getLog(TSSYHourProcess.class);


	/**
	 * 投诉释疑流程入口
	 * 
	 */
//	@Override
//	public BaseDTO pagingProcess(CommonDRProcessRequest request) throws ProcessException, BusinessException {
//		//...
//		//process(request, busiMeta);
//
//		return null;
//	}
	
	
	@Override
	public DRProcessDTO process(CommonDRProcessRequest request, MetaModel busiMeta,Map extendParams) throws ProcessException, BusinessException {
		//查询数据
//		List<String[]> modelList = loadData(request, busiMeta, false);
//
//		//数据取出来之后做相应处理，如汇总
//        String interfaceType = request.getInterfaceType();
//		Object[] result = null;
//		if("F15".equals(interfaceType)){//2.1.5	某个时段某个应用某日流量使用情况
//            result = dealCommonRequestF15(modelList, busiMeta.getModelId(), request);
//        }
//		
//		//数据处理完之后，
//		DRProcessDTO dto = new DRProcessDTO();
//		dto.setStats(new HashMap());
//		dto.setdRModels((List)result[0]);
//		return dto;
		return null;
	}

    //处理2.1.5	某个时段某个应用某日流量使用情况
    private Object[]  dealCommonRequestF15(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
		long endTime = System.currentTimeMillis();
		log.info("sort requestF15 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        Map<String,String> everyHourSumFlow = new HashMap<String, String>(); //key=APP_ID,value=1000M
        long oneAppAllHoursSumFlow = 0L;

        String appCodeParam = request.getAppCode();

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            //String appCode = mapRecord.get("APP_ID");
            //if(appCode!=null && appCode.equals(appCodeParam)){
                String currItemFlowStr = mapRecord.get("GPRS_FLOW");
                oneAppAllHoursSumFlow += Long.parseLong(currItemFlowStr);
                String hourStr = mapRecord.get("MOMENT_ID");
                String preItemFlow = everyHourSumFlow.get(hourStr);
                if(preItemFlow==null || preItemFlow.trim().equals("")){
                    everyHourSumFlow.put(hourStr,currItemFlowStr);
                }
           // }
        }

        for(Iterator<String> it = everyHourSumFlow.keySet().iterator();it.hasNext();){
            String hourStr = it.next();
            String appFlowStr = everyHourSumFlow.get(hourStr);
            double appFlow = Double.valueOf(appFlowStr);
            double NoScaleFlowRate = appFlow/oneAppAllHoursSumFlow;
            double flowRate = new BigDecimal(NoScaleFlowRate).setScale(4,BigDecimal.ROUND_CEILING).doubleValue();
            String flowRateStr = String.valueOf(flowRate);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("hour",hourStr);
            retMap.put("appDayFlow",appFlowStr);
            retMap.put("flowRate",decimalFillLength(flowRateStr,4,"0"));
            retData.add(retMap);
        }
        endTime = System.currentTimeMillis();
        log.info("requestF15 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData};
    }

	
}
