package com.asiainfo.billing.drquery.process.core.process;

import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.format.datatime.DateFormatter;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.DRCommonProcess;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.utils.LogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * 投诉释疑处理流程
 * @author wangkai8
 *
 */
@SuppressWarnings("unchecked")
public class TSSYDayProcess extends DRCommonProcess{

    private final static Log log = LogFactory.getLog(TSSYDayProcess.class);


	/**
	 * 投诉释疑流程入口
	 * 
	 */
//	@Override
//	public BaseDTO pagingProcess(CommonDRProcessRequest request) throws ProcessException, BusinessException {
//		//process(request, meta, false);
//		return null;
//	}
	
	
	@Override
	public DRProcessDTO process(CommonDRProcessRequest request, MetaModel busiMeta,Map extendParams) throws ProcessException, BusinessException {
//		//查询数据
//		List<String[]> modelList = loadData(request, busiMeta, false);
//
//        //数据取出来之后做相应处理，如汇总
//        Object[] result = null;
//        String interfaceType = request.getInterfaceType();
//        if("F11".equals(interfaceType)){ //2.1.1某个时段所有应用流量使用情况
//            result = dealCommonRequestF11(modelList,busiMeta.getModelId(),request);
//        }else if("F12".equals(interfaceType)){//2.1.2 某个时段某个应用流量使用情况
//            result = dealCommonRequestF12(modelList, busiMeta.getModelId(), request);
//        }else if("F13".equals(interfaceType)){//2.1.3	某个时段每天流量使用情况
//            result = dealCommonRequestF13(modelList, busiMeta.getModelId(), request);
//        }else if("F14".equals(interfaceType)){//2.1.4	某个时段某日流量使用情况
//            result = dealCommonRequestF14(modelList, busiMeta.getModelId(), request);
//        }else if("F16".equals(interfaceType)){//2.1.6	流量总体使用情况
//            result = dealCommonRequestF16(modelList, busiMeta.getModelId(), request);
//        }else if("F17".equals(interfaceType)){//2.1.7	用户信息总体概览
//            result = dealCommonRequestF17(modelList, busiMeta.getModelId(), request);
//        }
//		
//		//数据处理完之后，
//		DRProcessDTO dto = new DRProcessDTO();
//		dto.setStats(new HashMap());
//		dto.setdRModels((List)result[0]);
//        List<Map<String,Object>> sumsList = new ArrayList<Map<String, Object>>();
//        if(result != null && result.length > 1){
//           sumsList = (List)result[1];
//        }
//        dto.setSumList(sumsList);
//		return dto;
		return null;
	}

