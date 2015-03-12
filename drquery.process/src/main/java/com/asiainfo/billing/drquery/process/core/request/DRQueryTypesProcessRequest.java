package com.asiainfo.billing.drquery.process.core.request;

public class DRQueryTypesProcessRequest {
	
	private String billId;//计费号码
	private String billMonth;//账期月
	private String fromDate;//开始日期YYYYMMDD
	private String thruDate;//结束日期YYYYMMDD
	private String dbType;
	public String getDbType() {
		return dbType;
	}
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	public String getBillId() {
		return billId;
	}
	public void setBillId(String billId) {
		this.billId = billId;
	}
	public String getBillMonth() {
		return billMonth;
	}
	public void setBillMonth(String billMonth) {
		this.billMonth = billMonth;
	}
	public String getFromDate() {
		return fromDate;
	}
	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}
	public String getThruDate() {
		return thruDate;
	}
	public void setThruDate(String thruDate) {
		this.thruDate = thruDate;
	}
	
}
