/**
 * 
 */
package com.asiainfo.billing.drquery.axis2server;


/**
 * @author liujs3
 *
 */
public class Axis2ServerAdaptor extends CommonAxis {

	public String getAxis2Result(String interfaceType,String paramterJson){
		if(interfaceType.equals("F11")){
			return new Axis2Server().getDimensionSummary(paramterJson);
		}else if(interfaceType.equals("F12")){
			return new Axis2Server().getDetail(paramterJson);
		}else{
			return null;
		}
	}
}