    public List<String[]> sort(List<String[]> retArr,final int sortIndex){
       	 Collections.sort(retArr, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                String start_time_1 = o1[sortIndex];
                String start_time_2 = o2[sortIndex];
                //比较通话时间
                if (start_time_1.compareTo(start_time_2) > 0) {
                    return 1;
                } else if (start_time_1.compareTo(start_time_2) < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        return retArr;
    }

    //处理2.1.1 某个时段所有应用流量使用情况
    private Object[]  dealCommonRequestF11(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
		long endTime = System.currentTimeMillis();
		log.info("sort requestF11 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        Map<String,String> everyAppSumFlow = new HashMap<String, String>(); //key=APP_ID,value=1000M
        Map<String,String> everyAppAnalyzeType = new HashMap<String,String>();
        long allAppSumFlow = 0L;

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            String currItemFlowStr = mapRecord.get("GPRS_FLOW");
            allAppSumFlow += Long.parseLong(currItemFlowStr);
            String analyzeType = mapRecord.get("ANALYZE_TYPE");
            String appId = mapRecord.get("APP_ID");
            everyAppAnalyzeType.put(appId,analyzeType);
            String preItemFlow = everyAppSumFlow.get(appId);
            if(preItemFlow==null || preItemFlow.trim().equals("")){
             everyAppSumFlow.put(appId, currItemFlowStr);
            }else{
             String newAppFlowStr = String.valueOf(Long.parseLong(preItemFlow)+Long.parseLong(currItemFlowStr));
             everyAppSumFlow.put(appId, newAppFlowStr);
            }
        }

        log.info("everyAppSumFlow size is : " + everyAppSumFlow.size());

        for(Iterator<String> it = everyAppSumFlow.keySet().iterator();it.hasNext();){
            String appId = it.next();
            String appFlowStr = everyAppSumFlow.get(appId);
            double appFlow = Double.valueOf(appFlowStr);
            double NoScaleFlowRate = appFlow/allAppSumFlow;
            double flowRate = new BigDecimal(NoScaleFlowRate).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
            String flowRateStr = String.valueOf(flowRate);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("appCode",appId);
            retMap.put("analyzeType",everyAppAnalyzeType.get(appId));
            retMap.put("appFlow",appFlowStr);
            retMap.put("flowRate",decimalFillLength(flowRateStr,4,"0"));
            log.info("data info: " + "appCode is " + appId + ";appFlow is " + appFlowStr + ";flowRate is " + flowRateStr);
            retData.add(retMap);
        }
        endTime = System.currentTimeMillis();
        log.info("requestF11 stat records token: " + (endTime - beginTime) + "ms");
        log.info("retData size is : " + retData.size());
        return new Object[]{retData};
    }

    //处理2.1.2 某个时段某个应用流量使用情况
    private Object[]  dealCommonRequestF12(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
        long endTime = System.currentTimeMillis();
        log.info("sort requestF12 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        Map<String,String> everyDateSumFlow = new HashMap<String, String>(); //key=date,value=1000M
        long allDateSumFlow = 0L;
        String retAppCode = "";
        String retAnalyzeType = "";

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            String currItemFlowStr = mapRecord.get("GPRS_FLOW");
            String data_date = mapRecord.get("DATA_DATE");
            String appCode = mapRecord.get("APP_ID");
            String preItemFlow = everyDateSumFlow.get(data_date);
            if(appCode != null && appCode.equalsIgnoreCase(request.getAppCode())){
                allDateSumFlow += Long.parseLong(currItemFlowStr);
                retAppCode = appCode;
                retAnalyzeType = mapRecord.get("ANALYZE_TYPE");
                if(preItemFlow==null || preItemFlow.trim().equals("")){
                    everyDateSumFlow.put(data_date,currItemFlowStr);
                }else{
                    String newDateFlowStr = String.valueOf(Long.parseLong(preItemFlow)+Long.parseLong(currItemFlowStr));
                    everyDateSumFlow.put(data_date,newDateFlowStr);
                }
            }
        }

        for(Iterator<String> it = everyDateSumFlow.keySet().iterator();it.hasNext();){
            String data_date = it.next();
            String dateFlowStr = everyDateSumFlow.get(data_date);
            double dateFlow = Double.valueOf(dateFlowStr);
            double NoScaleFlowRate = dateFlow/allDateSumFlow;
            double flowRate = new BigDecimal(NoScaleFlowRate).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
            String flowRateStr = String.valueOf(flowRate);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("date",data_date);
            retMap.put("appFlow",dateFlowStr);
            retMap.put("flowRate",decimalFillLength(flowRateStr,4,"0"));
            retMap.put("appCode",retAppCode);
            retMap.put("analyzeType",retAnalyzeType);
            retData.add(retMap);
        }
        endTime = System.currentTimeMillis();
        log.info("requestF12 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData};
    }

    //处理2.1.3	某个时段每天流量使用情况
    private Object[]  dealCommonRequestF13(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
        long endTime = System.currentTimeMillis();
        log.info("sort requestF13 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        Map<String,String> everyDateSumFlow = new HashMap<String, String>(); //key=Date,value=1000M
        long allDateSumFlow = 0L;

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            String currItemFlowStr = mapRecord.get("GPRS_FLOW");
            allDateSumFlow += Long.parseLong(currItemFlowStr);
            String data_date = mapRecord.get("DATA_DATE");
            String preItemFlow = everyDateSumFlow.get(data_date);
            if(preItemFlow==null || preItemFlow.trim().equals("")){
                everyDateSumFlow.put(data_date,currItemFlowStr);
            }else{
                String newDateFlowStr = String.valueOf(Long.parseLong(preItemFlow)+Long.parseLong(currItemFlowStr));
                everyDateSumFlow.put(data_date,newDateFlowStr);
            }
        }

        for(Iterator<String> it = everyDateSumFlow.keySet().iterator();it.hasNext();){
            String data_date = it.next();
            String dateFlowStr = everyDateSumFlow.get(data_date);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("date",data_date);
            retMap.put("appFlow",dateFlowStr);
            retData.add(retMap);
        }
        endTime = System.currentTimeMillis();
        log.info("requestF13 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData};
    }

    //处理2.1.4	某个时段某日流量使用情况
    private Object[]  dealCommonRequestF14(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
        long endTime = System.currentTimeMillis();
        log.info("sort requestF14 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        long allAppsSumFlow = 0L;

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("date",request.getFromDate());
            retMap.put("appCode",mapRecord.get("APP_ID"));
            retMap.put("analyzeType",mapRecord.get("ANALYZE_TYPE"));
            String appFlowStr = mapRecord.get("GPRS_FLOW");
            retMap.put("dayFlow",appFlowStr);
            allAppsSumFlow += Long.parseLong(appFlowStr);
            retData.add(retMap);
        }
        for(Map<String,String> map : retData){
            String dayFlowStr = map.get("dayFlow");
            double dayFlow = Double.valueOf(dayFlowStr);
            double NoScaleFlowRate = dayFlow/allAppsSumFlow;
            double flowRate = new BigDecimal(NoScaleFlowRate).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
            String flowRateStr = String.valueOf(flowRate);
            map.put("flowRate",decimalFillLength(flowRateStr,4,"0"));
        }
        endTime = System.currentTimeMillis();
        log.info("requestF14 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData};
    }

    //处理2.1.6	流量总体使用情况
    private Object[]  dealCommonRequestF16(List<String[]> retArr,String modelId,CommonDRProcessRequest request) throws ProcessException {
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
        long endTime = System.currentTimeMillis();
        log.info("sort requestF16 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        Map<String,String> everyAppTypeSumFlow = new HashMap<String, String>(); //key=Date,value=1000M
        Set<String> appSet = new HashSet<String>();
        long allAppTypesSumFlow = 0L;

        beginTime = System.currentTimeMillis();
        //计算开始日期和结束日期间隔天数
        String startDateStr = request.getFromDate();
        String endDateStr = request.getThruDate();
        DateFormatter formatter = new DateFormatter("yyyyMMdd");
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = formatter.parse(startDateStr, Locale.getDefault());
            endDate = formatter.parse(endDateStr,Locale.getDefault());
        } catch (ParseException e) {
            throw new ProcessException("startDate or endDate format is error ."+ " startDate is " + startDateStr
                    + "endDate is " + endDateStr);
        }

        int daysBetween = daysBetween(startDate,endDate);
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            String currItemFlowStr = mapRecord.get("GPRS_FLOW");
            allAppTypesSumFlow += Long.parseLong(currItemFlowStr);
            String appId = mapRecord.get("APP_ID");
            if(!appSet.contains(appId)){
                appSet.add(appId);
            }
            String appType = mapRecord.get("APP_TYPE");
            String preItemFlow = everyAppTypeSumFlow.get(appType);
            if(preItemFlow==null || preItemFlow.trim().equals("")){
                everyAppTypeSumFlow.put(appType,currItemFlowStr);
            }else{
                String newDateFlowStr = String.valueOf(Long.parseLong(preItemFlow)+Long.parseLong(currItemFlowStr));
                everyAppTypeSumFlow.put(appType,newDateFlowStr);
            }
        }

        List<Map<String,String>> sumsList =  new ArrayList<Map<String, String>>();
        Map<String,String> otherDataMap = new HashMap<String, String>();
        otherDataMap.put("totalFlow",String.valueOf(allAppTypesSumFlow));
        otherDataMap.put("dayAvgFlow",String.valueOf(allAppTypesSumFlow/(daysBetween+1)));
        otherDataMap.put("appNum",String.valueOf(appSet.size()));
        sumsList.add(otherDataMap);


        for(Iterator<String> it = everyAppTypeSumFlow.keySet().iterator();it.hasNext();){
            String appType = it.next();
            String appTypeFlowStr = everyAppTypeSumFlow.get(appType);
            double appTypeFlow = Double.valueOf(appTypeFlowStr);
            double noScaleFlowRate = appTypeFlow/allAppTypesSumFlow;
            double flowRate = new BigDecimal(noScaleFlowRate).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
            String  flowRateStr = String.valueOf(flowRate);
            Map<String,String> retMap = new HashMap<String, String>();
            retMap.put("appType",appType);
            retMap.put("appFlow",appTypeFlowStr);
            retMap.put("flowRate",decimalFillLength(flowRateStr,4,"0"));
            retData.add(retMap);
        }
        endTime = System.currentTimeMillis();
        log.info("requestF16 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData,sumsList};
    }

    //处理2.1.7	用户信息总体概览
    private Object[]  dealCommonRequestF17(List<String[]> retArr,String modelId,CommonDRProcessRequest request){
        long beginTime = System.currentTimeMillis();
        List<String[]> sortedList = sort(retArr,0);
        long endTime = System.currentTimeMillis();
        log.info("sort requestF17 records token: " + (endTime - beginTime) + "ms");
        //统计逻辑
        List<Map<String,String>> retData = new ArrayList<Map<String, String>>();
        long allSumFlow = 0L;

        beginTime = System.currentTimeMillis();
        for(int i=0; i<sortedList.size(); i++){
            String[] arrRecord = sortedList.get(i);
            Map<String,String> mapRecord = convertRowDataToUniformStructure(arrRecord,modelId);
            String currItemFlowStr = mapRecord.get("GPRS_FLOW");
            allSumFlow += Long.parseLong(currItemFlowStr);
            if(i==sortedList.size()-1){
                Map<String,String> retMap = new HashMap<String, String>();
                retMap.put("allFlow",String.valueOf(allSumFlow));
                retMap.put("termCode",mapRecord.get("USER_TERM_BRAND"));
                retMap.put("planCode",mapRecord.get("PLAN_ID"));
                retMap.put("freeFlow",mapRecord.get("FREE_FLOW"));
                retMap.put("flowPlanCode",mapRecord.get("FLOW_PLAN_ID"));
                retMap.put("sugPlanCode","0"); //保留
                retData.add(retMap);
            }
        }
        endTime = System.currentTimeMillis();
        log.info("requestF17 stat records token: " + (endTime - beginTime) + "ms");
        return new Object[]{retData};
    }


    public static int daysBetween(Date smdate,Date bdate)
       {
           Calendar cal = Calendar.getInstance();
           cal.setTime(smdate);
           long time1 = cal.getTimeInMillis();
           cal.setTime(bdate);
           long time2 = cal.getTimeInMillis();
           Double between_days_double=(time2-time1)/(1000*3600*24.0);
           long between_days = Math.round(between_days_double);
           log.info("between_days_double = "+between_days_double + " between_days = " + between_days);
           return Integer.parseInt(String.valueOf(between_days));
       }

    public static double div(double v1, double v2, int scale) {
      if (scale < 0) {
        throw new IllegalArgumentException("The scale must be a positive integer or zero");
      }
      BigDecimal b = new BigDecimal(Double.toString(v1));
      BigDecimal one = new BigDecimal(v2);
      return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     *
     * @param decimal
     * @param needLength 小数点后需要精确地位数
     * @param fillStr   填充字符串
     * @return
     */
    public static String decimalFillLength(String decimal,int needLength,String fillStr){
        int needFillLength = 0;
        if(decimal != null){
          int pointIndex = decimal.indexOf(".");
          int currLength = decimal.length() - pointIndex-1;
          needFillLength = needLength - currLength;
       }
       StringBuffer buf = new StringBuffer(decimal);
       for(int i=0; i<needFillLength; i++){
         buf.append(fillStr);
       }
       return buf.toString();
    }

    public boolean distinctAndMerge(MetaModel busiMeta, List<Map<String, String>> retData, Map<String, String> rowData, Map<String, Integer> repeatCheckMap,Map<String, Integer> mergeCheckMap, int index){
        boolean isDisincted = distinct.distinct(busiMeta, retData, rowData, repeatCheckMap, index);
        boolean isMerged = merge.merge(busiMeta, retData, rowData, mergeCheckMap, index);
        if(isDisincted){
            LogUtils.debug(log, "distinct record: " + rowData);
        }
        if(isMerged){
            LogUtils.debug(log, "merge record: " + rowData);
        }
        return !isDisincted && !isMerged;
    }

}
