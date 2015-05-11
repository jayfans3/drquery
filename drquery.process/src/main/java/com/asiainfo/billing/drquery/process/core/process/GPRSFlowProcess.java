package com.asiainfo.billing.drquery.process.core.process;

import com.ailk.oci.ocnosql.common.rowkeygenerator.MD5RowKeyGenerator;
import com.asiainfo.billing.drquery.cache.CacheProvider;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.Executor;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.DRCommonProcess;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.utils.DateUtil;
import com.asiainfo.billing.drquery.utils.NumberUtils;
import com.asiainfo.billing.drquery.utils.PropertiesUtil;
import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by wangkai8 on 15/5/8.
 */
public class GPRSFlowProcess extends DRCommonProcess {

    public static final int timeout = Integer.parseInt(PropertiesUtil.getProperty("drquery.service/runtime.properties", "redis.expiretime", "300"));

    public static final Log log = LogFactory.getLog(GPRSFlowProcess.class);
    @Override
    public DRProcessDTO process(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams) throws ProcessException, BusinessException {
        if("F1".equals(request.getInterfaceType())) {
            return processF1(request, viewMeta, extendParams);
        } else if("F2".equals(request.getInterfaceType())) {
            return processF2(request, viewMeta, extendParams);
        } else if("F3".equals(request.getInterfaceType()) || "F4".equals(request.getInterfaceType()) || "F5".equals(request.getInterfaceType())) {
            return processF3(request, viewMeta, extendParams);
        }  else if("F6".equals(request.getInterfaceType()) || "F7".equals(request.getInterfaceType()) || "F8".equals(request.getInterfaceType())) {
            return processF6(request, viewMeta, extendParams);
        }
        return null;
    }


    public DRProcessDTO processF1(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams) throws ProcessException, BusinessException {
        DRProcessDTO dto = new DRProcessDTO();
        //String currentDate = DateUtil.dateToString(new Date(), "YYYYMMdd");
        String rowkey = (String) new MD5RowKeyGenerator().generate(request.getJsonArgsStr().get("phoneNo"));
        String sql = "select PHONE_NO,NAME,EPARCHY_CODE,FLOW_PLAN_ID,MAIN_PLAN_FLAG,FREE_FLOW,USED_FLOW,REMAIN_FLOW,USER_TERM_BRAND,USER_TERM_MODEL from THB_USER_INFO_DAY " +
                "where id='" + rowkey + "'";
        List<Map<String, String>> list = loadData(sql, null);
        dto.setdRModels(list);
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("totalCount", list.size());
        dto.setStats(stats);
        return dto;
    }


    public DRProcessDTO processF2(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams) throws ProcessException, BusinessException {
        DRProcessDTO dto = new DRProcessDTO();
        if("1".equals(request.getJsonArgsStr().get("isCurrentMonth"))) {
            return processF1(request, viewMeta, extendParams);
        }

        String rowkey = new MD5RowKeyGenerator().generatePrefix(request.getJsonArgsStr().get("phoneNo")) + request.getJsonArgsStr().get("phoneNo");
        String sql = "select PHONE_NO,NAME,EPARCHY_CODE,FLOW_PLAN_ID,MAIN_PLAN_FLAG,FREE_FLOW,USED_FLOW,REMAIN_FLOW,USER_TERM_BRAND,USER_TERM_MODEL "+
                " from THB_USER_INFO_" + request.getJsonArgsStr().get("dataMonth") +
                " where id='" + rowkey + "'";
        List<Map<String, String>> list = loadData(sql, null);
        dto.setdRModels(list);
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("totalCount", list.size());
        dto.setStats(stats);
        return dto;
    }


