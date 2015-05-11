package com.asiainfo.billing.drquery.process.core.process;

import com.ailk.oci.ocnosql.client.jdbc.HbaseJdbcHelper;
import com.ailk.oci.ocnosql.client.jdbc.phoenix.PhoenixJdbcHelper;
import com.ailk.oci.ocnosql.common.config.Connection;
import com.ailk.oci.ocnosql.common.rowkeygenerator.MD5RowKeyGenerator;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.BaseProcess;
import com.asiainfo.billing.drquery.process.core.DataSourceRoute;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.core.request.DRProcessRequest;
import com.asiainfo.billing.drquery.process.core.request.ProcessRequest;
import com.asiainfo.billing.drquery.process.dto.BaseDTO;
import com.asiainfo.billing.drquery.process.dto.DRResultDTO;
import com.asiainfo.billing.drquery.process.dto.model.ResMsg;
import com.asiainfo.billing.drquery.utils.DateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by wangkai8 on 15/4/20.
 * this process is for update operation
 */
public class GPRSFlowUnpdateProcess<T extends CommonDRProcessRequest> implements BaseProcess<T>{

    private final static Log log = LogFactory.getLog(GPRSFlowUnpdateProcess.class);


    private String tablePrefix;//表名前缀

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }


    @Override
    public <E extends BaseDTO> E process(T rq, MetaModel meta, Map extendParams) throws ProcessException, BusinessException {
        if("appUpdate".equals(rq.getInterfaceType())) {
            return appUpdate(rq, meta, extendParams);
        } else if ("detailUpdate".equals(rq.getInterfaceType())) {
            return detailUpdate(rq, meta, extendParams);
        } else {
            throw new ProcessException("interface type invalid: " + rq.getInterfaceType());
        }

    }


    public <E extends BaseDTO> E appUpdate(T rq, MetaModel meta, Map extendParams) throws ProcessException, BusinessException {
        HbaseJdbcHelper jdbcHelper = new PhoenixJdbcHelper();
        String phone = rq.getJsonArgsStr().get("phoneNo");
        String startTime = rq.getJsonArgsStr().get("startTime");
        String endTime = rq.getJsonArgsStr().get("endTime");
        String prefix = (String) new MD5RowKeyGenerator().generatePrefix(phone);
        String startKey = prefix + phone + startTime;
        String stopKey = prefix + phone + endTime + "g";
        List<String> months = monthsBetween(rq.getJsonArgsStr());
        Configuration conf = Connection.getInstance().getConf();
        byte[] family = "F".getBytes();
        byte[] column = "APP_EXT_FLAG".getBytes();
        byte[] value = "1".getBytes();
        for(String month : months) {
            String tableName = tablePrefix + month;
            try {
                HTable table = new HTable(conf, tableName.getBytes());
                List<Put> putList = new ArrayList<Put>();
                StringBuffer select = new StringBuffer("select id, start_time from ").append(tableName).append(" where ")
                    .append("id>='").append(startKey).append("' and id<='").append(stopKey).append("' and BUSI_ID='").append(rq.getJsonArgsStr().get("appId")).append("'");
                log.info(select.toString());
                List<Map<String, Object>> result = jdbcHelper.executeQuery(select.toString());
                for(Map<String, Object> record : result) {
                    Put put = new Put(((String)record.get("ID")).getBytes());
                    put.add(family, column, value);
                    putList.add(put);
                }
                table.put(putList);
            } catch (Exception e) {
                throw new ProcessException("execute update exception", e);
            }
        }
        E dto = (E) new DRResultDTO();
        ResMsg msg = new ResMsg();
        msg.setRetCode("0");
        dto.setResMsg(msg);
        return dto;
    }

    public <E extends BaseDTO> E detailUpdate(T rq, MetaModel meta, Map extendParams) throws ProcessException, BusinessException {
        String rowkey = rq.getJsonArgsStr().get("rowkey");
        String startTime = rq.getJsonArgsStr().get("startTime");
        String tableName = tablePrefix + startTime.substring(0,6);
        Configuration conf = Connection.getInstance().getConf();
        HbaseJdbcHelper jdbcHelper = new PhoenixJdbcHelper();
        try {
            List<Map<String, Object>> result = jdbcHelper.executeQuery("select id,start_time from " + tableName + " where id='" + rowkey + "'");
            if(result.size() > 0) {
                HTable table = new HTable(conf, tableName.getBytes());
                Put put = new Put(rowkey.getBytes());
                put.add("F".getBytes(), "DETAIL_EXT_FLAG".getBytes(), "1".getBytes());
                table.put(put);
            } else {
                log.warn("rowkey: " + rowkey + " is not exist in table: " + tableName);
            }
        } catch (Exception e) {
            throw new ProcessException("execute update exception", e);
        }
        E dto = (E) new DRResultDTO();
        ResMsg msg = new ResMsg();
        msg.setRetCode("0");
        dto.setResMsg(msg);
        return dto;
    }




    public static List<String> monthsBetween(Map<String, String> jsonArgsStr) {
        List<String> monthsList = new ArrayList<String>();
        String startTimeStr = jsonArgsStr.get("startTime");
        String endTimeStr = jsonArgsStr.get("endTime");
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        int interValMonths;
        try {
            interValMonths = DateUtil.getMonths(df.parse(startTimeStr), df.parse(endTimeStr));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}