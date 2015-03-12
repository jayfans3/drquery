package com.asiainfo.billing.drquery.datasource.query;

import java.util.Map;

/**
 * 详单查询参数
 * 
 * @author Rex Wong
 *
 * @version
 */
public class DRQueryParameters{
	
    private String billId;
    private String month;
    private String billType;
    private String from;
    private String thru;
    private String destId;
    private boolean commonQuery;
    private boolean isFilter;//是否根据业务进行过滤
    private Map<String, Object> requestParam;
    
	public String getBillId() {
		return billId;
	}
	public void setBillId(String billId) {
		this.billId = billId;
	}
	public String getMonth() {
		return month;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	public String getBillType() {
		return billType;
	}
	public void setBillType(String billType) {
		this.billType = billType;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getThru() {
		return thru;
	}
	public void setThru(String thru) {
		this.thru = thru;
	}
	public String getDestId() {
		return destId;
	}
	public void setDestId(String destId) {
		this.destId = destId;
	}
	public boolean isCommonQuery() {
		return commonQuery;
	}
	public void setCommonQuery(boolean commonQuery) {
		this.commonQuery = commonQuery;
	}
	public boolean isFilter() {
		return isFilter;
	}
	public void setFilter(boolean isFilter) {
		this.isFilter = isFilter;
	}
	public Map<String, Object> getRequestParam() {
		return requestParam;
	}
	public void setRequestParam(Map<String, Object> requestParam) {
		this.requestParam = requestParam;
	}
	
}
