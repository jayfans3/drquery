package com.asiainfo.billing.drquery.process.core.process;

import com.ailk.oci.ocnosql.client.rowkeygenerator.*;
import com.asiainfo.billing.drquery.cache.*;
import com.asiainfo.billing.drquery.exception.*;
import com.asiainfo.billing.drquery.model.*;
import com.asiainfo.billing.drquery.process.*;
import com.asiainfo.billing.drquery.process.core.*;
import com.asiainfo.billing.drquery.process.core.request.*;
import com.asiainfo.billing.drquery.process.dto.*;
import com.asiainfo.billing.drquery.process.operation.*;
import com.asiainfo.billing.drquery.utils.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import java.math.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class GPRSFlowProcess extends DRCommonProcess {

    private final static Log log = LogFactory.getLog(GPRSFlowProcess.class);

    private String tablePrefix;//表名前缀

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DRProcessDTO process(CommonDRProcessRequest request, MetaModel busiMeta, final Map extendParams)
            throws ProcessException, BusinessException {
        final Vector<Map<String, String>> modelList = new Vector<Map<String, String>>(); //线程安全
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
                    " are like \"yyyyMMddHHmmss\"");
        } catch (Exception e) {
            throw new ProcessException(e.getMessage());
        }
        final CountDownLatch runningThreadNum = new CountDownLatch(monthsTables.size());
        final LinkedBlockingQueue<Exception> exceptionQueue = new LinkedBlockingQueue<Exception>();
        List<Future> futures = new ArrayList<Future>();
        final Object[] sqlObj = buildSQLAndQuery(monthsTables.get(0), jsonArgsStr, interfaceType, busiMeta, extendParams);
        String sql = (String) sqlObj[0];
        long execSqlStartTime = System.currentTimeMillis();
        for (final String month : monthsTables) {
            StringBuffer buf = new StringBuffer(sql);
            int tablePrefixIndex = buf.indexOf(tablePrefix);
            int endIndex = tablePrefixIndex + tablePrefix.length() + month.length();
            final String monthSql = new StringBuffer(sql).replace(tablePrefixIndex + tablePrefix.length(),
                    endIndex, month).toString();
            log.info("monthSql = " + monthSql);
            Thread worker = new Thread() {
                @Override
                public void run() {
                    try {
                        long execEverySqlStartTime = System.currentTimeMillis();
                        List<Map<String, String>> everyModelList = loadData(monthSql, (String[]) sqlObj[1]);
                        long costTime = System.currentTimeMillis() - execEverySqlStartTime;
                        extendParams.put(MonitorLog.SQL_COST_TIME + "_" + month, costTime);//记录每一条SQL的执行时间
                        log.info("query <" + month + "> time is " + costTime);
                        log.info("====="+month+"====everyModelList size=" + everyModelList.size());
                        modelList.addAll(everyModelList);
                        log.info("====="+month+"====modelList size=" + modelList.size());
                    } catch (Exception e) {
                        exceptionQueue.add(e);
                    } finally {
                        runningThreadNum.countDown();
                    }
                }
            };
            futures.add(Connection.getInstance().getThreadPool().submit(worker));
        }
        try {
            runningThreadNum.await();
            if (exceptionQueue.size() != 0) {
                Exception e = exceptionQueue.poll();
                throw e;
            }
            log.info("=========modelList size=" + modelList.size());
            extendParams.put(MonitorLog.SQL_COST_TIME, System.currentTimeMillis() - execSqlStartTime);//记录sql总执行时间
        } catch (Exception e) {
            throw new ProcessException("multiple thread query faild:" + e.getMessage());
        }
        Object[] result = null;
        if ("F11".equals(interfaceType)) { //用户流量消费行为单维度汇总
            result = summaryData(modelList, jsonArgsStr, extendParams);
        } else if ("F12".equals(interfaceType)) {//用户流量消费行为详单查询接口
            result = processData(modelList, jsonArgsStr, busiMeta, extendParams);
        }
        DRProcessDTO dto = new DRProcessDTO();
        dto.setdRModels((List) result[0]);
        if (result[1] instanceof List) {
            dto.setSumList((List) result[1]);
        } else if (result[1] instanceof Map) {
            dto.setStats((Map) result[1]);
        }
        return dto;
    }

    private Object[] buildSQLAndQuery(String month, Map<String, String> args, String interfaceType, MetaModel busiMeta, Map extendParams)
            throws ProcessException, BusinessException {
        StringBuffer sql = new StringBuffer();
        String phoneNo = args.get("phoneNo");
        String startTimeStr = args.get("startTime");
        String endTimeStr = args.get("endTime");
        StringBuffer startIDStr = new StringBuffer();
        StringBuffer endIDStr = new StringBuffer();
        String md5PhoneNo = (String) new MD5RowKeyGenerator().generatePrefix(phoneNo);
        startIDStr.append(md5PhoneNo).append(phoneNo).append(startTimeStr);
        endIDStr.append(md5PhoneNo).append(phoneNo).append(endTimeStr);
        if ("F11".equals(interfaceType)) { //用户流量消费行为单维度汇总
            sql.append(" select distinct LOG_ID,APP_ID,EX_TL_DOMAIN,TO_NUMBER(FLOW)/1024 as ALIASFLOW").
                append(" from ").append(tablePrefix).append(month).
                append(" where PHONE_NO=\'").append(phoneNo).append("\'").
                append(" and ID>= ? and ID<= ? ");
        } else if ("F12".equals(interfaceType)) {//用户流量消费行为详单查询接口
            String orderColumnCode = args.get("orderColumnCode");
            String whereAppIDFieldValue = args.get("appId");
            String whereMainDomainFieldValue = args.get("mainDomain");
            Map<String, String> backNameToDBName = busiMeta.getBackNameToDBName();//取出参数对应的数据库字段
            String orderColumn = backNameToDBName.get(orderColumnCode);
            String whereAppIDField = backNameToDBName.get("appName");
            String whereMainDomainField = backNameToDBName.get("mainDomain");
            if (StringUtils.isEmpty(orderColumn)) {//没有找到对应的数据库字段
                throw new ProcessException("Cann't find column by " + orderColumnCode + " please check \"BUSI_GPRSFLOW.xml\" file");
            }
            String orderFlag = args.get("orderFlag");
            sql.append("select distinct LOG_ID,TERM_MODEL_ID,START_TIME,APP_ID,").
                    append("BUSI_TYPE_ID,EX_TL_DOMAIN,EX_COMP_DOMAIN,ACCE_URL,").
                    append("TO_NUMBER(FLOW)/1024 as ALIASFLOW").
                    append(" from ").append(tablePrefix).append(month).
                    append(" where PHONE_NO=\'").append(phoneNo).append("\'").
                    append(" and ID>= ? and ID<= ? ");
            if (!StringUtils.isEmpty(whereAppIDFieldValue)) {
                if (!whereAppIDFieldValue.trim().equalsIgnoreCase("null")) {
                    sql.append(" and ").append(whereAppIDField).append("=").append("\'").append(whereAppIDFieldValue).append("\'");
                } else {
                    sql.append(" and ").append(whereAppIDField).append(" is null ");
                }
            }
            if (!StringUtils.isEmpty(whereMainDomainFieldValue)) {
                if (!whereMainDomainFieldValue.trim().equalsIgnoreCase("null")) {
                    sql.append(" and ").append(whereMainDomainField).append("=").append("\'").append(whereMainDomainFieldValue).append("\'");
                } else {
                    sql.append(" and ").append(whereMainDomainField).append(" is null ");
                }
            }
            sql.append(" order by ");
            if(orderColumn.equalsIgnoreCase("flow")){
                sql.append("ALIASFLOW");
            }else {
                sql.append(orderColumn);
            }
            sql.append(" ").append(orderFlag);
        }
        StringBuffer sqlInfo = new StringBuffer(sql);
        int firstArg = sql.indexOf("?");
        sqlInfo = sqlInfo.replace(firstArg, firstArg + 1, "\'" + startIDStr + "\'");
        int lastArg = sqlInfo.indexOf("?");
        sqlInfo = sqlInfo.replace(lastArg, lastArg + 1, "\'" + endIDStr + "\'");
        log.info("sql = " + sqlInfo);
        extendParams.put(MonitorLog.SQL, sqlInfo.toString()); //记录执行的sql语句
        return new Object[]{sql.toString(), new String[]{startIDStr.toString(), endIDStr.toString()}};
    }

    public Object[] summaryData(List<Map<String, String>> retArr, Map<String, String> args, Map extendParams) {
        long summaryStartTime = System.currentTimeMillis();
        //对跨月多表查询的情况，再做汇总
        String groupColumnCode = args.get("groupColumnCode");
        String groupByColumn = "";
        if (groupColumnCode.trim().equalsIgnoreCase("appId")) {
            groupByColumn = "APP_ID";
        } else if (groupColumnCode.trim().equalsIgnoreCase("mainDomain")) {
            groupByColumn = "EX_TL_DOMAIN";
        }
        Map<String,Map<String, String>> kindMaps = new HashMap<String,Map<String, String>>();
        Set<String> kindSet = new HashSet<String>();
        BigDecimal totalFlow = new BigDecimal(0);
        for (Map<String, String> originalRecord : retArr) {
           String kind = originalRecord.get(groupByColumn);
           BigDecimal recordFlow = new BigDecimal(originalRecord.get("ALIASFLOW"));
           totalFlow = totalFlow.add(recordFlow);
           if(!kindSet.contains(kind)){
              Map<String,String> oneKind = new HashMap<String,String>();
              oneKind.put("groupValue",kind);
              if(StringUtils.isEmpty(kind) || kind.equalsIgnoreCase("null") || kind.equals("0")){ //c3的数据0表示空值
                    oneKind.put("groupValueName","其它");
              }else{
                  if(groupByColumn.equals("APP_ID")){
                    String escapeAppName = CacheProvider.getAPP_NAMEByAPP_ID("APP_NAME", kind); //转义
                    if(StringUtils.isEmpty(escapeAppName) || escapeAppName.equalsIgnoreCase("null")){
                        oneKind.put("groupValueName","其它");
                    }else{
                        oneKind.put("groupValueName",escapeAppName);
                    }
                  }else{
                    oneKind.put("groupValueName",kind);
                  }
              }
              oneKind.put("groupTotalFlow",String.valueOf(recordFlow));
              oneKind.put("groupRecordCount","1");
              kindSet.add(kind);
              kindMaps.put(kind,oneKind);
           }else{
              Map<String,String> preMap = kindMaps.get(kind);
              BigDecimal preFlow = new BigDecimal(preMap.get("groupTotalFlow"));
              preMap.put("groupTotalFlow",String.valueOf(preFlow.add(recordFlow)));
              long preRecordCount = Long.parseLong(preMap.get("groupRecordCount"));
              preMap.put("groupRecordCount",String.valueOf(preRecordCount+1));
           }
        }
        log.info("kindMaps size is " + kindMaps.size());

        List<Map<String, String>> excludeOtherRetData = new ArrayList<Map<String, String>>(); // 去除“其它”类型的结果集
        Map<String, String> otherAppOrDomainMap = new HashMap<String, String>();//“其它”类型的结果集
        for(Iterator<String> it = kindMaps.keySet().iterator();it.hasNext();){
             String key = it.next();
             Map<String,String> km = kindMaps.get(key);
             String fillGroupTotalFlow = decimalFillLength(km.get("groupTotalFlow"),4,"0");
             km.put("groupTotalFlow",fillGroupTotalFlow);
             if(km.get("groupValueName").equals("其它")){
                 otherAppOrDomainMap = km;
             }else{
                 excludeOtherRetData.add(km);
             }
        }
        log.info("excludeOtherRetData size is " + excludeOtherRetData.size());
        kindMaps = null; //置为null，利于回收

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
        //根据topNum参数返回值
        List<Map<String, String>> retData = new ArrayList<Map<String, String>>();
        int top = Integer.valueOf(args.get("topNum"));
        if (excludeOtherRetData.size() >= top) {
            retData = excludeOtherRetData.subList(0, top);
        } else {
            retData.addAll(excludeOtherRetData);
            if (otherAppOrDomainMap.size() > 0)
                retData.add(otherAppOrDomainMap);
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

    @SuppressWarnings({"rawtypes"})
    public Object[] processData(List<Map<String, String>> retArr, Map<String, String> args, MetaModel busiMeta, Map extendParams) {
        log.info("retArr size is" + retArr.size());
        long processStartTime = System.currentTimeMillis();
        for(Map<String,String> map : retArr){
            String flow = map.get("ALIASFLOW");
            String fillFlow = decimalFillLength(flow,4,"0");
            map.put("FLOW",fillFlow);
            map.remove("ALIASFLOW");
            map.remove("LOG_ID");
        }
        String orderColumnCode = args.get("orderColumnCode");
        final String orderFlag = args.get("orderFlag");
        Map<String, String> backNameToDBName = busiMeta.getBackNameToDBName();//取出参数对应的数据库字段
        final String orderColumn = backNameToDBName.get(orderColumnCode);
        //对跨月多表查询的情况，再按排序字段排列
        Collections.sort(retArr, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                String o1TF = o1.get(orderColumn);
                String o2TF = o2.get(orderColumn);
//                log.info("o1TF=" + o1TF + " o2TF=" + o2TF);
                if (!StringUtils.isEmpty(o1TF) && !StringUtils.isEmpty(o2TF)
                        && !o1TF.trim().equalsIgnoreCase("null")
                        && !o2TF.trim().equalsIgnoreCase("null")) {
                    Double o1TFDou = Double.valueOf(o1TF);
                    Double o2TFDou = Double.valueOf(o2TF);
                    if(!StringUtils.isEmpty(orderFlag) && orderFlag.equalsIgnoreCase("asc")){
                        return o1TFDou.compareTo(o2TFDou);
                    }else {
                        return o2TFDou.compareTo(o1TFDou);  //要求排序字符串可以转换为数值类型
                    }
                } else if ((!StringUtils.isEmpty(o1TF) && !o1TF.trim().equalsIgnoreCase("null"))
                        && (StringUtils.isEmpty(o2TF) || o2TF.trim().equalsIgnoreCase("null"))) {
                    if(!StringUtils.isEmpty(orderFlag) && orderFlag.equalsIgnoreCase("asc")){
                        return 1;
                    }else {
                        return -1;
                    }
                } else if ((StringUtils.isEmpty(o1TF) || o1TF.trim().equalsIgnoreCase("null"))
                        && (!StringUtils.isEmpty(o2TF) && !o2TF.trim().equalsIgnoreCase("null"))) {
                    if(!StringUtils.isEmpty(orderFlag) && orderFlag.equalsIgnoreCase("asc")){
                        return -1;
                    }else {
                        return 1;
                    }
                } else {
                    return 0;
                }
            }
        });
        //去重，合并
        int index = 0;
        Map<String, Integer> repeatCheckMap = new HashMap<String, Integer>();
        Map<String, Integer> mergeCheckMap = new HashMap<String, Integer>();
        List<Map<String,String>> newRetData = new ArrayList<Map<String, String>>();
        for (int i = 0; i < retArr.size(); i++) {
            Map<String, String> record = retArr.get(i);
            boolean isDisincted = distinct.distinct(busiMeta, retArr, record, repeatCheckMap, index);//去重
            boolean isMerged = false;
            if (!isDisincted) {
                isMerged = merge.merge(busiMeta, retArr, record, mergeCheckMap, index); //合并
                if (isMerged) {
                    LogUtils.debug(log, "merge record: " + record);
                }
            } else {
                LogUtils.debug(log, "distinct record: " + record);
            }
            if (!isDisincted && !isMerged) {
                newRetData.add(record);
                index++;
            }
        }
        log.info("newRetData size is" + newRetData.size());
        Map<String, String> stateMap = new HashMap<String, String>();
        stateMap.put("totalCount", newRetData.size() + "");
        int totalCount = newRetData.size();
        String startIndexStr = args.get("startIndex");
        String offsetStr = args.get("offset");
        if (!StringUtils.isEmpty(startIndexStr) && !StringUtils.isEmpty(offsetStr)) {
            int startIndex = Integer.parseInt(startIndexStr);
            int offset = Integer.parseInt(offsetStr);
            if (startIndex + offset > totalCount) {
                offset = totalCount - startIndex;
            }
            stateMap.put("startIndex", startIndexStr);
            stateMap.put("offset", offset + "");
        } else {
            stateMap.put("startIndex", 1 + "");
            stateMap.put("offset", totalCount + "");
        }
        extendParams.put(MonitorLog.DEAL_DETAILRECORD_COST_TIME, System.currentTimeMillis() - processStartTime);
        Object[] result = new Object[]{newRetData, stateMap};
        return result;
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

    public static void main(String[] args) throws Exception {
        Map<String,String> jsonArgsStr = new HashMap<String, String>();
        jsonArgsStr.put("startTime","20130901000000");
        jsonArgsStr.put("endTime","20131130590100");
        List<String> list = monthsBetween(jsonArgsStr);
        for(String month : list){
            System.out.println("month="+month);
        }
//        isTimeArgsLegal("20010229230059");
    }

}
