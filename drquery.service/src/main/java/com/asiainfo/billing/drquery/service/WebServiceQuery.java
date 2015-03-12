package com.asiainfo.billing.drquery.service;

import com.alibaba.druid.support.json.*;
import com.alibaba.druid.support.json.JSONUtils;
import com.asiainfo.billing.drquery.controller.reponse.*;
import com.asiainfo.billing.drquery.exception.*;
import com.asiainfo.billing.drquery.model.*;
import com.asiainfo.billing.drquery.process.*;
import com.asiainfo.billing.drquery.process.core.*;
import com.asiainfo.billing.drquery.process.core.request.*;
import com.asiainfo.billing.drquery.process.dto.*;
import com.asiainfo.billing.drquery.process.dto.model.*;
import com.asiainfo.billing.drquery.process.operation.*;
import com.asiainfo.billing.drquery.utils.*;
import com.google.common.util.concurrent.*;
import net.sf.json.*;
import net.sf.json.util.*;
import org.apache.commons.collections.map.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;
import org.apache.hadoop.hdfs.web.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lile3
 * Date: 13-12-7
 * Time: 上午9:57
 * To change this template use File | Settings | File Templates.
 */
public class WebServiceQuery {

    private static Log log = LogFactory.getLog(WebServiceQuery.class);
	private static IExceptionHandler handler = new ExceptionLogHandler();
    private static final String ENCODING_TYPE="UTF-8";
    private static final int RECORD_BYTES = 1000;//估设置单条记录转换出json的最大字节数，按照上网详单为最大记录

    @SuppressWarnings("unchecked")
    private static ProcessFactory<CommonDRProcessRequest> processFactory = ServiceLocator.getInstance().
            getService("processFactory", ProcessFactory.class);



