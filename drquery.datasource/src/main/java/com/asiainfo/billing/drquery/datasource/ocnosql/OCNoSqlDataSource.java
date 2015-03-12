package com.asiainfo.billing.drquery.datasource.ocnosql;

import java.math.*;
import java.sql.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ailk.oci.ocnosql.client.jdbc.*;
import com.ailk.oci.ocnosql.client.jdbc.phoenix.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ailk.oci.ocnosql.client.config.spi.Connection;
import com.ailk.oci.ocnosql.client.query.criterion.Criterion;
import com.ailk.oci.ocnosql.client.spi.ClientAdaptor;
import com.asiainfo.billing.drquery.busiTypeRule.BusiTypeRule;
import com.asiainfo.billing.drquery.busiTypeRule.BusiTypeRuleFactory;
import com.asiainfo.billing.drquery.connection.ConnectionException;
import com.asiainfo.billing.drquery.connection.ConnectionFactory;
import com.asiainfo.billing.drquery.connection.ConnectionUtils;
import com.asiainfo.billing.drquery.datasource.BaseDataSource;
import com.asiainfo.billing.drquery.datasource.DataSourceException;
import com.asiainfo.billing.drquery.datasource.query.DRQueryParameters;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.utils.DateUtil;

/**
 * 
 * @author Rex Wong
 *
 * @version
 */
public class OCNoSqlDataSource implements BaseDataSource{
	
	private final static Log log = LogFactory.getLog(OCNoSqlDataSource.class);
	
	private ConnectionFactory<OCNoSqlConnectionHolder> factory;
	private String tablePrefix;//表明前缀
	
	public void setFactory(ConnectionFactory<OCNoSqlConnectionHolder> factory) {
		this.factory = factory;
	}
	
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public List<String[]> loadDR(DRQueryParameters queryParameters, MetaModel metaModel)
			throws DataSourceException {
		OCNoSqlConnectionHolder holder = null;
		ClientAdaptor query = null;
		try {
			holder = factory.getConnectionHolder();
			query = (ClientAdaptor) holder.getNatvieConnection();
		} catch(ConnectionException ex){
			throw new DataSourceException("ocnosql exception",ex);
		}

		List<String[]> retArr = new ArrayList<String[]>();

		try{
			List<String> tableNames = buildTableNames(queryParameters, metaModel.getTable());

			BusiTypeRule queryRule = BusiTypeRuleFactory.createBusiRule(metaModel.getModelId(), queryParameters.getRequestParam());
			Criterion busiRuleQueryParam = null;
			if(queryRule != null){
				busiRuleQueryParam = queryRule.getQueryParam();
			}

			long t1 = System.currentTimeMillis();

			retArr = query.queryByRowkey(Connection.getInstance(), queryParameters.getBillId(), tableNames, busiRuleQueryParam, null);

			log.info("ocnosql return "+ retArr.size() +" records, query token: " + (System.currentTimeMillis() - t1) + "ms");
		} catch(Exception ex){
			Throwable cause = ex.getCause();
			if(cause!=null&&(cause instanceof org.apache.hadoop.hbase.TableNotFoundException)){
				throw new DataSourceException("ocnosql found exception: table not found["+cause.getMessage()+"]");
			}
			else if(StringUtils.equals(ex.getMessage(), "hostname can't be null")){
				try {
					throw new ConnectionException("get ocnosql connection failed:do you set hostname in your os?",ex);
				}
				catch (ConnectionException e) {
					log.error(e);
				}
			}
			else{
				//holder=null;
				throw new DataSourceException("ocnosql found exception",ex);
			}
		}finally{
			try {
				ConnectionUtils.releaseConnection(holder, factory);
			} catch (ConnectionException e) {
				log.error(e);
			}
		}

		return retArr;
	}


