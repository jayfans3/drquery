package com.asiainfo.billing.drquery.model;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.WeakHashMap;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.asiainfo.billing.drquery.ModelRuntimeException;
import com.asiainfo.billing.drquery.utils.PropertiesUtil;

/**
 * 视图层，返回给外围系统的字段
 */
public class ViewModelReader {

	private final static Log log = LogFactory.getLog(ViewModelReader.class);
	private static SAXReader reader = new SAXReader();
	private static Map<String,MetaModel> metaModels;
	private static Map<String,String> allBusiType = new WeakHashMap<String,String>();
	
	public static Map<String, MetaModel> getMetaModels() {
		return metaModels;
	}
	
   	public static Map<String,String> getAllBusiType() {
		return allBusiType;
	}
   	
	public void loadModel(String path) throws Exception{
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();  
	    Resource[] resources=resolver.getResources(path);  
		if (resources.length!=0) {
			if(log.isInfoEnabled()){
				log.info("model path:"+resources[0].getURL().getPath().replaceAll(resources[0].getFilename(), ""));
			}
			metaModels = new WeakHashMap<String,MetaModel>();
			for(Resource resource:resources){
				try{
					MetaModel metaModel = readFile2MetaModel(resource);
					if(metaModel!=null){
						metaModels.put(metaModel.getModelId(), metaModel);
					}
				}
				catch(Exception e){
					if (log.isErrorEnabled()) {
						log.error(e);
					}
				}
				
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private MetaModel readFile2MetaModel(Resource resource) throws Exception{
		InputStream inputStream = resource.getInputStream();
		if (log.isInfoEnabled()) {
			log.info("mappring model>>>" + resource.getFilename());
		}
		Document document = null;
		try {
			document = reader.read(inputStream);
		} 
		catch (DocumentException e) {
			if (log.isErrorEnabled()) {
				log.error("loader meta model exception: read model["+resource.getFilename()+"] file failed");
			}
			throw new ModelRuntimeException("loader meta model exception: read model["+resource.getFilename()+"] file failed",e);
		}
		Element root = document.getRootElement();
		MetaModel metaModel = new MetaModel();
		Attribute modelId = root.attribute("modelId");
		if(modelId!=null){
			metaModel.setModelId(modelId.getValue());
		}
		Attribute table = root.attribute("table");
		if(table!=null){
			metaModel.setTable(table.getValue());
		}
		Attribute name = root.attribute("name");
		if(name!=null){
			metaModel.setName(name.getValue());
		}
		Attribute process = root.attribute("process");
		if(process!=null){
			metaModel.setProcess(process.getValue());
		}
		else{
			throw new ModelRuntimeException("The model xml not set process field. process value cant be null!");
		}
		Attribute statType = root.attribute("statType");
		if(statType!=null){
			metaModel.setStatType(statType.getValue());
		}
		Attribute statRule = root.attribute("statRule");
		if(statRule!=null){
			metaModel.setStatRule(statRule.getValue());
		}
        Attribute useCache = root.attribute("useCache");
        if(useCache != null && "true".equals(useCache.getValue())){
            metaModel.setUseCache(true);
        }

		allBusiType.put(metaModel.getModelId(), metaModel.getName());
		
		List<Element> nodeList = root.elements();
		if (!CollectionUtils.isEmpty(nodeList)) {
			Map<String,Field> fields = new HashMap<String,Field>();
            Map<String,String> sqls = new HashMap<String, String>();
            Map<String,String> backNameToDBName = new HashMap<String,String>();
            Map<String,String> dbNameToBackName  = new HashMap<String,String>();
			for (Element element : nodeList) {
                String elementName=element.getName();
                if(elementName.equalsIgnoreCase("field")){
                    List<Attribute> attrs = element.attributes();
                    Field field = new Field();
                    String dbName = "";
                    String backName = "";
                    for(Attribute attr:attrs){
                        if(log.isDebugEnabled()){
                            log.debug("set field["+attr.getName()+"-"+attr.getValue()+"] to "+modelId.getValue());
                        }
                        String value = attr.getValue();
                        String fieldName = attr.getName();
                        if(StringUtils.contains(value, " ")){
                            log.error("model["+metaModel.getModelId()+"] field["+fieldName+"]'s value["+value+"] contain blank space");
                            value = StringUtils.trim(value);
                        }
                        if("name".equalsIgnoreCase(attr.getName())){
                            dbName = attr.getValue();
                        }
                        if("backName".equalsIgnoreCase(attr.getName())){
                            backName = attr.getValue();
                        }
                        BeanUtils.setProperty(field, attr.getName(), attr.getValue());
                    }
                    log.info("dbname=" + dbName + " backname=" + backName);
                    backNameToDBName.put(backName,dbName);
                    dbNameToBackName.put(dbName,backName);
                    fields.put(field.getName(), field);
                }else if(elementName.equalsIgnoreCase("sql")){
                    List<Attribute> attrs = element.attributes();
                    for(Attribute attr:attrs){
                        String fieldName = attr.getName();
                        String value = attr.getValue();
                        log.info("model["+metaModel.getModelId()+"] sqlName["+fieldName+"], sqlValue["+value+"]");
                        sqls.put(fieldName,value);
                    }
                }
			}
            metaModel.setBackNameToDBName(backNameToDBName);
            metaModel.setDbNameToBackName(dbNameToBackName);
            metaModel.setSql(sqls);
			metaModel.setFields(fields);
			//return metaModel;
		}
		if(log.isWarnEnabled()){
			log.warn("the model["+resource.getFilename()+"] is null");
		}
	    return metaModel;
	}
	
	
	public void afterPropertiesSet() throws Exception {
		loadModel("classpath:drquery.model/viewMapping/*.xml");
	}
	
	
	public static void main(String[] args){
		ModelReader reader = new ModelReader();
		try {
			reader.loadModel("classpath:drquery.model/busiMapping/*.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Properties prop = PropertiesUtil.getProperties("gbase_sql.properties");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("gbase_sql_after.properties");
			Map<String, MetaModel> models = ModelReader.getMetaModels();
			Properties propinput = new Properties();// 属性集合对象   
			for(Entry<String,MetaModel> entry:models.entrySet()){
				Map<String, Field> fields = entry.getValue().getFields();
				String sql = "SELECT ";
				for(Entry<String,Field> fieldEntry:fields.entrySet()){
					sql+=fieldEntry.getValue().getName()+",";
				}
				sql=sql.substring(0, sql.length()-1)+" FROM";
				String key = entry.getValue().getModelId();
				String value = (String) prop.get(key);
				sql = sql+" "+value;
		        // 将Properties集合保存到流中   
				propinput.setProperty(key, sql);
			}
			propinput.store(fos, "Copyright (c) Rexwong");   
			fos.close();// 关闭流   
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}