    /**
     * 通过webservice方式暴露给CMOD系统的查询接口
     * @param jsonRequest  json类型的请求参数
     * @param interfaceType  请求类型（如：F11,F12）
     * @return  结果以json字符串返回
     */
	public static String query(String jsonRequest,String interfaceType){
		BaseDTO dto = new DRProcessDTO();
		boolean isSuccess = false;
        String resultJson;
		long t1 = System.currentTimeMillis();
        CommonDRProcessRequest request = new CommonDRProcessRequest();
        request.setInterfaceType(interfaceType);
		DRProcess<CommonDRProcessRequest> process = null;
        Map extendParams = new LinkedMap();
        try{
            Map<String,Object> jsonArgs = JsonUtils.parserToMap(jsonRequest);
            Object obj = jsonArgs.get("qryCond");
            Map<String,String> jsonArgsStr = new HashMap<String,String>();
            if(obj instanceof Map){
                jsonArgsStr = (Map)obj;
            }
            request.setJsonArgsStr(jsonArgsStr); //请求参数
			process = resolveProcess(request.getProcessType());
            dto = process.pagingProcess(request,extendParams);
			isSuccess = true;
		}catch(BusinessException ex){
			handlerBusinessException(ex, dto);
		}catch(ProcessException ex){
			handlerSystemException(ex, dto);
		}catch(RuntimeException ex){
			handlerSystemException(ex, dto);
		}finally{
            resultJson = transferBaseDTOtoJSON(dto,interfaceType);
			long t2 = System.currentTimeMillis();
            int statesStartIndex = resultJson.indexOf("stats");
            if(statesStartIndex >= 0){
                if(resultJson.substring(statesStartIndex,statesStartIndex+14).contains("null")){
                    extendParams.put(MonitorLog.RESULT_STATS,"null");
                }else{
                    int statesEndIndex = resultJson.indexOf("}",statesStartIndex);
                    extendParams.put(MonitorLog.RESULT_STATS,resultJson.substring(statesStartIndex,statesEndIndex+1));
                }
            }else{
                extendParams.put(MonitorLog.RESULT_STATS,"null");
            }
			extendParams.put(MonitorLog.ITF_NAME, interfaceType);
            extendParams.put(MonitorLog.ITF_PARAM,jsonRequest);
            extendParams.put(MonitorLog.START_TIME,new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(t1)));
            extendParams.put(MonitorLog.COST_TIME,t2 - t1);
            extendParams.put(MonitorLog.DEAL_STATE,isSuccess==true?0:1);
            extendParams.put(MonitorLog.REMARK,"暂无");
			MonitorLog mlog = new MonitorLog(request,extendParams);
			LogUtils.getMonitorLogger().info(mlog);
		}
        return resultJson;
	}


    private static String transferBaseDTOtoJSON(BaseDTO dto,String interfaceType){
        ResMsg msg = null;
        Map<String,Object> retMap;
        msg = dto.getResMsg();
        if("0".equals(msg.getRetCode())) {
                retMap = new DataResponse(dto.getReplyDisInfo(),msg,dto.getStatus(),dto.getSums()).toMap();
        }else{
               retMap = new FailResponse(msg).toMap();
        }
        String retJsonStr="";
        try{
            ObjectMapper om = new ObjectMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();//取默认值(recordSize>1?RECORD_BYTES*recordSize:RECORD_BYTES)根据返回的记录数设置临时输出流的大小
            JsonGenerator jsonGenerator = om.getJsonFactory().createJsonGenerator(baos, JsonEncoding.UTF8);
            om.writeValue(jsonGenerator, retMap);//map to json
            retJsonStr = new String(baos.toByteArray(),ENCODING_TYPE);
        }catch(Exception e){
            JSONObject json = new JSONObject();
			msg = new ResMsg();
			msg.setRetCode("-1");
			msg.setErrorMsg("process request failed : "+e.getMessage());
			msg.setHint("查询失败");
            json.putAll( new FailResponse(msg).toMap());
            retJsonStr = json.toString();
        }
        log.info("retJsonStr = " + retJsonStr);
        if(interfaceType.trim().equalsIgnoreCase("F11") && dto.getResMsg().getRetCode().equals("0")){
            retJsonStr = StringUtils.replace(retJsonStr,"\"stats\":","\"NOO\":");
            retJsonStr = StringUtils.replace(retJsonStr,"\"sums\":[","\"stats\":");
            int rIndex = retJsonStr.indexOf("]",retJsonStr.indexOf("stats"));
            retJsonStr = new StringBuffer(retJsonStr).replace(rIndex,rIndex+1,"").toString();
        }
        log.info("replace retJsonStr = " + retJsonStr);
        return retJsonStr;
    }

    private static DRProcess<CommonDRProcessRequest> resolveProcess(String processType) {
		DRProcess<CommonDRProcessRequest> process = null;
		try {
			process = processFactory.getProcessByBusiType(processType);
			if (process == null)
				process = processFactory.getCommProcessByBusiType(processType);
		} catch (ProcessException e) {
			new ProcessException("unsupport billtype [" + processType + "]");
		}
		return process;
	}

    public static void handlerBusinessException(BusinessException ex, BaseDTO dto){
		ExceptionMessage exMessage = handler.handlerException(ex);
		ResMsg msg = new ResMsg();
		msg.setRetCode("-1");
		msg.setHint(exMessage.getMessage());
		msg.setErrorCode(exMessage.getCode());
		dto.setResMsg(msg);
	}

	public static void handlerSystemException(Exception ex, BaseDTO dto){
		ExceptionMessage exMessage = ExceptionContext.getInstance().getExceptionMessage(ExceptionCode.ERROR_CODE_1000);
		ResMsg msg = new ResMsg();
		msg.setRetCode("-1");
		msg.setHint(exMessage.getMessage());
		msg.setErrorCode(exMessage.getCode());
		msg.setErrorMsg(ex.getMessage());
		dto.setResMsg(msg);
		log.error(ex.getMessage(), ex);
	}

}