    public DRProcessDTO processF3(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams) throws ProcessException, BusinessException {
        String startTime = request.getJsonArgsStr().get("startDate");
        String endTime = request.getJsonArgsStr().get("endDate");
        //DateUtil.isTimeLegal(startTime);
        //DateUtil.isTimeLegal(endTime);
        String phoneNo = request.getJsonArgsStr().get("phoneNo");
        DRProcessDTO dto = new DRProcessDTO();
        List<String> months = DateUtil.getMonthsBetween(startTime, endTime, "yyyyMMdd");

        String sql = "";
        String md5Phone = (String) new MD5RowKeyGenerator().generate(phoneNo);
        String startKey = md5Phone + startTime;
        String stopKey = md5Phone + endTime + "g";
        for(String month : months) {
             sql += "select $0, " +
                    " sum(to_number(gprs_flow)) gprs_flow, sum(to_number(lte_flow)) lte_flow, " +
                     "sum(to_number(roam_flow)) roam_flow, sum(to_number(http_flow)) http_flow" +
                    " from THB_USER_FLOW_" + month  +
                    " where id >= '" + startKey + "' and id < '"+ stopKey + "' group by phone_No, $1 union all ";
        }
        sql = sql.substring(0, sql.lastIndexOf("union"));
        if("F3".equals(request.getInterfaceType())) {
            sql = sql.replace("$0", "data_date").replace("$1", "data_date");
        } else if("F4".equals(request.getInterfaceType())) {
            sql = sql.replace("$0", "PROTOCOL_TYPE_ID").replace("$1", "PROTOCOL_TYPE_ID");
        } else if("F5".equals(request.getInterfaceType())) {
            sql = sql.replace("$0", "BUSI_TYPE_ID").replace("$1", "BUSI_TYPE_ID");
        }
        List<Map<String, String>> list = loadData(sql, null);
        long totalFlow = 0;
        for(Map<String, String> record : list) {
            totalFlow += NumberUtils.parseLong(record.get("GPRS_FLOW"));
        }
        for(Map<String, String> record : list) {
            if("F3".equals(request.getInterfaceType())) {
                if(totalFlow != 0) {
                    record.put("FLOW_CNT", (NumberUtils.parseDouble(record.get("GPRS_FLOW")) / totalFlow) + "");
                } else {
                    record.put("FLOW_CNT", "0");
                }
            } else if("F4".equals(request.getInterfaceType())) {
                if(totalFlow != 0) {
                    record.put("PROTOCOL_TYPE_CNT", (NumberUtils.parseDouble(record.get("GPRS_FLOW")) / totalFlow) + "");
                } else {
                    record.put("PROTOCOL_TYPE_CNT", "0");
                }
            } else if("F5".equals(request.getInterfaceType())) {
                if(totalFlow != 0) {
                    record.put("BUSI_TYPE_CNT", (NumberUtils.parseDouble(record.get("GPRS_FLOW")) / totalFlow) + "");
                } else {
                    record.put("BUSI_TYPE_CNT", "0");
                }
            }
        }
        dto.setdRModels(list);
        Map<String, Object> cache = new HashMap<String, Object>();
        Map<String, Object> statInfo = new HashMap<String, Object>();
        statInfo.put("totalCount", list.size());
        cache.put("statInfo", statInfo);
        cache.put("dataInfo", list);
        redisCache.putData2Cache(request.generateCacheKey(), cache, true);
        return dto;
    }


    public DRProcessDTO processF6(CommonDRProcessRequest request, MetaModel viewMeta, final Map extendParams) throws ProcessException, BusinessException {

        DRProcessDTO dto = new DRProcessDTO();

        String phoneNo = request.getJsonArgsStr().get("phoneNo");
        int startIndex = Integer.valueOf(request.getJsonArgsStr().get("startIndex"));
        int limit = -1;
        String offset = request.getJsonArgsStr().get("offset");
        if(StringUtils.isNotEmpty(offset)) {
            limit = Integer.parseInt(offset);
        }

        String cacheKey = request.generateCacheKey();
        int countCache = -1;
        String pageCache = null;
        List counterAndRowKey = CacheProvider.getCountAndRowkeyInfo(cacheKey, startIndex);
        if(counterAndRowKey.get(0) != null) {  //不是第一次查询
            countCache = (Integer) counterAndRowKey.get(0);
            pageCache = (String) counterAndRowKey.get(1);
        }

        String sql = "", countQuery = "", detailQuery = "";

//        String startKey;
//        if(pageCache != null) {
//            startKey = pageCache;
//        } else {
//            startKey = md5Phone + startTime;
//        }
//        String stopKey = md5Phone + endTime + "g";

        String[] sqls = buildSQL(countCache, pageCache, limit, request);

        List<Future<List<Map<String, String>>>> futures = new ArrayList<Future<List<Map<String, String>>>>();
        for(int i = 0; i < sqls.length; i++) {
            final String querySql = sqls[i];
            Callable<List<Map<String, String>>> callable = new Callable<List<Map<String, String>>>() {
                @Override
                public List<Map<String, String>> call() throws Exception {
                    return loadData(querySql, null);
                }
            };
            futures.add(Executor.getInstance().getThreadPool().submit(callable));
        }
        int i = 0;
        List<Map<String, String>> countRecords = null;
        List<Map<String, String>> detailRecords = null;
        try {
            for (Future<List<Map<String, String>>> future : futures) {
                if(i == 0 && countCache == -1)
                    countRecords = future.get();
                else
                    detailRecords = future.get();
                i ++;
            }
        } catch (Exception e) {
            throw new ProcessException("execute query exception", e);
        }
        int totalCount = 0;
        if(countCache == -1) {
            for (Map<String, String> record : countRecords) {
                totalCount += Integer.parseInt(record.get("C"));
            }
            CacheProvider.put(cacheKey, -9, totalCount, timeout);
        } else {
            totalCount = countCache;
        }
        String nextKey = null;
        if(limit != -1 && detailRecords.size() == limit + 1) {
            nextKey = detailRecords.get(detailRecords.size() -1).get("ID");
            CacheProvider.put(cacheKey, startIndex + limit, nextKey, timeout);
            detailRecords = detailRecords.subList(0, limit);
        }
        dto.setdRModels(detailRecords);
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("totalCount", totalCount);
        dto.setStats(stats);
        return dto;
    }



