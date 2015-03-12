package com.asiainfo.billing.drquery.process.dto;

import com.asiainfo.billing.drquery.process.dto.model.ResMsg;
import com.asiainfo.billing.drquery.process.dto.model.Status;

import java.util.List;
import java.util.Map;

/**
 * 详单查询基础DTO
 * 
 * @author Rex Wong
 *
 * @version
 */
public class DRProcessDTO implements BaseDTO{
    private List<Map<String,String>> dRModels;
    private Map<String,Object> stats;
	public List<Map<String,String>> getdRModels() {
		return dRModels;
	}
	public void setdRModels(List<Map<String,String>> dRModels) {
		this.dRModels = dRModels;
	}

    public Map<String, Object> getStats() {
		return stats;
	}
	public void setStats(Map<String, Object> stats) {
		this.stats = stats;
	}

	private ResMsg resMsg;
	private Object replyDisInfo;	
	private List<Map<String,Object>> sumList;

	public ResMsg getResMsg() {
		return resMsg;
	}

	public void setResMsg(ResMsg resMsg) {
		this.resMsg = resMsg;
	}

	public Object getReplyDisInfo() {
		return replyDisInfo;
	}

	public void setReplyDisInfo(Object replyDisInfo) {
		this.replyDisInfo = replyDisInfo;
	}
	
	@Override
	public Status getStatus() {
		return null;
	}

    @Override
    public Object getSums() {
        return null;
    }

    @Override
    public void setSums(Object sums) {
    }

    public List<Map<String,Object>> getSumList() {
		return sumList;
	}
	public void setSumList(List<Map<String,Object>> sumList) {
		this.sumList = sumList;
	}
	
	
}
