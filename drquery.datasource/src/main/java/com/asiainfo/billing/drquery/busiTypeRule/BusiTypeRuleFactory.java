package com.asiainfo.billing.drquery.busiTypeRule;

import com.ailk.oci.ocnosql.client.query.criterion.Criterion;

import java.util.Map;




public class BusiTypeRuleFactory {
	
	public static BusiTypeRule createBusiRule(String modleId, Map<String, Object> requestParam){
		BusiTypeRule busiRule = null;
		if("GPRS".equals(modleId)){
			//busiRule = ...
		}else if("TSSY_D".equals(modleId)){
			
		}else if("TSSY_H".equals(modleId)){
            if(requestParam == null || requestParam.isEmpty())return null;
			String interfaceType = (String)requestParam.get("interfaceType");
            if("F15".equals(interfaceType)){
               final String appCodeParam = (String)requestParam.get("appCode");
               busiRule = new BusiTypeRule(){
                   @Override
                   public Criterion getQueryParam() {
                       Criterion criterion = new Criterion();
                       //criterion.setEqualsTo("APP_ID",appCodeParam);
                       return criterion;
                   }
               };
            }
		}else{
			//if not found, throw new RuntimeException
		}
		
		return busiRule;
	}
}
