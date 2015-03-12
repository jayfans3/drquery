package com.asiainfo.billing.drquery.datasource;

import java.util.List;
import java.util.Map;

import com.asiainfo.billing.drquery.datasource.query.DRQueryBusiTypes;
import com.asiainfo.billing.drquery.datasource.query.DRQueryParameters;
import com.asiainfo.billing.drquery.model.MetaModel;

/**
 * @author tianyi
 */
public interface BaseDataSource {
	
	/**
	 * 数据查询
	 * 
	 * @param queryParameters
	 * @param metaModel
	 * @return
	 * @throws DataSourceException
	 */
	List<String[]> loadDR(DRQueryParameters queryParameters,MetaModel metaModel) throws DataSourceException;

    /**
	 * 数据查询  通过sql语句查询
	 *
	 * @param sql
	 * @param args
	 * @return
	 * @throws DataSourceException
	 */
	List<Map<String,String>> loadDR(String sql,Object[] args) throws DataSourceException;

//	/**
//	 * 用户业务类型查询
//	 * 
//	 * @param queryParameters
//	 * @return
//	 * @throws DataSourceException
//	 */
//	Map<String,String> queryTypes(DRQueryBusiTypes queryParameters) throws DataSourceException;
//        
//        /**
//	 * 清单查询
//	 * 
//	 * @param queryParameters
//	 * @return
//	 * @throws DataSourceException
//	 */
//	List<Map<String,String>> queryCheck(DRQueryParameters queryParameters,MetaModel metaModel) throws DataSourceException;
	
}
