package com.asiainfo.billing.drquery.controller.reponse;

import java.util.HashMap;
import java.util.Map;

public class DataResponse extends BaseResponse {
	
	public DataResponse(Object object) {
		this.result = SUCC;
		this.message = null;
		this.data = object;
	}
	
	public DataResponse(Object object, Object message) {
		this.result = SUCC;
		this.message = message;
		this.data = object;
	}
	
	public DataResponse(Object object, Object message, Object stat) {
		this.result = SUCC;
		this.message = message;
		this.data = object;
		this.stat = stat;
	}

    public DataResponse(Object object, Object message, Object stat,Object sums) {
		this.result = SUCC;
		this.message = message;
		this.data = object;
		this.stat = stat;
        this.sums = sums;
	}

	public Map<String,Object> toMap(){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("result", this.result);
		map.put("resMsg", this.message);
		map.put("replyDisInfo", this.data);
        map.put("sums",this.sums);
		if(this.stat != null){
			map.put("stats", this.stat);
		}
		return map;
	}
        
}
