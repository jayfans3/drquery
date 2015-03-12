package com.asiainfo.billing.drquery.process.core;

import com.asiainfo.billing.drquery.datasource.BaseDataSource;
import com.asiainfo.billing.drquery.datasource.DataSourceException;
import com.asiainfo.billing.drquery.datasource.query.DRQueryParameters;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.exception.DrqueryRuntimeException;
import com.asiainfo.billing.drquery.model.Field;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.model.ModelReader;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.process.operation.distinct.DistinctOperation;
import com.asiainfo.billing.drquery.process.operation.merge.MergeOperation;
import com.asiainfo.billing.drquery.process.operation.summary.SummaryOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * 常用字段查询
 * @author wangyp5
 *
 * 25 Apr 2012
 * @version
 */
public class DRCommonProcess extends DRProcess<CommonDRProcessRequest>{
	
	private final static Log log = LogFactory.getLog(DRCommonProcess.class);
	protected DistinctOperation distinct;
	protected MergeOperation merge;
	protected SummaryOperation summary;
	private DataSourceRoute dataSourceRoute;

	public void setDataSourceRoute(DataSourceRoute dataSourceRoute) {
		this.dataSourceRoute = dataSourceRoute;
	}
	public void setDistinct(DistinctOperation distinct) {
		this.distinct = distinct;
	}
	public void setMerge(MergeOperation merge) {
		this.merge = merge;
	}
	public void setSummary(SummaryOperation summary) {
		this.summary = summary;
	}


	public List<String[]> loadData(CommonDRProcessRequest request, MetaModel meta, boolean isCommonQuery) throws ProcessException,BusinessException {
		String formdata = request.getFromDate();
		String thruDate = request.getThruDate();
		String month = formdata.substring(0, 6);
		BaseDataSource baseDataSource = this.dataSourceRoute.getDataSourceByTime(month, request.getDbType());
		if(baseDataSource==null){
			if(log.isErrorEnabled()){
				log.error("datasource is null,check app_process.xml");
			}
			throw new ProcessException("datasource is null,check app_process.xml");
		}
		
		List<String[]> modelList = null;
//		long t1 = System.currentTimeMillis();
		DRQueryParameters queryParameters = new DRQueryParameters();
		queryParameters.setBillId(request.getBillId());
		queryParameters.setFrom(formdata);
		queryParameters.setThru(thruDate);
		
		queryParameters.setRequestParam(request.ToMap());
		try {
			modelList = baseDataSource.loadDR(queryParameters, meta);
		} catch (DataSourceException e) {
			throw new DrqueryRuntimeException(e);
		}
		
//		long t2 = System.currentTimeMillis();
//		Map extendParams = new HashMap();
//		extendParams.put(MonitorLog.TOTAL_COUNT, modelList.size());
//		extendParams.put(MonitorLog.INTERFACE_TYPE, "GETQUERYLIST_2003");
//		extendParams.put(MonitorLog.ENTRY_POINT, "OCNOSQL.query");
//		MonitorLog mlog = new MonitorLog(request, true, t2 - t1, extendParams);
//		LogUtils.getMonitorLogger().info(mlog);
		
		return modelList;
	}
	

	public List<Map<String,String>> loadData(String sql, Object[] args) throws ProcessException,BusinessException {
		BaseDataSource baseDataSource = this.dataSourceRoute.getDataSourceByTime(null,"ocnosqlDataSource");
		if(baseDataSource==null){
			if(log.isErrorEnabled()){
				log.error("datasource is null,check app_process.xml");
			}
			throw new ProcessException("datasource is null,check app_process.xml");
		}
		List<Map<String,String>> modelList = null;
		try {
			modelList = baseDataSource.loadDR(sql, args);
		} catch (DataSourceException e) {
			throw new DrqueryRuntimeException(e);
		}
		return modelList;
	}

	@Override
	@SuppressWarnings("unchecked")
	public DRProcessDTO process(CommonDRProcessRequest request, MetaModel meta,Map extendParams) throws ProcessException,BusinessException {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, String> convertRowDataToUniformStructure(String[] rowData, String modelId){
    	Map<String, Field> fields = ModelReader.getMetaModels().get(modelId).getFields();
    	Map<String, String> map = new HashMap();
    	
    	for(Iterator it = fields.keySet().iterator(); it.hasNext();){
    		String key = (String)it.next();
    		String value  =  rowData[fields.get(key).getIndex() - 1];
    		map.put(key,value);
    	}
    	return map;
    }
}
