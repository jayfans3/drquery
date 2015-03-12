package com.asiainfo.billing.drquery.model;

import java.util.HashMap;
import java.util.Map;

public class MapTest {

	public static void main(String[] args){
		Map map1 = new HashMap();
		map1.put("username", "wk");
		
		Map map2 = new HashMap();
		map2.put("username", "leixue");
		
		Map map = new HashMap();
		map.putAll(map1);
		map.putAll(map2);
		
		System.out.println(map);
	}
}
