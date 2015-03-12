package com.asiainfo.billing.drquery.process.operation.fieldEscape;import java.lang.*;import java.lang.System;import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.List;import java.util.Map;import org.apache.commons.beanutils.MethodUtils;import org.apache.commons.lang3.StringUtils;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import com.asiainfo.billing.drquery.process.operation.fieldEscape.model.FieldEscapeModel;import com.asiainfo.billing.drquery.utils.DateUtil;import com.asiainfo.billing.drquery.cache.CacheProvider;import com.asiainfo.billing.drquery.utils.NumberUtils;import com.asiainfo.billing.drquery.process.core.request.DRProcessRequest;import com.asiainfo.billing.drquery.Constants;/** * 转义规则 */public class LocalFieldEscape{	         /*************************************${template.GPRS}*************************************************/    public String DEST_GPRS_K1(Map<String, String> model, String fieldName, FieldEscapeModel field, DRProcessRequest request) {        String DEST_COLUMN = "";        String SRC_COLUMN = model.get(fieldName);        if(fieldName.equals("BUSI_ID")){            DEST_COLUMN = CacheProvider.getBUSI_NAMEByBUSI_ID("BUSI_NAME",SRC_COLUMN);        }else if(fieldName.equals("APP_ID")){            DEST_COLUMN = CacheProvider.getAPP_NAMEByAPP_ID("APP_NAME",SRC_COLUMN);        }else if(fieldName.equals("BUSI_TYPE_ID")){            DEST_COLUMN = CacheProvider.getBUSI_TYPE_NAMEByBUSI_TYPE_ID("BUSI_TYPE_NAME",SRC_COLUMN);        }else if(fieldName.equals("TERM_MODEL_ID")){            DEST_COLUMN = CacheProvider.getTERM_MODEL_NAMEByTERM_MODEL_ID("TERM_MODEL_NAME",SRC_COLUMN);        }        //System.out.println("DEST_COLUMN="+DEST_COLUMN);        if(StringUtils.isEmpty(DEST_COLUMN) || DEST_COLUMN.equals("0") || DEST_COLUMN.equals("未识别")){           DEST_COLUMN = "其它";        }        return DEST_COLUMN;    }}