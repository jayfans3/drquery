package com.asiainfo.billing.drquery.process.core;

import com.asiainfo.billing.drquery.datasource.BaseDataSource;
import com.asiainfo.billing.drquery.datasource.DataSourceException;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.exception.DrqueryRuntimeException;
import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.process.operation.distinct.DistinctOperation;
import com.asiainfo.billing.drquery.process.operation.merge.MergeOperation;
import com.asiainfo.billing.drquery.process.operation.summary.SummaryOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;


/**
 * 常用字段查询
 * @author wangyp5
 *
 * 25 Apr 2012
 * @version
 */
public  class DRCommonProcess extends DRProcess<CommonDRProcessRequest>{
	
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
	public  DRProcessDTO process(CommonDRProcessRequest request, MetaModel meta,Map extendParams) throws ProcessException,BusinessException {
        return null;
    }


}
