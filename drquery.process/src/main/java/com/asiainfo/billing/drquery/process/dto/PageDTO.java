package com.asiainfo.billing.drquery.process.dto;

import com.asiainfo.billing.drquery.process.dto.model.Status;

/**
 * 分页数据
 * @author tianyi
 * @param <T>
 */
public abstract class PageDTO<T> implements BaseDTO{

	//protected List<T> contents = Collections.emptyList();
	/*protected int startIndex;
	protected int stopIndex;
	protected int count;*/

	/*public List<T> getContents() {
		return contents;
	}

	public void setContents(List<T> contents) {
		this.contents = contents;
	}*/

	/*public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getStopIndex() {
		return stopIndex;
	}

	public void setStopIndex(int stopIndex) {
		this.stopIndex = stopIndex;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}*/
	
	private Status status;
	
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}	
	
}
