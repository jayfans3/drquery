package com.asiainfo.billing.drquery.process.dto.support;

import java.util.List;
import java.util.Map;

import com.asiainfo.billing.drquery.model.MetaModel;
import com.asiainfo.billing.drquery.process.core.request.DRProcessRequest;
import com.asiainfo.billing.drquery.process.dto.DRResultDTO;
import com.asiainfo.billing.drquery.process.dto.model.FieldDefListList;

public class DtoSupport {
	//private final static Log log = LogFactory.getLog(DtoSupport.class);
	
	/**
	 * @param modelList 	详单明细
	 * @param sumList 汇总信息
	 * @param stats
	 * @param meta
	 * @return
	 */
	public static DRResultDTO<List<String>> buildDto(
			List<Map<String, String>> modelList,
			List<Map<String,Object>> sumList,
			Map<String, Object> stats,
			MetaModel meta,
			DRProcessRequest request) {
		
		
//		//获取详单明细列表
//		FieldDefListList cdrDisplays = toCdrDisplays(modelList,meta); 
//		
//		//获取汇总信息  
//		SumTypeList sums = new SumTypeList();
//		if(sumList != null){
//			for(Map<String,Object> sum : sumList){
//				SumType sumType = new SumType();
//				sumType.setAccCode(Integer.valueOf(sum.get(SumType.ACC_CODE).toString()));
//				sumType.setAccName(sum.get(SumType.ACC_NAME)+"");
//	
//				String bigType = (String) sum.get(SumType.BIG_TYPE);
//				if("EMEND".equals(bigType)){
//					sumType.setSumType(1);
//				}else if("GSM".equals(bigType)){
//					sumType.setSumType(2);
//				}else if("SMS".equals(bigType)){
//					sumType.setSumType(3);
//				}else if("GPRS".equals(bigType)){
//					sumType.setSumType(4);
//				}else if("SP".equals(bigType)){
//					sumType.setSumType(6);
//				}else if("OWNSP".equals(bigType)){
//					sumType.setSumType(5);
//				}
//				sumType.setFee(sum.get(SumType.FEE)+"");
//				sumType.setType((Integer)sum.get(SumType.TYPE));
//				sumType.setSortId((Integer)sum.get(SumType.SORT_ID));
//				sums.add(sumType);
//			}
//		}
//		
//		DRResultDTO<List<String>> dto = new DRResultDTO<List<String>>();
//		dto.setReplyDisInfo(new ReplyDisInfo()); //话单内容
//		dto.getReplyDisInfo().setPhoneNum(request.getBillId());
//		
//		dto.getReplyDisInfo().setCdrDisplays(cdrDisplays);//详单明细列表
//		dto.getReplyDisInfo().setSums(sums); //汇总信息
//		
//		dto.setStatus(new Status());  //设置Stats;
//		if(stats.containsKey("startIndex")){
//			dto.getStatus().setStartIndex((Integer)stats.get("startIndex"));
//			stats.remove("startIndex");
//		}
//		if(stats.containsKey("stopIndex")){
//			dto.getStatus().setStopIndex((Integer)stats.get("stopIndex"));
//			stats.remove("stopIndex");
//		}
//		if(stats.containsKey("pageIndex")){
//			dto.getStatus().setPageIndex((Integer)stats.get("pageIndex"));
//			stats.remove("pageIndex");
//		}
//		if(stats.containsKey("pageSize")){
//			dto.getStatus().setPageSize((Integer)stats.get("pageSize"));
//			stats.remove("pageSize");
//		}
//		if(stats.containsKey("totalCount")){
//			dto.getStatus().setTotalCount((Integer)stats.get("totalCount"));
//			stats.remove("totalCount");
//		}
//		
//	    return dto;
		return null;
	}
	
	/**
	 * 转成详单明细列表
	 * @param modelList
	 * @param meta
	 * @return
	 */
	public static FieldDefListList toCdrDisplays(List<Map<String,String>> modelList,MetaModel meta){
//		if(modelList == null)
//			return null;
//		
//		/* 整理name与pos */
//		List<Field> arrfields = new ArrayList<Field>();
//		Map<String, Field> fields = meta.getFields();
//		for(Entry<String,Field> entry:fields.entrySet()){
//			arrfields.add(entry.getValue());
//		}
//		Collections.sort(arrfields);
//
//		
////		/* 整理value */
//		FieldDefListList fieldDefLists = new FieldDefListList(); 
//		for(Map<String,String> model : modelList){
//			FieldDefList fieldDefList = new FieldDefList();
//			for(int j=0;  j < arrfields.size(); j++ ){
//				FieldDef field = new FieldDef();
//				field.setName(arrfields.get(j).getName());
//				field.setPos(arrfields.get(j).getIndex().intValue());
//				String value = model.get(field.getName());
//				value = (value == null || value.equals("null")) ? "" : value;
//				field.setValue(value);							
//				fieldDefList.add(field);
//			}
//			fieldDefLists.add(fieldDefList);
//		}
//		return fieldDefLists;
		return null;
	}
}
