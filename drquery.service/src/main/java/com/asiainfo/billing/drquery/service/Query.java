package com.asiainfo.billing.drquery.service;

import com.asiainfo.billing.drquery.exception.*;
import com.asiainfo.billing.drquery.process.ProcessException;
import com.asiainfo.billing.drquery.process.core.BaseProcess;
import com.asiainfo.billing.drquery.process.core.DRProcess;
import com.asiainfo.billing.drquery.process.core.ProcessFactory;
import com.asiainfo.billing.drquery.process.core.request.CommonDRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.BaseDTO;
import com.asiainfo.billing.drquery.process.dto.DRProcessDTO;
import com.asiainfo.billing.drquery.process.dto.model.ResMsg;
import com.asiainfo.billing.drquery.process.operation.MonitorLog;
import com.asiainfo.billing.drquery.utils.LogUtils;
import com.asiainfo.billing.drquery.utils.ServiceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;


public class Query {
	
	private static Log log = LogFactory.getLog(Query.class);
	private static IExceptionHandler handler = new ExceptionLogHandler();
	
	@SuppressWarnings("unchecked")
	private static ProcessFactory<CommonDRProcessRequest> processFactory = ServiceLocator.getInstance().getService("processFactory", ProcessFactory.class);
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BaseDTO query(CommonDRProcessRequest request){
		BaseDTO dto = new DRProcessDTO();
		boolean isSuccess = false;
		long t1 = System.currentTimeMillis();
        Map extendParams = new HashMap();
		try{
			DRProcess<CommonDRProcessRequest> process = resolveProcess(request.getProcessType());
            dto = process.pagingProcess(request,extendParams);
			isSuccess = true;
		}catch(BusinessException ex){
			handlerBusinessException(ex, dto);
		}catch(ProcessException ex){
			handlerSystemException(ex, dto);
		}catch(RuntimeException ex){
			handlerSystemException(ex, dto);
		}finally{
			long t2 = System.currentTimeMillis();
			extendParams.put(MonitorLog.TOTAL_COUNT, dto.getStatus() != null ? dto.getStatus().getTotalCount() : 0);
			extendParams.put(MonitorLog.INTERFACE_TYPE, request.getProcessType());
			extendParams.put(MonitorLog.ENTRY_POINT, "Query.query");
			MonitorLog mlog = new MonitorLog(request, isSuccess, t2 - t1, extendParams);
			LogUtils.getMonitorLogger().info(mlog);
		}
		return dto;
	}

    public static BaseDTO update(CommonDRProcessRequest request){
        BaseDTO dto = new DRProcessDTO();
        boolean isSuccess = false;
        long t1 = System.currentTimeMillis();
        Map extendParams = new HashMap();
        try{
            BaseProcess<CommonDRProcessRequest> process = resolveCommonProcess(request.getProcessType());
            dto = process.process(request, null, extendParams);
            isSuccess = true;
        }catch(BusinessException ex){
            handlerBusinessException(ex, dto);
        }catch(ProcessException ex){
            handlerSystemException(ex, dto);
        }catch(RuntimeException ex){
            handlerSystemException(ex, dto);
        }finally{
            long t2 = System.currentTimeMillis();
            extendParams.put(MonitorLog.TOTAL_COUNT, dto.getStatus() != null ? dto.getStatus().getTotalCount() : 0);
            extendParams.put(MonitorLog.INTERFACE_TYPE, request.getProcessType());
            extendParams.put(MonitorLog.ENTRY_POINT, "Query.query");
            MonitorLog mlog = new MonitorLog(request, isSuccess, t2 - t1, extendParams);
            LogUtils.getMonitorLogger().info(mlog);
        }
        return dto;
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

    private static <E extends BaseProcess<CommonDRProcessRequest>> E resolveCommonProcess(String processType) {
        BaseProcess<CommonDRProcessRequest> process = null;
        try {
            process = processFactory.getProcessByBusiType(processType);
            if (process == null)
                process = processFactory.getCommProcessByBusiType(processType);
        } catch (ProcessException e) {
            new ProcessException("unsupport billtype [" + processType + "]");
        }
        return (E) process;
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
