package com.asiainfo.billing.drquery.process.dto;

import com.asiainfo.billing.drquery.process.dto.model.ResMsg;

import java.util.*;

/**
 * 详单类型
 * 
 * @author tianyi
 */
public class DRResultDTO<T> extends PageDTO<T> {	
	
	   private ResMsg resMsg;
	   private Object replyDisInfo;
       private Object sums;



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

        public Object getSums() {
            return sums;
        }

        public void setSums(Object sums) {
            this.sums = sums;
        }
}
