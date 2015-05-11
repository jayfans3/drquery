package com.asiainfo.billing.drquery.process.core;

import com.asiainfo.billing.drquery.cache.ICache;
import com.asiainfo.billing.drquery.cache.support.RedisSwitch;
import com.asiainfo.billing.drquery.exception.BusinessException;
import com.asiainfo.billing.drquery.exception.DrqueryRuntimeException;
import com.asiainfo.billing.drquery.model.*;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.core.request.DRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.BaseDTO;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.process.dto.DRResultDTO;
import com.asiainfo.billing.drquery.process.dto.model.ResMsg;
import com.asiainfo.billing.drquery.process.dto.model.Status;
import com.asiainfo.billing.drquery.process.operation.*;
import com.asiainfo.billing.drquery.process.operation.fieldEscape.FieldEscapeOperation;
import com.asiainfo.billing.drquery.utils.PropertiesUtil;
import org.apache.commons.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 详单查询Process基类 </p> 定义针对所有详单查询的基础业务逻辑方法
 * 
 * @author Rex Wong
 * 
 *         14 May 2012
 * @version
 */
public abstract class DRProcess<T extends DRProcessRequest> implements BaseProcess<T> {
	private static final Log log = LogFactory.getLog(DRProcess.class);
	public int restrictIndex = Integer.parseInt(PropertiesUtil.getProperty("drquery.service/runtime.properties", "restrictIndex","10000"));//最大返回值限制
	
	protected FieldEscapeOperation fieldEscape;
	protected RedisSwitch redisSwitch;
	protected ICache redisCache;
    protected boolean isCache = false;
		
	@Autowired
	public void setRedisSwitch(RedisSwitch redisSwitch) {
		this.redisSwitch = redisSwitch;
	}

	@Autowired
	public void setRedisCache(ICache redisCache) {
		this.redisCache = redisCache;
	}
	
	public void setFieldEscape(FieldEscapeOperation fieldEscape) {
		this.fieldEscape = fieldEscape;
	}

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public BaseDTO pagingProcess(T request, Map extendParams) throws ProcessException, BusinessException {
		long startTime = System.currentTimeMillis();
		redisSwitch.checkRedisStatus();
		List<Map<String, String>> result = new ArrayList<Map<String, String>> ();
		List<Map<String,String>> modelList = null; //详单明细
		List<Map<String,Object>> sumList = null; //汇总数据
        Map<String ,String> jsonArgsStr = request.getJsonArgsStr();
        String startIndexStr = jsonArgsStr.get("startIndex");
        String offsetStr =  jsonArgsStr.get("offset");
        int startIndex = 1;
        int offset = 10;

        if(!StringUtils.isEmpty(startIndexStr)){
          startIndex = Integer.valueOf(startIndexStr);
        }
        if(!StringUtils.isEmpty(offsetStr)){
           offset = Integer.valueOf(offsetStr);
        }
        int totalCount = 0;
        log.info("startIndexStr=" + startIndexStr + " offsetStr=" + offsetStr);
		extendParams.put(MonitorLog.CACHE_FLAF, 0);
		MetaModel viewMeta = ViewModelReader.getMetaModels().get(((CommonDRProcessRequest)request).getProcessType());
		
		if(viewMeta == null){
			throw new ProcessException("meta model["+request.getBillType()+"] is null," +
					"do you define value in Spring context? " +
					"we need define model xml file in mapping dir<drquery.model> and clean web server cache to handle this error.");
		}

		if(viewMeta.isUseCache()){
            Map<String,List<Map>> cacheMap = redisCache.getValue(request.generateCacheKey());
            if(cacheMap != null){
                sumList = (List)cacheMap.get("statInfo");
                List<Map<String,String>> dataList = (List)cacheMap.get("dataInfo");
                totalCount = (Integer)sumList.get(0).get("totalCount");
                if(totalCount == 0) {
                    try{
                        DRResultDTO dto = new DRResultDTO();
                        dto.setResMsg(ResMsg.buildSuccess());
                        return dto;
                    }catch(Exception e){
                        throw new DrqueryRuntimeException(e);
                    }
                }

                if(offset < 0) {
                    if(startIndex > totalCount) {
                        modelList = new ArrayList<Map<String, String>>();
                    } else {
                        modelList = dataList.subList(startIndex - 1, dataList.size());
                    }
                } else {
                    if(startIndex > totalCount) {
                        modelList = new ArrayList<Map<String, String>>();
                    } else {
                        if(startIndex + offset - 1 > dataList.size()) {
                            modelList = dataList.subList(startIndex -1, dataList.size());
                        } else {
                            modelList = dataList.subList(startIndex -1, startIndex - 1 + offset);
                        }
                    }
                }

                extendParams.put(MonitorLog.CACHE_FLAF, 1);
                result = modelList;
            }
		}

		if(CollectionUtils.isEmpty(modelList)){
			DRProcessDTO processdtos = null;
			processdtos = this.process(request, viewMeta,extendParams);
			modelList = processdtos.getdRModels();
            sumList =  processdtos.getSumList();
			if(modelList != null){
				Map<String,Object> stats = processdtos.getStats();
                if(!viewMeta.isUseCache()){
                    totalCount = (Integer)stats.get("totalCount");
                    result = modelList;
                } else {
                    totalCount = modelList.size();
                    if(offset < 0) {
                        if(startIndex > totalCount) {
                            result = new ArrayList<Map<String, String>>();
                        } else {
                            result = modelList.subList(startIndex - 1, modelList.size());
                        }
                    } else {
                        if(startIndex > totalCount) {
                            result = new ArrayList<Map<String, String>>();
                        } else {
                            if(startIndex + offset - 1 > modelList.size()) {
                                result = modelList.subList(startIndex -1, modelList.size());
                            } else {
                                result = modelList.subList(startIndex -1, startIndex - 1 + offset);
                            }
                        }
                    }
                }
			}
		}
		DRResultDTO dto = new DRResultDTO();
		dto.setSums(sumList);
        //if(!viewMeta.isUseCache()){
            Status pagingInfo = new Status();
            pagingInfo.setStartIndex(startIndex);
            pagingInfo.setOffset(offset);
            pagingInfo.setTotalCount(totalCount);
            dto.setStatus(pagingInfo);
        //}
		try {
            //result = fieldEscape.execute(result, viewMeta, request); //转义
            dto.setReplyDisInfo(result);
			dto.setResMsg(ResMsg.buildSuccess());
		} catch (Exception e) {
			throw new DrqueryRuntimeException(e);
		}finally{
			long endTime = System.currentTimeMillis();
			log.info("<!- execute query token: " + (endTime - startTime) + "ms >");
		}
		return dto;
	}

}