    public String[] buildSQL(int countCache, String pageCache, int limit, CommonDRProcessRequest request) {
        String md5Phone = (String) new MD5RowKeyGenerator().generate(request.getJsonArgsStr().get("phoneNo"));
        String sql = "", countQuery = "", detailQuery = "", startKey = "", stopKey = "";
        if("F6".equals(request.getInterfaceType())) {
            String startTime = request.getJsonArgsStr().get("startDate");
            String endTime = request.getJsonArgsStr().get("endDate");
//            DateUtil.isTimeLegal(startTime);
//            DateUtil.isTimeLegal(endTime);
            List<String> months = DateUtil.getMonthsBetween(startTime, endTime, "yyyyMMdd");
            if(pageCache == null)
                startKey = md5Phone + startTime;
            else
                startKey = pageCache;
            stopKey = md5Phone + endTime + "g";
            for(String month : months) {
                sql +=  "select $1 "+
                        " from THB_GPRS_CHARGE_"  + month  +
                        " where id >= '" + startKey + "' and id < '"+ stopKey + "' union all ";
            }
            sql = sql.substring(0, sql.lastIndexOf("union"));
            countQuery = sql.replace("$1", "count(1) as c");
            detailQuery = sql.replace("$1", "ID,DATA_DATE,PHONE_NO,CHARGING_ID,EPARCHY_CODE,START_TIME,DURATION,CHARGE_FLOW,TOTAL_FLOW");
        } else if("F7".equals(request.getInterfaceType())) {
            if(pageCache == null)
                startKey = md5Phone + request.getJsonArgsStr().get("dataDate");
            else
                startKey = pageCache;
            stopKey = md5Phone + request.getJsonArgsStr().get("dataDate") + "g";
            sql +=  "select $1 "+
                    " from THB_GPRS_FLOW_"  + request.getJsonArgsStr().get("dataDate").substring(0, 6)  +
                    " where id >= '" + startKey + "' and id < '"+ stopKey + "' and data_date='" + request.getJsonArgsStr().get("dataDate") + "'" +
                    "  and CHARGING_ID='" + request.getJsonArgsStr().get("chargingId") + "'" +
                    //"  and to_date(start_time, 'yyyyMMddHHmmss') >= to_date('" + request.getJsonArgsStr().get("startTime") + "', 'yyyyMMddHHmmss') - 0.0000115740741 * 60 * 5 " +
                    "  and to_date(start_time, 'yyyyMMddHHmmss') >= to_date('" + request.getJsonArgsStr().get("startTime") + "', 'yyyyMMddHHmmss')" +
                    " union all ";
            sql = sql.substring(0, sql.lastIndexOf("union"));
            countQuery = sql.replace("$1", "count(1) as c");
            detailQuery = sql.replace("$1", "ID,DATA_DATE,PHONE_NO,CHARGING_ID,NET_TYPE,ACCESS_MODE,TERM_BRAND,TERM_MODEL,START_TIME,DURATION,TOTAL_FLOW,UP_FLOW,DOWN_FLOW,PROTOCOL_TYPE_ID,BUSI_ID,BUSI_REMARK,substr(start_time, 9, 2) hour_id");
        } else if("F8".equals(request.getInterfaceType())) {
            if(pageCache == null)
                startKey = md5Phone + request.getJsonArgsStr().get("dataTime");
            else
                startKey = pageCache;
            stopKey = md5Phone + request.getJsonArgsStr().get("dataTime") + "g";
            sql +=  "select $1 "+
                    " from THB_GPRS_WAP_"  + request.getJsonArgsStr().get("dataTime").substring(0, 6)   +
                    " where id >= '" + startKey + "' and id < '"+ stopKey + "' and data_time='" + request.getJsonArgsStr().get("dataTime") + "'" +
                    "  and CHARGING_ID='" + request.getJsonArgsStr().get("chargingId") + "'" +
                    "  and busi_id='" + request.getJsonArgsStr().get("busiId") +  "'" +
                    //"  and to_date(start_time, 'yyyyMMddHHmmss') >= to_date('" + request.getJsonArgsStr().get("startTime") + "', 'yyyyMMddHHmmss') - 0.0000115740741 * 60 * 5 " +
                    "  and to_date(start_time, 'yyyyMMddHHmmss') >= to_date('" + request.getJsonArgsStr().get("startTime") + "', 'yyyyMMddHHmmss') " +
                    " union all ";
            sql = sql.substring(0, sql.lastIndexOf("union"));
            countQuery = sql.replace("$1", "count(1) as c");
            detailQuery = sql.replace("$1", "ID,DATA_TIME,PHONE_NO,CHARGING_ID,BUSI_ID,START_TIME,DURATION,TOTAL_FLOW,UP_FLOW,DOWN_FLOW,BUSI_REMARK");
        }

        if(limit > 0) {
            detailQuery += " limit "+ (limit + 1);
        }

        String[] sqls;
        if(countCache == -1) {
            sqls = new String[]{countQuery, detailQuery};
        } else {
            sqls = new String[]{detailQuery};
        }
        return sqls;
    }

}
