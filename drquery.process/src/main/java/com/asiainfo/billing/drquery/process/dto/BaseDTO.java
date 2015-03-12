package com.asiainfo.billing.drquery.process.dto;

import com.asiainfo.billing.drquery.process.dto.model.ResMsg;
import com.asiainfo.billing.drquery.process.dto.model.Status;



/**
 *
 * @author tianyi
 */
public interface BaseDTO{
	public void setResMsg(ResMsg resMsg);
	public ResMsg getResMsg() ;
	public Object getReplyDisInfo();
	public void setReplyDisInfo(Object replyDisInfo) ;
	public Status getStatus();
    public Object getSums();
    public void setSums(Object sums);
}
