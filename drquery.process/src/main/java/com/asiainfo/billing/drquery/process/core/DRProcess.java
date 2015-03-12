package com.asiainfo.billing.drquery.process.core;

import com.asiainfo.billing.drquery.cache.ICache;
import com.asiainfo.billing.drquery.cache.support.CacheParameters.Range;
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
import com.asiainfo.billing.drquery.process.dto.support.*;
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
	private int restrictIndex=Integer.parseInt(PropertiesUtil.getProperty("drquery.service/runtime.properties", "restrictIndex","10000"));//最大返回值限制
	
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
	public BaseDTO pagingProcess(T request,Map extendParams) throws ProcessException, BusinessException {
		long startTime = System.currentTimeMillis();
		redisSwitch.checkRedisStatus();
		String key = request.generateCacheKey();
        String globalKey = request.generateGlobalCacheKey();
		Map<String,Object> stats = redisCache.getHashValue2Map(globalKey);//get map stats
        log.info("globalCacheKey = " + globalKey + " states size is " + stats.size());
		List<Map<String, String>> result = new ArrayList<Map<String, String>> ();
		List<Map<String,String>> modelList = null; //详单明细
		List<Map<String,Object>> sumList = null; //汇总数据
		int stopIndex=0;
		int startIndex=0;
		int totalCount = 0;
        Map<String ,String> jsonArgsStr = request.getJsonArgsStr();
        String startIndexStr = jsonArgsStr.get("startIndex");
        String offsetStr =  jsonArgsStr.get("offset");
        log.info("startIndexStr=" + startIndexStr + " offsetStr=" + offsetStr);
        boolean isNeedPaging = !StringUtils.isEmpty(startIndexStr) && !StringUtils.isEmpty(offsetStr);
		extendParams.put(MonitorLog.CACHE_FLAF,0);
		MetaModel busiMeta = BusiModelReader.getMetaModels().get(((CommonDRProcessRequest)request).getProcessType());
		
		if(busiMeta == null){
			throw new ProcessException("meta model["+request.getBillType()+"] is null," +
					"do you define value in Spring context? " +
					"we need define model xml file in mapping dir<drquery.model> and clean web server cache to handle this error.");
		}
		
		if(isNeedPaging){	
			startIndex = Integer.valueOf(startIndexStr);
			stopIndex = startIndex + Integer.valueOf(offsetStr)-1;
			if(stopIndex > restrictIndex){
				stopIndex = restrictIndex;
				if(log.isErrorEnabled()){
					log.error("the stop index["+stopIndex+"] from request larger than restrictIndex["+restrictIndex+"]," +
							"we use restrictIndex to return");
				}
			}
		}
		
		if(isNeedPaging && request.getInterfaceType().trim().equalsIgnoreCase("F12") && !CollectionUtils.isEmpty(stats)){
			if(stats.get("totalCount")!=null){
                log.info("stats = " + stats.get("totalCount"));
				totalCount = Integer.valueOf(((String)stats.get("totalCount")).trim());
				if(totalCount == 0){
					try{
						DRResultDTO dto = new DRResultDTO();
						ResMsg msg = new ResMsg(); //返回给上层的日志信息
						msg.setRetCode("0");
						msg.setErrorMsg("查询成功");
						msg.setHint("查询成功");
						dto.setResMsg(msg);
						return dto;
					}catch(Exception e){
						throw new DrqueryRuntimeException(e);
					}
				}
				if(totalCount<startIndex){
					throw new ProcessException("the startIndex["+startIndex+"] is larger than the size[totalCount:"+totalCount+"] of modelist");
				}
                if(!StringUtils.isEmpty(offsetStr) && offsetStr.equals("-1")){
                    stopIndex = totalCount;
                    offsetStr = String.valueOf(stopIndex - startIndex + 1);
                }else{
				    stopIndex=stopIndex>totalCount?totalCount:stopIndex;
                }
				Range range = new Range(startIndex,stopIndex);
				modelList = redisCache.getValue(key, range);//get list model
                extendParams.put(MonitorLog.CACHE_FLAF,1);
                log.info("from cache get modelList size =" + modelList.size());
				result = modelList;
				sumList = (List<Map<String, Object>>) stats.get("sums");
			}
		}

		if(CollectionUtils.isEmpty(modelList)){
			DRProcessDTO processdtos = null;
			processdtos = this.process(request, busiMeta,extendParams);
			modelList = processdtos.getdRModels();
			if(modelList != null){
				sumList =  processdtos.getSumList();
				stats = processdtos.getStats();
				totalCount = modelList.size();
				if(isNeedPaging){
					long t1 = System.currentTimeMillis();
					redisCache.putData2Cache(key, modelList,true);
					redisCache.putData2Cache(globalKey,stats,true);
					long t2 = System.currentTimeMillis();
					log.info("put page data to redis token: " + (t2 -t1) + "ms");
					if(modelList.size() < startIndex && modelList.size()!=0){
						throw new ProcessException("The startIndex is larger than the size of modelist, please check the pageIndex and pageSize.");
					}
                    if(!StringUtils.isEmpty(offsetStr) && offsetStr.equals("-1")){
                        stopIndex = modelList.size();
                        offsetStr = String.valueOf(stopIndex - startIndex + 1);
                    }else{
					    stopIndex = stopIndex > modelList.size() ? modelList.size() : stopIndex;
                    }
				}
			}
			if(isNeedPaging){
                startIndex = (startIndex-1) > stopIndex ? stopIndex : startIndex;
                if(startIndex == 0){
                    startIndex = 1;
                }
				result = new ArrayList<Map<String,String>>(modelList.subList(startIndex - 1, stopIndex));
			}else{
				result = modelList;
			}
		}
		DRResultDTO dto = new DRResultDTO();
		ResMsg msg = new ResMsg(); //返回给上层的日志信息
		dto.setSums(sumList);
        if(request.getInterfaceType().trim().equalsIgnoreCase("F12")){
            Status pagingInfo = new Status();
            pagingInfo.setStartIndex(startIndex);
            int offset = StringUtils.isEmpty(offsetStr)||offsetStr.trim().equalsIgnoreCase("null")?0:Integer.parseInt(offsetStr);
            if(startIndex+offset > totalCount){
                offset = totalCount - startIndex + 1;
            }
            if(offset < 0){
                offset = 0;
            }
            pagingInfo.setOffset(offset);
            pagingInfo.setTotalCount(totalCount);
            dto.setStatus(pagingInfo);
        }
		try {
            if(request.getInterfaceType().equals("F12")){
                result = fieldEscape.execute(result, busiMeta, request); //转义
                result = transferMapKeyName(result,busiMeta);
            }
            dto.setReplyDisInfo(result);
			msg.setRetCode("0");
			msg.setErrorMsg("查询成功");
			msg.setHint("查询成功");
			dto.setResMsg(msg);
		} catch (Exception e) {
			throw new DrqueryRuntimeException(e);
		}finally{
			long endTime = System.currentTimeMillis();
			log.info("<!- execute query token: " + (endTime - startTime) + "ms >");
		}
		return dto;
	}

    private static List<Map<String, String>> transferMapKeyName(List<Map<String, String>> resultList,MetaModel busiMeta){
             List<Map<String, String>> newResultList = new ArrayList<Map<String, String>>();
             for(Map<String,String> oriResultMap : resultList){
                 Set<String> set = oriResultMap.keySet();
                 Map<String,String> newResultMap = new HashMap<String, String>();
                 for(Iterator<String> it = set.iterator();it.hasNext();){
                     String key = it.next();
                     String newKey = busiMeta.getDbNameToBackName().get(key);
                     String value = oriResultMap.get(key);
                     //log.info("key="+key +" newkey="+newKey + " value=" + value);
                     newResultMap.put(newKey,value);
                 }
                 newResultList.add(newResultMap);
             }
         return newResultList;
    }

}
