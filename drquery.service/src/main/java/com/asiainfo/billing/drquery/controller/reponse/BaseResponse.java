package com.asiainfo.billing.drquery.controller.reponse;

/**
 *
 * @author tianyi
 */
public class BaseResponse {
    public final static int SUCC = 0;
    public final static int FAIL = -1;
    protected int result;
    protected Object message;
    protected Object data;
    protected Object stat;
    protected Object sums;

	public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

	public Object getStat() {
		return stat;
	}

	public void setStat(Object stat) {
		this.stat = stat;
	}

    public Object getSums() {
        return sums;
    }

    public void setSums(Object sums) {
        this.sums = sums;
    }
}