	public List<Map<String, String>> loadDR(String sql,Object[] args)
			throws DataSourceException {
		List<Map<String, String>>  retArr = new ArrayList<Map<String, String>>();
        HbaseJdbcHelper jdbcHelper = null;
        try{
			long t1 = System.currentTimeMillis();
			jdbcHelper = new PhoenixJdbcHelper();
			ResultSet rsResultSet = jdbcHelper.executeQueryRaw(sql,args);
            int columnCount = rsResultSet.getMetaData().getColumnCount();
            while (rsResultSet.next()) {
                Map<String, String> map = new HashMap<String, String>();
                for (int i = 1; i <= columnCount; i++) {
                    String metaData = rsResultSet.getMetaData().getColumnName(i);
                    Object valueData = rsResultSet.getObject(i);
                    String value=valueData + "";
                    map.put(metaData,value);
                }
                retArr.add(map);
            }
			log.info("ocnosql return "+ retArr.size() +" records, query token: " + (System.currentTimeMillis() - t1) + "ms");
		} catch(Exception ex){
			Throwable cause = ex.getCause();
			if(cause!=null&&(cause instanceof SQLException)){
				throw new DataSourceException("ocnosql exec query sqlexception,error is ["+cause.getMessage()+"]");
			}else if(StringUtils.equals(ex.getMessage(), "hostname can't be null")){
				try {
					throw new ConnectionException("get ocnosql connection failed:do you set hostname in your os?",ex);
				}catch (ConnectionException e) {
					log.error(e);
				}
			}else{
				throw new DataSourceException("ocnosql found exception",ex);
			}
		}finally{
           try{
               if(jdbcHelper != null){
                  jdbcHelper.close();
               }
           }catch (SQLException e){
              throw new DataSourceException("close connection error",e);
           }
        }
		return retArr;
	}

	/**
	 * @param queryParameters 
	 * @param tablePrefix 业务类型
	 * @return
	 */
	private List<String> buildTableNames(DRQueryParameters queryParameters,String tablePrefix){
		Date start = DateUtil.stringToUtilDate(queryParameters.getFrom(), "yyyyMMdd");
		Date end = DateUtil.stringToUtilDate(queryParameters.getThru(), "yyyyMMdd");
		List<String> intervalTime = DateUtil.getIntervalTime(start,DateUtil.getNextDate(end),"yyyyMMdd");
		
		List<String> tableNames = new ArrayList<String>();
		for(String interval:intervalTime){
			String tablename = tablePrefix + interval;
			tableNames.add(tablename);
		}
		log.info("ocnosql begin to query, rowkey="+ queryParameters.getBillId() +", beginDate=" + intervalTime.get(0) + ", endDate=" + intervalTime.get(intervalTime.size() - 1));
		return tableNames;
	}
	
	
	
    
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//	public Map<String, String> convertToUniformStructure(String[] rowData){
//    	Map<String, String> map = new HashMap();
//    	map.put("SRC_CALL_TYPE" , rowData[0]);
//    	map.put("SRC_ROAM_TYPE" , rowData[1]);
//    	map.put("SRC_USER_NUMBER" , rowData[2]);
//    	map.put("SRC_DURATION" , rowData[3]);
//    	map.put("SRC_IMSI" , rowData[4]);
//    	map.put("SRC_START_TIME" , rowData[5]);
//    	map.put("SRC_SERVICE_TYPE" , rowData[6]);
//    	map.put("SRC_BASIC_CHARGE" , rowData[7]);
//    	map.put("SRC_TOLL_CHARGE" , rowData[8]);
//    	map.put("SRC_INFO_CHARGE" , rowData[9]);
//    	map.put("SRC_VPLMN" , rowData[10]);
//    	map.put("SRC_OPP_AREA_CODE" , rowData[11]);
//    	map.put("SRC_USER_TYPE" , rowData[12]);
//    	map.put("SRC_PLAN_ID" , rowData[13]);
//    	map.put("SRC_OPP_TYPE" , rowData[14]);
//    	map.put("SRC_OPP_NUMBER" , rowData[15]);
//    	map.put("SRC_OPP_EXT_INFO" , rowData[16]);
//    	map.put("SRC_INFO_MAJOR" , rowData[17]);
//    	map.put("SRC_INFO_MINOR" , rowData[18]);
//    	map.put("SRC_HPLMN3" , rowData[19]);
//    	map.put("SRC_CARRIER_INFO" , rowData[20]);
//    	map.put("SRC_EXT_INFO" , rowData[21]);
//    	map.put("SRC_FREERES_CODE" , rowData[22]);
//    	map.put("SRC_FREERES_VAL" , rowData[23]);
//    	map.put("SRC_EXTEND_ATTR_LONGCODE1" , rowData[24]);
//    	map.put("SRC_EXTEND_ATTR_LONGCODE2" , rowData[25]);
//    	map.put("SRC_EXTEND_ATTR_LONGCODE3" , rowData[25]);
//    	map.put("SRC_EXTEND_ATTR_LONGCODE4" , rowData[27]);
//    	map.put("SRC_CHARGE_DISC" , rowData[28]);
//    	map.put("SRC_EXTEND_ATTR_SHORTCODE1" , rowData[29]);
//    	map.put("SRC_EXTEND_ATTR_SHORTCODE2" , rowData[30]);
//    	map.put("SRC_BILL_MONTH" , rowData[31]);
//    	return map;
//    }
        
}
