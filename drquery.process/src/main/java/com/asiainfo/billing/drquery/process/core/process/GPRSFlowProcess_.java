package com.asiainfo.billing.drquery.process.core.process;

import com.ailk.oci.ocnosql.common.rowkeygenerator.MD5RowKeyGenerator;
import com.asiainfo.billing.drquery.Constants;
import com.asiainfo.billing.drquery.cache.CacheProvider;
import com.asiainfo.billing.drquery.cache.redis.RedisCache;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.Executor;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.DRCommonProcess;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.process.operation.MonitorLog;
import com.asiainfo.billing.drquery.utils.DateUtil;
import com.asiainfo.billing.drquery.utils.PropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GPRSFlowProcess_ extends DRCommonProcess {

    private final static Log log = LogFactory.getLog(GPRSFlowProcess_.class);

    private final static Boolean ISCOUNTER = true;

    private final static String RCOUNT = "RCOUNT";

    private String tablePrefix;//表名前缀
    
    public static final int timeout = Integer.parseInt(PropertiesUtil.getProperty("drquery.service/runtime.properties","redis.expiretime", "300"));

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public DRProcessDTO process(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams)
            throws ProcessException, BusinessException {
        List<Map<String, String>> modelList = new ArrayList<Map<String, String>>();
        String interfaceType = request.getInterfaceType();
        Map<String, String> jsonArgsStr = request.getJsonArgsStr();
        List<String> monthsTables;
        try {
            //判断输入参数时间是否合法
            isTimeArgsLegal(jsonArgsStr.get("startTime"));
            isTimeArgsLegal(jsonArgsStr.get("endTime"));
            monthsTables = monthsBetween(jsonArgsStr);//计算开始时间和结束时间间隔的月份
        } catch (ParseException e) {
            throw new ProcessException("time format error,please check startTime or endTime " +
                    " are like \"yyyyMMddHHmmss\"",e);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(),e);
        }
        Object[] result = null;
        DRProcessDTO dto = new DRProcessDTO();
        if ("F11".equals(interfaceType)) { //用户流量消费行为单维度汇总
            modelList = querySumData(request,monthsTables,jsonArgsStr,interfaceType,viewMeta,extendParams);
            result = summaryData(modelList, jsonArgsStr, extendParams);
            List<Map> allAppOrDomain = (List) result[0];
            List<Map> sumList = (List) result[1];
            Map<String,List<Map>> summaryInfoMap = new HashMap<String, List<Map>>();
            summaryInfoMap.put(Constants.SUM_DATA_INFO, allAppOrDomain);
            summaryInfoMap.put(Constants.SUM_STAT_INFO, sumList);
            redisCache.putData2Cache(request.generateCacheKey(), summaryInfoMap, true);
        } else if ("F12".equals(interfaceType)) {//用户流量消费行为详单查询接口
            Object[] resObj = queryPageData(request,monthsTables,jsonArgsStr,interfaceType,viewMeta,extendParams);
            modelList = (List<Map<String, String>>)resObj[0];
            Map<String, Integer> counterMap = (Map<String, Integer>)resObj[1];
            result = processData(modelList, jsonArgsStr, viewMeta, extendParams,counterMap);
        }

        dto.setdRModels((List) result[0]);
        if (result[1] instanceof List) {
            dto.setSumList((List) result[1]);
        } else if (result[1] instanceof Map) {
            dto.setStats((Map) result[1]);
        }
        return dto;
    }

    
    private Object[] buildSQL(String month, Map<String, String> args, String interfaceType,
                              MetaModel busiMeta, Map extendParams,String startKey,boolean isCountQuery)
            throws ProcessException, BusinessException {
        StringBuffer sql = new StringBuffer();
        String phoneNo = args.get("phoneNo");
        String startTimeStr = args.get("startTime");
        String endTimeStr = args.get("endTime");
        StringBuffer startIDStr = new StringBuffer();
        StringBuffer endIDStr = new StringBuffer();
        String chargeId = args.get("chargeId");
        String md5PhoneNo = (String) new MD5RowKeyGenerator().generatePrefix(phoneNo);
        if(StringUtils.isEmpty(startKey)){
            startIDStr.append(md5PhoneNo).append(phoneNo).append(startTimeStr);
        }else{
            startIDStr.append(startKey);
        }
        endIDStr.append(md5PhoneNo).append(phoneNo).append(endTimeStr).append("g");
        if ("F11".equals(interfaceType)) { //用户流量消费行为单维度汇总
            String groupColumnCode = args.get("groupColumnCode");
            String groupByColumn = "";
            if (groupColumnCode.trim().equalsIgnoreCase("appId")) {
                groupByColumn = "BUSI_ID";
            } else if (groupColumnCode.trim().equalsIgnoreCase("mainDomain")) {
                groupByColumn = "EX_TL_DOMAIN";
            }
            sql.append(" select /*SEEK_TO_COLUMN*/ ").append(groupByColumn).append(",").append(" TERM_BRAND_ID").append(",").
                append("TERM_MODEL_ID, IMEI_SEG, ").
                append(" count(").append(groupByColumn).append(") as GROUPCOUNT,").
                append(" SUM(TO_NUMBER(FLOW)/1024) as ALIASFLOW,").
                append(" MAX(APP_EXT_FLAG) as APP_EXT_FLAG,").
                append(" MAX(START_TIME) as MAX_TIME,").
                append(" MIN(START_TIME) as MIN_TIME").
                append(" from ").append(tablePrefix).append(month).
                append(" where ").
                append(" ID>= ? and ID< ? ");

            if(StringUtils.isNotEmpty(chargeId)) {
                sql.append(" and CHARGING_ID=").append("'").append(chargeId).append("'");
            }
            String billNo = args.get("billNo");
            if(StringUtils.isNotEmpty(billNo)) {
                if("gg".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='2' and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 23 and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) >=7");
                } else if("ga".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='2' and (TO_NUMBER(SUBSTR(START_TIME, 9, 2)) > 22 or TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 7)");
                } else if("gh".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='1' and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 23 and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) >=7");
                } else if("gb".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='1' and (TO_NUMBER(SUBSTR(START_TIME, 9, 2)) > 22 or TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 7)");
                }
            }
            sql.append(" group by ").append(groupByColumn).append(",").append("IMEI_SEG,").append("TERM_BRAND_ID,").append("TERM_MODEL_ID");
        } else if ("F12".equals(interfaceType)) {//用户流量消费行为详单查询接口
            String orderColumnCode = args.get("orderColumnCode");
            String whereAppIDFieldValue = args.get("appId");
            String whereMainDomainFieldValue = args.get("mainDomain");

            sql.append("select ");
            if(isCountQuery){
                sql.append(" count(1) as  ").append(RCOUNT);
            }else {
                sql.append("ID, START_TIME, ACCE_URL,DETAIL_EXT_FLAG,").
                    append("TO_NUMBER(FLOW)/1024 as ALIASFLOW");
            }
            sql.append(" from ").append(tablePrefix).append(month).
                append(" where ").
                append(" ID>= ? and ID< ? ");
            if (!StringUtils.isEmpty(whereAppIDFieldValue)) {
                if (!whereAppIDFieldValue.trim().equalsIgnoreCase("null")) {
                    sql.append(" and ").append("BUSI_ID").append("=").append("'").append(whereAppIDFieldValue).append("'");
                } else {
                    sql.append(" and ").append("BUSI_ID").append(" is null ");
                }
            }
            if (!StringUtils.isEmpty(whereMainDomainFieldValue)) {
                if (!whereMainDomainFieldValue.trim().equalsIgnoreCase("null")) {
                    sql.append(" and ").append("EX_TL_DOMAIN").append("=").append("'").append(whereMainDomainFieldValue).append("'");
                } else {
                    sql.append(" and ").append("EX_TL_DOMAIN").append(" is null ");
                }
            }

            if(StringUtils.isNotEmpty(chargeId)) {
                sql.append(" and CHARGING_ID=").append("'").append(chargeId).append("'");
            }

            String billNo = args.get("billNo");
            if(StringUtils.isNotEmpty(billNo)) {
                if("gg".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='2' and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 23 and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) >=7");
                } else if("ga".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='2' and (TO_NUMBER(SUBSTR(START_TIME, 9, 2)) > 22 or TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 7)");
                } else if("gh".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='1' and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 23 and TO_NUMBER(SUBSTR(START_TIME, 9, 2)) >=7");
                } else if("gb".equals(billNo)) {
                    sql.append(" and ACCESS_MODE_ID='1' and (TO_NUMBER(SUBSTR(START_TIME, 9, 2)) > 22 or TO_NUMBER(SUBSTR(START_TIME, 9, 2)) < 7)");
                }
            }

            String offset = args.get("offset");
            if(!StringUtils.isEmpty(offset) && !offset.equals("-1")){
               sql.append(" limit ").append(offset);
            }
        }

        StringBuffer sqlInfo = new StringBuffer(sql);
        int firstArg = sql.indexOf("?");
        sqlInfo = sqlInfo.replace(firstArg, firstArg + 1, "\'" + startIDStr + "\'");
        int lastArg = sqlInfo.indexOf("?");
        sqlInfo = sqlInfo.replace(lastArg, lastArg + 1, "\'" + endIDStr + "\'");
        log.info("sql = " + sqlInfo);
        extendParams.put((isCountQuery==true?"countQuery":"")+"_"+MonitorLog.SQL, sqlInfo.toString()); //记录执行的sql语句
        return new Object[]{sql.toString(), new String[]{startIDStr.toString(), endIDStr.toString()}};
    }

    public Map<String, Object> summaryAllData(List<Map<String, String>> retArr, Map<String, String> args, Map extendParams) {
        double totalFlow = 0;
        for(Map<String, String> record : retArr) {
            if(StringUtils.isNotEmpty(record.get("FLOW")))
                totalFlow += Double.parseDouble(record.get("FLOW"));
        }
        Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("totalFlow", decimalFillLength(String.valueOf(totalFlow),4,"0"));
        return stateMap;
    }

    
    public Object[] summaryData(List<Map<String, String>> retArr, Map<String, String> args, Map extendParams) {
        long summaryStartTime = System.currentTimeMillis();
        //对跨月多表查询的情况，再做汇总
        String groupColumnCode = args.get("groupColumnCode");
        String groupByColumn = "";
        if (groupColumnCode.trim().equalsIgnoreCase("appId")) {
            groupByColumn = "BUSI_ID";
        } else if (groupColumnCode.trim().equalsIgnoreCase("mainDomain")) {
            groupByColumn = "EX_TL_DOMAIN";
        }
        Map<String,Map<String, String>> groupKeyMaps = new HashMap<String,Map<String, String>>();
        Set<String> groupKeySet = new HashSet<String>();
        BigDecimal totalFlow = new BigDecimal(0);
        for (Map<String, String> originalRecord : retArr) {
            String groupKey = originalRecord.get(groupByColumn) + "|" + originalRecord.get("TERM_BRAND_ID") + "|" + originalRecord.get("TERM_MODEL_ID");
            String appId = originalRecord.get(groupByColumn);
            BigDecimal recordFlow = new BigDecimal(originalRecord.get("ALIASFLOW"));
            totalFlow = totalFlow.add(recordFlow);

            if(!groupKeySet.contains(groupKey)){
                originalRecord.put("groupTotalFlow",String.valueOf(recordFlow));
                originalRecord.put("groupRecordCount",originalRecord.get("GROUPCOUNT"));
                groupKeySet.add(groupKey);
                groupKeyMaps.put(groupKey, originalRecord);
           }else{
              Map<String,String> preMap = groupKeyMaps.get(groupKey);
              BigDecimal preFlow = new BigDecimal(preMap.get("groupTotalFlow"));
              preMap.put("groupTotalFlow",String.valueOf(preFlow.add(recordFlow)));
              long preGroupCount = Long.parseLong(preMap.get("groupRecordCount"));
              Long currGroupCount = Long.valueOf(originalRecord.get("GROUPCOUNT"));
              preMap.put("groupRecordCount",String.valueOf(preGroupCount+currGroupCount));
           }
        }
        log.info("kindMaps size is " + groupKeyMaps.size());

        List<Map<String, String>> excludeOtherRetData = new ArrayList<Map<String, String>>(); // 去除“其它”类型的结果集
        List<Map<String, String>> otherAppOrDomainData = new ArrayList<Map<String, String>>();
        //Map<String, String> otherAppOrDomainMap = new HashMap<String, String>();//“其它”类型的结果集
        for(Iterator<String> it = groupKeyMaps.keySet().iterator();it.hasNext();){
             String key = it.next();
             Map<String,String> km = groupKeyMaps.get(key);
             //String fillGroupTotalFlow = decimalFillLength(km.get("groupTotalFlow"),4,"0");
             //km.put("groupTotalFlow",fillGroupTotalFlow);
             if(km.get("BUSI_ID").equals("-9")){
                 otherAppOrDomainData.add(km);
             }else{
                 excludeOtherRetData.add(km);
             }
        }
        log.info("excludeOtherRetData size is " + excludeOtherRetData.size());
        groupKeyMaps = null; //置为null，利于回收

        //对去除“未识别”类型的按分类总流量降序排序
        Collections.sort(excludeOtherRetData, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                String o1TF = o1.get("groupTotalFlow");
                String o2TF = o2.get("groupTotalFlow");
                //log.info("o1TF=" + o1TF + " o2TF=" + o2TF);
                if (!StringUtils.isEmpty(o1TF) && !StringUtils.isEmpty(o2TF)
                        && !o1TF.trim().equalsIgnoreCase("null")
                        && !o2TF.trim().equalsIgnoreCase("null")) {
                    Double o1TFDou = Double.valueOf(o1TF);
                    Double o2TFDou = Double.valueOf(o2TF);
                    //比较流量大小
                    return o2TFDou.compareTo(o1TFDou);
                } else if ((!StringUtils.isEmpty(o1TF) && !o1TF.trim().equalsIgnoreCase("null"))
                        && (StringUtils.isEmpty(o2TF) || o2TF.trim().equalsIgnoreCase("null"))) {
                    return -1;
                } else if ((StringUtils.isEmpty(o1TF) || o1TF.trim().equalsIgnoreCase("null"))
                        && (!StringUtils.isEmpty(o2TF) && !o2TF.trim().equalsIgnoreCase("null"))) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        List<Map<String, String>> retData = new ArrayList<Map<String, String>>();
        retData.addAll(excludeOtherRetData);
        if(otherAppOrDomainData.size() > 0){
            retData.addAll(otherAppOrDomainData);
        }
        Map<String, Object> stateMap = new HashMap<String, Object>();
        stateMap.put("groupCount", retData.size());
        stateMap.put("totalFlow", decimalFillLength(String.valueOf(totalFlow),4,"0"));
        List<Map<String, Object>> sumList = new ArrayList<Map<String, Object>>();
        sumList.add(stateMap);
        extendParams.put(MonitorLog.DEAL_SUMMARYRESULT_COST_TIME, System.currentTimeMillis() - summaryStartTime);
        Object[] result = new Object[]{retData, sumList};
        return result;
    }

    
    public Object[] processData(List<Map<String, String>> retArr, Map<String, String> args,
                                MetaModel busiMeta, Map extendParams,Map<String, Integer> counterResultMap) {
        log.info("retArr size is" + retArr.size());
        long processStartTime = System.currentTimeMillis();
       
        long totalCount = 0L;
        for(Iterator<String> it = counterResultMap.keySet().iterator();it.hasNext();){
            String month = it.next();
            totalCount += counterResultMap.get(month);
        }
        Map<String, String> stateMap = new HashMap<String, String>();
        stateMap.put("totalCount",String.valueOf(totalCount));
        String startIndexStr = args.get("startIndex");
        String offsetStr = args.get("offset");
        stateMap.put("startIndex", startIndexStr);
        stateMap.put("offset", offsetStr);
        extendParams.put(MonitorLog.DEAL_DETAILRECORD_COST_TIME, System.currentTimeMillis() - processStartTime);
        Object[] result = new Object[]{retArr, stateMap};
        return result;
    }

    public List<Map<String, String>> querySumData(CommonDRProcessRequest request,List<String> monthsTables,Map<String, String> args,
                                          String interfaceType, MetaModel busiMeta, Map extendParams)
                            throws ProcessException,BusinessException{
        List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        String[] months = listToArray(monthsTables);
        List<Object[]> sqlArgsList = new ArrayList<Object[]>();
        String[] monthSQLs = new String[months.length];
        Map<Boolean,String[]> monthSQLsMap = new HashMap<Boolean,String[]>();
        for(int i=0; i < months.length; i++){
            String month = months[i];
            Object[] sqlAndArgs = buildSQL(month,args,interfaceType,busiMeta,extendParams,null,false);
            String dataSql = (String)sqlAndArgs[0];
            Object[] dataSqlArgs = (Object[])sqlAndArgs[1];
            sqlArgsList.add(dataSqlArgs);
            monthSQLs[i] = dataSql;
        }
        monthSQLsMap.put(!ISCOUNTER, monthSQLs);
        Map<Boolean,Map<String,List<Map<String, String>>>> resultMap = queryBySQL(monthSQLsMap,sqlArgsList,monthsTables,extendParams);
        //将每个月的结果统计组装
        Map<String,List<Map<String, String>>> dataResultMap = resultMap.get(!ISCOUNTER);
        for(Iterator<String> it = dataResultMap.keySet().iterator();it.hasNext();){
            String month = it.next();
            resultList.addAll(dataResultMap.get(month));
        }
        return resultList;
    }


    
	public Object[] queryPageData(CommonDRProcessRequest request,List<String> monthsTables, Map<String, String> args,
                                          String interfaceType, MetaModel busiMeta, Map extendParams)
            throws ProcessException,BusinessException{
      String cacheKey = request.generateCacheKey();
      int startIndex = Integer.valueOf(args.get("startIndex"));
      int offset = Integer.valueOf(args.get("offset"));
      String[] months = listToArray(monthsTables);
      List<Object[]> sqlArgsList = new ArrayList<Object[]>();
      Map<Boolean,String[]> monthSQLsMap = new HashMap<Boolean,String[]>();

      Map<String, Integer> countCacheMap = null;
      Map<String, String> pageCacheMap = null;
      List counterAndRowKeyMap = CacheProvider.getCountAndRowkeyInfo(cacheKey,startIndex);
      if(counterAndRowKeyMap.get(0) != null) {  //不是第一次查询
          Iterator it = counterAndRowKeyMap.iterator();
          int index = 0;
          while (it.hasNext()){
              if(index++ == 0){
                 countCacheMap = (Map<String, Integer>)it.next();
              }else{
                 pageCacheMap = (Map<String, String>)it.next();
              }
          }
      }
      
      //Map<String, String> cacheMap = CacheProvider.getPageStartKey(cacheKey, months, startIndex);
      
      List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
      //Map<String, Integer> counterResultMap = new TreeMap<String, Integer>();
      Map<String,List<Map<String, String>>> dataResultMap = new HashMap<String, List<Map<String, String>>>();
      if(pageCacheMap == null || pageCacheMap.size() == 0){//第一次查询
         String[] countMonthSQLs = new String[months.length];
         String[] dataMonthSQLs = new String[months.length];
         for(int i=0; i < months.length; i++){
            String month = months[i];
            if(countCacheMap == null) {
            	Object[] countSqlAndArgs = buildSQL(month,args,interfaceType,busiMeta,extendParams,null,true);
            	String countSql = (String)countSqlAndArgs[0];
            	countMonthSQLs[i] = countSql;
            }
            Object[] dataSqlAndArgs = buildSQL(month,args,interfaceType,busiMeta,extendParams,null,false);
            String dataSql = (String)dataSqlAndArgs[0];
            Object[] dataSqlArgs = (Object[])dataSqlAndArgs[1];
            sqlArgsList.add(dataSqlArgs);
            dataMonthSQLs[i] = dataSql;
         }
         if(countCacheMap == null)
        	 monthSQLsMap.put(ISCOUNTER, countMonthSQLs);
         monthSQLsMap.put(!ISCOUNTER,dataMonthSQLs);
      }else {
         months = new String[pageCacheMap.size()];
         String[] dataMonthSQLs = new String[months.length];
         sqlArgsList = new ArrayList<Object[]>();
         int index = 0;
         for(Iterator<String> it = pageCacheMap.keySet().iterator(); it.hasNext();){
              String month = it.next();
              String startKey = pageCacheMap.get(month);
              months[index] = month;
              Object[] dataSqlAndArgs = buildSQL(month,args,interfaceType,busiMeta,extendParams,startKey,false);
              String dataSql = (String)dataSqlAndArgs[0];
              dataMonthSQLs[index] = dataSql;
              Object[] dataSqlArgs = (Object[])dataSqlAndArgs[1];
              sqlArgsList.add(dataSqlArgs);
              index++;
         }
         monthSQLsMap.put(!ISCOUNTER,dataMonthSQLs);
      }
      
      Map<Boolean,Map<String,List<Map<String, String>>>> resultMap = queryBySQL(monthSQLsMap,sqlArgsList,Arrays.asList(months),extendParams);
      
      if(countCacheMap == null){
    	  	Map<String, List<Map<String, String>>> map = resultMap.get(ISCOUNTER);
    	  	countCacheMap = new TreeMap<String, Integer>();
        	for( Map.Entry<String, List<Map<String, String>>>  entity : map.entrySet()) {
        		countCacheMap.put(entity.getKey(), Integer.parseInt(entity.getValue().get(0).get("RCOUNT")));
        	}
            ((RedisCache)redisCache).getRedisTemplate().opsForHash().put(cacheKey,-9,countCacheMap);
      }

      dataResultMap = resultMap.get(!ISCOUNTER);

      //将每个月的结果统计组装
      Map<String,List<Map<String, String>>> newDataResultMap = new LinkedHashMap<String, List<Map<String, String>>>();
      for(String month:months){
         newDataResultMap.put(month,dataResultMap.get(month));
         resultList.addAll(dataResultMap.get(month));
      }
      
      //将分页信息存入redis
      CacheProvider.putPageStartKeyOrCountToCache(cacheKey, newDataResultMap, countCacheMap, startIndex, offset,  
    		  new MD5RowKeyGenerator().generate(args.get("phoneNo")) + args.get("startTime"), timeout);
      return new Object[]{resultList, countCacheMap};
    }

	
	public Map<Boolean,Map<String,List<Map<String, String>>>> queryBySQL(Map<Boolean,String[]> sqlMaps, final List<Object[]> args,
                                      List<String> monthsTables, final Map extendParams)
                                 throws ProcessException{
        long execSqlStartTime = System.currentTimeMillis();
        Map<Boolean,Map<String,List<Map<String, String>>>> resultMap = new HashMap<Boolean,Map<String,List<Map<String, String>>>>();
        final Map<String,List<Map<String, String>>> countModelListMap = new LinkedHashMap<String,List<Map<String, String>>>(); //线程安全
        final Hashtable<String,List<Map<String, String>>> dataModelListMap = new Hashtable<String,List<Map<String, String>>>(); //线程安全
        final CountDownLatch runningThreadNum = new CountDownLatch(sqlMaps.size() * monthsTables.size());
        final LinkedBlockingQueue<Exception> exceptionQueue = new LinkedBlockingQueue<Exception>();
        List<Future> futures = new ArrayList<Future>();
        for(Iterator<Boolean> it = sqlMaps.keySet().iterator();it.hasNext();){
            final boolean isCountQuery = it.next();
            String[] monthSQLs = sqlMaps.get(isCountQuery);
            for (int index = 0; index < monthSQLs.length; index++) {
                final int monthIndexCode = index;
                final String month = monthsTables.get(index);
                String sql = monthSQLs[index];
                int tablePrefixIndex = sql.indexOf(tablePrefix);
                int endIndex = tablePrefixIndex + tablePrefix.length() + month.length();
                final String monthSql = new StringBuffer(sql).replace(tablePrefixIndex + tablePrefix.length(),
                        endIndex, month).toString();
                //log.info("monthSql = " + monthSql);
                Thread worker = new Thread() {
					@Override
                    public void run() {
                        try {
                            long execEverySqlStartTime = System.currentTimeMillis();
                            List<Map<String, String>> everyModelList = loadData(monthSql, args.get(monthIndexCode));
                            long costTime = System.currentTimeMillis() - execEverySqlStartTime;
                            extendParams.put((isCountQuery==true?"countQuery":"")+"_"+MonitorLog.SQL_COST_TIME + "_" + month, costTime);//记录每一条SQL的执行时间
                            log.info("query table " + tablePrefix + month + ", token: " + costTime + "ms, return " + everyModelList.size() + "records.");
                            if(isCountQuery){
                               countModelListMap.put(month,everyModelList);
                            }else {
                               dataModelListMap.put(month,everyModelList);
                            }
                        } catch (Exception e) {
                            log.error("query sql exception", e);
                            exceptionQueue.add(e);
                        } finally {
                            runningThreadNum.countDown();
                        }
                    }
                };
                futures.add(Executor.getInstance().getThreadPool().submit(worker));
            }
        }
        try {
            runningThreadNum.await();
            if (exceptionQueue.size() != 0) {
                Exception e = exceptionQueue.poll();
                throw e;
            }
            extendParams.put(MonitorLog.SQL_COST_TIME, System.currentTimeMillis() - execSqlStartTime);//记录sql总执行时间
        } catch (Exception e) {
            throw new ProcessException("multiple thread query faild:" + e.getMessage(), e);
        }
        resultMap.put(ISCOUNTER,countModelListMap);
        resultMap.put(!ISCOUNTER,dataModelListMap);
        return resultMap;
    }

    /**
     * 返回开始时间和结束时间之间的所有月份  yyyymm
     *
     * @param jsonArgsStr
     * @return List:{"201301","201302","201303"}
     */
    public static List<String> monthsBetween(Map<String, String> jsonArgsStr) throws ParseException {
        List<String> monthsList = new ArrayList<String>();
        String startTimeStr = jsonArgsStr.get("startTime");
        String endTimeStr = jsonArgsStr.get("endTime");
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        int interValMonths = DateUtil.getMonths(df.parse(startTimeStr), df.parse(endTimeStr));
        int year = Integer.parseInt(startTimeStr.substring(0, 4));
        int month = Integer.parseInt(startTimeStr.substring(4, 6));
        for (int i = 0; i <= interValMonths; i++) {
            if (month > 12) {
                year = year + 1;
                month = 1;
            }
            String fillMonth = String.valueOf(month);
            if ((month) < 10) {
                fillMonth = "0" + month;
            }
            month = month + 1;
            monthsList.add(year + fillMonth);
        }
        return monthsList;
    }

    /**
     * @param decimal
     * @param needLength 小数点后需要精确地位数
     * @param fillStr    填充字符串
     * @return
     */
    public static String decimalFillLength(String decimal, int needLength, String fillStr) {
        int needFillLength = 0;
        if (decimal != null) {
            int pointIndex = decimal.indexOf(".");
            if(pointIndex < 0){
                return decimal;
            }
            int currLength = decimal.length() - pointIndex - 1;
            needFillLength = needLength - currLength;
        }
        if (needFillLength < 0) {
            DecimalFormat f = new DecimalFormat("#######0.0000");
            f.setRoundingMode(RoundingMode.HALF_UP);
            return f.format(Double.valueOf(decimal));
        }
        StringBuffer buf = new StringBuffer(decimal);
        for (int i = 0; i < needFillLength; i++) {
            buf.append(fillStr);
        }
        return buf.toString();
    }

    public static void isTimeArgsLegal(String time) throws Exception {
        String msg = "";

        if (time == null || time.length() != 14) {
            msg = "时间:" + time + " 长度不符合14位要求的长度 ,请参照yyyyMMddHHmmss";
            throw new Exception(msg);
        }
        String year = time.substring(0, 4);
        String month = time.substring(4, 6);
        String day = time.substring(6, 8);
        String hour = time.substring(8, 10);
        String minute = time.substring(10, 12);
        String second = time.substring(12, 14);
        int sy = 0;
        int sm =0;
        int sd = 0;
        int shour = 0;
        int sminute = 0;
        int ssecond = 0;
        try {
            sy = Integer.parseInt(year);
            sm = Integer.parseInt(month);
            sd = Integer.parseInt(day);
            shour = Integer.parseInt(hour);
            sminute = Integer.parseInt(minute);
            ssecond = Integer.parseInt(second);
        } catch (Exception e) {
            msg = "输入的时间不能包含字母";
            throw new Exception(msg);
        }
        int maxDays = 31;
        if (sm > 12 || sm < 1) {
            msg = "您输入的月份不在规定范围内";
            throw new Exception(msg);
        } else if (sm == 4 || sm == 6 || sm == 9 || sm == 11) {
            maxDays = 30;
        } else if (sm == 2) {
            if (sy % 4 == 0 && sy % 100 != 0)
                maxDays = 29;
            else if (sy % 100 == 0 && sy % 400 == 0)
                maxDays = 29;
            else
                maxDays = 28;
        }
        if (sd < 1 || sd > maxDays) {
            msg = "您输入的日期不在规定范围内";
            throw new Exception(msg);
        }
        if (shour < 0 || shour > 23) {
            msg = "您输入的小时不在规定范围内";
            throw new Exception(msg);
        }
        if (sminute < 0 || sminute > 59) {
            msg = "您输入的分钟不在规定范围内";
            throw new Exception(msg);
        }
        if (ssecond < 0 || ssecond > 59) {
            msg = "您输入的秒不在规定范围内";
            throw new Exception(msg);
        }
    }

    public static String[] listToArray(List<String> list){
       if(list != null){
          String[] arr = new String[list.size()];
          for(int i=0; i<list.size(); i++){
              arr[i] = list.get(i);
          }
          return arr;
       }
       return null;
    }

    public static void main(String[] args) throws Exception {
        Map<String,String> jsonArgsStr = new HashMap<String, String>();
        jsonArgsStr.put("startTime","20130901000000");
        jsonArgsStr.put("endTime","20131130230100");
        List<String> list = monthsBetween(jsonArgsStr);
        for(String month : list){
            System.out.println("month="+month);
        }
    }

}
