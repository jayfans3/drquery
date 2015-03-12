package com.asiainfo.billing.drquery.busiTypeRule;

import com.ailk.oci.ocnosql.client.query.criterion.Criterion;

public class MutilTypeRule implements BusiTypeRule{
	
	private Criterion queryParam;

	@Override
	public Criterion getQueryParam() {
		// TODO Auto-generated method stub
		return this.queryParam;
	}
	
	public void setQueryParam(Criterion queryParam){
		this.queryParam = queryParam;
	}

}
