package com.al.crm.utils;


import com.al.common.exception.BaseException;
import net.sf.ezmorph.object.DateMorpher;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.DefaultValueProcessor;
import net.sf.json.util.JSONUtils;
import net.sf.json.util.PropertyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Json-lib的封装类，实现json字符串与json对象之间的转换
 * 网友johncon提供稍作修改
 * 
 * modify：
 * 	2009-8-31 zhaoxin :增加NUMBER_NULL_JSONCONF,处理Number的子类null会默认转为0的问题
 * @author zhaoxin 
 *
 */
public class JsonUtil {
	private static Logger log = LoggerFactory.getLogger(JsonUtil.class);
	
	/**
	 * 判断List是否为null或者空
	 * 
	 * @return
	 */
	public static boolean isListEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}
	/**
	 * 处理number及其子类的null转换
	 * 具体的使用例子:
	 * List<Order> orderList = new ArrayList<Order>();
	 * orderList.add(new Order());
	 * JSONObject json = new JSONObject();
	 * json.elementOpt("order", orderList,JsonUtil.NUMBER_NULL_JSONCONF);
	 * 通过以上的代码,order对象中的Integer属性如果有null的，就会自动转成json的null，不会默认为0
	 */
	public final static JsonConfig NUMBER_NULL_JSONCONF = createNumberNullValueJsonConfig();
	//处理number的null与日期类
	public final static JsonConfig DEFAULT_AND_DATTE_JSONCONF = createDefaultAndDateJsonConfig(false);
	//处理number的null与日期类，同时处理过滤null
	public final static JsonConfig FILTENULL_AND_DATTE_JSONCONF = createDefaultAndDateJsonConfig(true);
	
	/**
	 * 从一个JSON 对象字符格式中得到一个java对象，形如： {"id" : idValue, "name" : nameValue,
	 * "aBean" : {"aBeanId" : aBeanIdValue, ...}}
	 * 
	 * @param clazz
	 * @return
	 */
	public static Object getObject(String jsonString, Class clazz) {
		JSONObject jsonObject = null;
		try {
			setDataFormat2JAVA();
			jsonObject = JSONObject.fromObject(jsonString);
		} catch (Exception e) {
			log.error("转换object异常:", e);
		}
		return JSONObject.toBean(jsonObject, clazz);
	}

	/**
	 * 从一个JSON 对象字符格式中得到一个java对象，其中beansList是一类的集合，形如： {"id" : idValue, "name" :
	 * nameValue, "aBean" : {"aBeanId" : aBeanIdValue, ...}, beansList:[{}, {},
	 * ...]}
	 * 
	 * @param jsonString
	 * @param clazz
	 * @param map
	 *            集合属性的类型 (key : 集合属性名, value : 集合属性类型class) eg: ("beansList" :
	 *            Bean.class)
	 * @return
	 */
	public static Object getObject(String jsonString, Class clazz, Map map) {
		JSONObject jsonObject = null;
		try {
			setDataFormat2JAVA();
			jsonObject = JSONObject.fromObject(jsonString);
		} catch (Exception e) {
			log.error("json string convert to obj error:",e);
			return null;
		}
		return JSONObject.toBean(jsonObject, clazz, map);
	}

	/**
	 * 从一个JSON数组得到一个java对象数组，形如： [{"id" : idValue, "name" : nameValue}, {"id" :
	 * idValue, "name" : nameValue}, ...]
	 * 
	 * @param clazz
	 * @return
	 */
	public static Object[] getObjectArray(String jsonString, Class clazz) {
		setDataFormat2JAVA();
		JSONArray array = JSONArray.fromObject(jsonString);
		Object[] obj = new Object[array.size()];
		for (int i = 0; i < array.size(); i++) {
			JSONObject jsonObject = array.getJSONObject(i);
			obj[i] = JSONObject.toBean(jsonObject, clazz);
		}
		return obj;
	}

	/**
	 * &#x4ece;&#x4e00;&#x4e2a;JSON&#x6570;&#x7ec4;&#x5f97;&#x5230;&#x4e00;&#x4e2a;java&#x5bf9;&#x8c61;&#x6570;&#x7ec4;&#xff0c;&#x5f62;&#x5982;&#xff1a; [{"id" : idValue, "name" : nameValue}, {"id" :
	 * idValue, "name" : nameValue}, ...]
	 *
	 * @param clazz
	 * @param map
	 * @return
	 */
	public static Object[] getObjectArray(String jsonString, Class clazz, Map map) {
		setDataFormat2JAVA();
		JSONArray array = JSONArray.fromObject(jsonString);
		Object[] obj = new Object[array.size()];
		for (int i = 0; i < array.size(); i++) {
			JSONObject jsonObject = array.getJSONObject(i);
			obj[i] = JSONObject.toBean(jsonObject, clazz, map);
		}
		return obj;
	}

	/**
	 * 从一个JSON数组得到一个java对象集合
	 * 
	 * @param clazz
	 * @return
	 */
	public static List getObjectList(String jsonString, Class clazz) {
		setDataFormat2JAVA();
		JSONArray array = JSONArray.fromObject(jsonString);
		List list = new ArrayList();
		for (Iterator iter = array.iterator(); iter.hasNext();) {
			JSONObject jsonObject = (JSONObject) iter.next();
			list.add(JSONObject.toBean(jsonObject, clazz));
		}
		return list;
	}

	/**
	 * 从一个JSON数组得到一个java对象集合，其中对象中包含有集合属性
	 * 
	 * @param clazz
	 * @param map
	 *            集合属性的类型
	 * @return
	 */
	public static List getObjectList(String jsonString, Class clazz, Map map) {
		setDataFormat2JAVA();
		JSONArray array = JSONArray.fromObject(jsonString);
		List list = new ArrayList();
		for (Iterator iter = array.iterator(); iter.hasNext();) {
			JSONObject jsonObject = (JSONObject) iter.next();
			list.add(JSONObject.toBean(jsonObject, clazz, map));
		}
		return list;
	}

	/**
	 * 从json HASH表达式中获取一个map，该map支持嵌套功能 形如：{"id" : "johncon", "name" : "小强"}
	 * 
	 * @return
	 */
	public static Map getMap(String jsonString) {
		setDataFormat2JAVA();
		JSONObject jsonObject = JSONObject.fromObject(jsonString);
		Map map = new HashMap();
		for (Iterator iter = jsonObject.keys(); iter.hasNext();) {
			String key = (String) iter.next();
			map.put(key, jsonObject.get(key));
		}
		return map;
	}

	/**
	 * 从json数组中得到相应java数组 json形如：["123", "456"]
	 * 
	 * @param jsonString
	 * @return
	 */
	public static Object[] getObjectArray(String jsonString) {
		JSONArray jsonArray = JSONArray.fromObject(jsonString);
		return jsonArray.toArray();
	}

	private static void setDataFormat2JAVA() {
		// 设定日期转换格式
		JSONUtils.getMorpherRegistry().registerMorpher(
				new DateMorpher(new String[] { "yyyy-MM-dd HH:mm:ss",
						"yyyy-MM-dd" }));
	}

	/**
	 * 把对象转换为json字符串 日期类型为默认的: YYYY-MM-DD HH:MM:SS
	 * 
	 * @param obj
	 * @return
	 */
	public static String getJsonString(Object obj) {
		if (obj == null)
			return "{}";
		return getJsonString(obj, DEFAULT_AND_DATTE_JSONCONF);
	}
	/**
	 * 取json字符串
	 * @param obj
	 * @param cfg
	 * @return
	 */
	public static String getJsonString(Object obj, JsonConfig cfg) {
		if (obj != null) {

			if (isArray(obj)) {
				JSONArray jsonArray = JSONArray.fromObject(obj, cfg);
				return jsonArray.toString();
			} else {

				JSONObject jsonObject = JSONObject.fromObject(obj, cfg);
				return jsonObject.toString();
			}
		}
		return "{}";
	}
	/**
	 * 对象是否是数组
	 * @param obj
	 * @return
	 */
	private static boolean isArray(Object obj) {
		return obj instanceof Collection || obj.getClass().isArray();
	}
	/**
	 * 处理number及其子类的null转换
	 * @return
	 */
	public static JsonConfig createNumberNullValueJsonConfig(){
		JsonConfig conf = new JsonConfig();
		registerDefaultNullValueProcessor(conf);
		return conf;

	}
	/**
	 * 处理全部
	 * @return
	 */
	public static JsonConfig createDefaultAndDateJsonConfig(boolean ifFilteNull){
		JsonConfig conf = new JsonConfig();
		if(!ifFilteNull) registerDefaultNullValueProcessor(conf);
//		registerDateValueProcessor(conf);
		if(ifFilteNull) registerNullFilter(conf);
		return conf;
	}
	
	/**
	 * 注册空过滤器
	 * */
	public static void registerNullFilter(JsonConfig conf){
		PropertyFilter filter = new PropertyFilter() {
    	public boolean apply(Object object, String fieldName, Object fieldValue) {
	    		//仅过滤NULL的属性值
	    		return null == fieldValue/*||"".equals(fieldValue.toString())*/;
    		}
    	}; 
    	conf.setJsonPropertyFilter(filter);
	}
	
	/**
	 * 处理日期类转换
	 * @param conf
	 */
//	public static void registerDateValueProcessor(JsonConfig conf){
//		conf.registerJsonValueProcessor(Date.class,
//				new JsonDateValueProcessor());
//	}
	/**
	 * 注册json的处理类
	 * @param conf
	 */
	public static void registerDefaultNullValueProcessor(JsonConfig conf){
		conf.registerDefaultValueProcessor(Number.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(AtomicInteger.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(BigDecimal.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(BigInteger.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Byte.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Double.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Float.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Integer.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Long.class,new DefaultNullValueProcessor());
		conf.registerDefaultValueProcessor(Short.class,new DefaultNullValueProcessor());		
	}
	/**
	 * null空值特殊处理类
	 * @author zhaoxin
	 *
	 */
	public static class DefaultNullValueProcessor implements DefaultValueProcessor {
		  public Object getDefaultValue(Class type){
	            return JSONNull.getInstance();
	        }
	}
	
	/**
	 * 判断入参的json数据中是否包含key，如果不存在则报错
	 * @param json
	 * @param key
	 * @param errorMsg 如果不存在key异常中的提示信息
	 * */

	public static void isContainsParam(JSONObject json, String key, String errorMsg) {
		if (!json.containsKey(key)) {
			throw new BaseException(errorMsg+"JSON数据中" + key + "为空");
		}
	}
	
	/**
	 * 从JSONObject获取Long值
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Long getLongFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Long型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			return null;
		}
		try{
			return jsonObj.getLong(key);
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Long型[").append(key).append("]失败，原因是：").append(
			                e.getMessage()).append("从JSON：").append(jsonObj).toString());
		}
	}

	/**
	 * 从JSONObject获取Integer值
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Integer getIntFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Integer型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "".equals(jsonObj.getString(key))
		        || "null".equals(jsonObj.getString(key))){
			return null;
		}
		try{
			return jsonObj.getInt(key);
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append(",获取Integer型[").append(key).append(
			                "]失败，原因是：").append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
		}
	}

	/**
	 * 从JSONObject获取String值
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static String getStringFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取String型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key)){
			return null;
		}
		try{
			String s = jsonObj.getString(key);
			if("null".equals(s) || "".equals(s)){
				s = null;
			}
			return s;
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append(",获取String型[")
			        .append(key).append("]失败，原因是：").append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
		}
	}

	/**
	 * 从JSONObject获取Date值
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Date getDateFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Date型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key)){
			return null;
		}
		try{
			String s = jsonObj.getString(key);
			if("null".equals(s) || "".equals(s)){
				return null;
			}else{
				try{
					return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
				}catch(java.text.ParseException e){
					return new SimpleDateFormat("yyyy-MM-dd").parse(s);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new BaseException(new StringBuffer().append(errorMsg).append(",获取Date型[").append(key).append("]失败，原因是：").append(
			                e.getMessage()).append("从JSON：").append(jsonObj).toString());
		}
	}

	/**
	 * 从JSONObject获取Long值，null也抛异常
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Long getNotNullLongFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Long型的[").append(key).append("]失败，原因是：不存在该属性JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空Long型的[").append(key).append("]失败fromJSON:").append(jsonObj).toString());

		}else{
			try{
				return jsonObj.getLong(key);
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Long型的[").append(key).append("]失败，原因是：")
				        .append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
			}
		}
	}

	public static Long getDataFromJSON(JSONObject jsonObj, String key, String errorMsg){

		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			return null;
		}else{
			try{
				return jsonObj.getLong(key);
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append(",获取Data型的[").append(key).append("]失败，原因是：")
				        .append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
			}
		}
	}

	/**
	 * 从JSONObject获取Integer值，null也抛异常
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Integer getNotNullIntFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Integer型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空int型的[").append(key).append("]失败fromJSON:").append(jsonObj).toString());
		}else{
			try{
				return jsonObj.getInt(key);
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Integer型的[").append(key).append(
				        "]失败，原因是：").append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
			}
		}
	}

	/**
	 * 从JSONObject获取String值，null也抛异常
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static String getNotNullStringFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取String型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空String型的[").append(key).append("]失败fromJSON:").append(jsonObj).toString());
		}
		try{
			return jsonObj.getString(key);
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取String型的[")
			        .append(key).append("]失败，原因是：").append(e.getMessage())
			        .append("从JSON：").append(jsonObj).toString());
		}
	}

	/**
	 * 从JSONObject获取Date值
	 * 
	 * @param jsonObj
	 * @param key
	 */
	public static Date getNotNullDateFromJSON(JSONObject jsonObj, String key, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).append("从JSON：").append(jsonObj).append(
		        "中获取Date型的[").append(key).append("]失败，原因是：JSON本身为Null").toString());
		if(!jsonObj.containsKey(key) || "".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空日期[").append(key).append("]失败fromJSON:").append(jsonObj).toString());
		}
		try{
			String s = jsonObj.getString(key);
			try{
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
			}catch(java.text.ParseException e){
				return new SimpleDateFormat("yyyy-MM-dd").parse(s);
			}
		}catch(Exception e){
			throw new BaseException(new StringBuffer(errorMsg).append("，获取Date型的[").append(key).append("]失败，原因是：")
			        .append(e.getMessage()).append("从JSON：").append(jsonObj).toString());
		}
	}

	public static void checkJSONObjectNullErrorThrow(JSONObject jsonObj, String errorMsg){
		if(jsonObj == null){
			throw new BaseException(errorMsg);
		}
	}

	public static JSONObject getOrCreateJSONObject(JSONObject jsonObj, String key){
		if(!jsonObj.containsKey(key)){
			jsonObj.element(key,new JSONObject());
			return jsonObj.getJSONObject(key);
		}else{
			return jsonObj.getJSONObject(key);
		}
	}

	public static JSONArray getOrCreateJSONArray(JSONObject jsonObj, String key){
		if(!jsonObj.containsKey(key)){
			jsonObj.element(key,new JSONArray());
			return jsonObj.getJSONArray(key);
		}else{
			return jsonObj.getJSONArray(key);
		}
	}

	/**
	 * 移除json的key在这个set中的子节点
	 * 
	 * @param json
	 * @param keys
	 */
	public static void removeJSONKeysInTheSet(JSONObject json, Set<String> keys){
		for(Iterator<String> i = keys.iterator();i.hasNext();){
			String key = i.next();
			json.remove(key);
		}
	}

	/**
	 * 移除json的key不在这个set中的子节点
	 * 
	 * @param json
	 * @param keys
	 */
	public static void removeJSONKeysOutTheSet(JSONObject json, Set<String> keys){
		Set<String> removeSet = new HashSet<String>();
		for(Iterator<String> k = json.keys();k.hasNext();){
			String key = k.next();
			if(!keys.contains(key)){
				removeSet.add(key);
			}
		}
		for(Iterator<String> k = removeSet.iterator();k.hasNext();){
			String key = k.next();
			json.remove(key);
		}
	}

	/**
	 * <pre>
	 * 根据名字从jsonObject中获得对应的属性值,
	 * 若果不存在对应的key，抛出SMO运行时异常
	 *  主要用来处理页面的必输入项
	 * </pre>
	 * 
	 * @param obj
	 * @param name
	 * @return
	 */
	public static Long getNotNullLongByKey(JSONObject obj, String name){
		if(!obj.has(name)){
			throw new BaseException("该json对象中没有该键值/属性(" + name + ")");
		}
		return Long.valueOf(obj.getLong(name));
	}

	/**
	 * 根据名字从jsonObject中获得对应的属性值,若果不存在对应的key，返回 null
	 *
	 * @return int
	 */
	public static Integer getIntByKey(JSONObject jsonObj, String strKey){
		if(jsonObj.containsKey(strKey)){
			Object obj = jsonObj.get(strKey);
			if(obj == null){
				return null;
			}else if(JSONNull.getInstance().equals(obj)){
				return null;
			}else if(jsonObj.getString(strKey).toString().equals("")){
				return null;
			}else{
				return Integer.valueOf(jsonObj.getString(strKey));
			}
		}else{
			return null;
		}
	}
	
	/**
	 * 从Map 中获取String值
	 * @param map
	 * @param key
	 * @param errorMsg
	 * @return
	 */
	public static String getStringFromMap(Map map, String key, String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从MAP：").append(map).append(
        "中获取String型的[").append(key).append("]失败，原因是：Map本身为Null").toString());
        if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key)==null){
	          return null;
		}
		try{
			return (map.get(key).toString());
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取String型[")
					.append(key).append("]失败，原因是：").append(e.getMessage()).append("从MAP：").append(map).toString());
		}
	}
	
	/**
	 * 从map中取非空的String值
	 * @param map
	 * @param key
	 * @param errorMsg
	 * @return
	 */
	public static String getNotNullStringFromMap(Map map,String key,String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从Map：").append(map).append(
		        "中获取String型的[").append(key).append("]失败，原因是：不存在该属性MAP本身为Null").toString());
		if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key)==null){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空String的[").append(key).append("]失败fromMap:").append(map).toString());
		}else{
			try{
				return map.get(key).toString();
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append(",获取String型的[").append(key).append("]失败，原因是：")
				        .append(e.getMessage()).append("从MAP：").append(map).toString());
			}
		}
	}
	
	/**
	 * 从map获取Long值，null也抛异常
	 * @param map
	 * @param key
	 * @param errorMsg
	 */
	public static Long getNotNullLongFromMap(Map map,String key,String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从MAP：").append(map).append(
		        "中获取Long型的[").append(key).append("]失败，原因是：不存在该属性MAP本身为Null").toString());
		if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key)==null){
			throw new BaseException(new StringBuffer().append(errorMsg).append(",获取非空Long型的[").append(key).append("]失败fromMap:").append(map).toString());
		}else{
			try{
				return Long.valueOf(map.get(key).toString());
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Long型的[").append(key).append("]失败，原因是：")
				        .append(e.getMessage()).append("从MAP：").append(map).toString());
			}
		}
	}
	
	/**
	 * 从MAP获取Long值
	 * @param map
	 * @param key
	 * @param errorMsg
	 */
	public static Long getLongFromMap(Map map,String key,String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从MAP：").append(map).append(
		        "中获取Long型的[").append(key).append("]失败，原因是：MAP本身为Null").toString());
		if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key) == null){
			return null;
		}
		try{
			return Long.valueOf(map.get(key).toString());
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Long型[")
					.append(key).append("]失败，原因是：").append(e.getMessage()).append("从MAP：").append(map).toString());
		}
	}
	
	/**
	 * 从map获取Integer值
	 * 
	 * @param key
	 */
	public static Integer getIntFromMap(Map map,String key,String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从MAP：").append(map).append(
		        "中获取Integer型的[").append(key).append("]失败，原因是：MAP本身为Null").toString());
		if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key) == null){
			return null;
		}
		try{
			return Integer.valueOf(map.get(key).toString());
		}catch(Exception e){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Integer型[")
					.append(key).append("]失败，原因是：").append(e.getMessage()).append("从MAP：").append(map).toString());
		}
	}
	
	/**
	 * 从JSONObject获取Integer值，null也抛异常
	 * 
	 * @param key
	 */
	public static Integer getNotNullIntFromMap(Map map,String key,String errorMsg){
		checkMapNullErrorThrow(map,new StringBuffer(errorMsg).append("从MAP：").append(map).append(
		        "中获取Integer型的[").append(key).append("]失败，原因是：MAP本身为Null").toString());
		if(!map.containsKey(key) || "".equals(map.get(key)) || "null".equals(map.get(key)) || map.get(key) == null){
			throw new BaseException(new StringBuffer().append(errorMsg).append("，获取非空Int型的[").append(key).append("]失败fromMap:").append(map).toString());

		}else{
			try{
				return Integer.valueOf(map.get(key).toString());
			}catch(Exception e){
				throw new BaseException(new StringBuffer().append(errorMsg).append("，获取Integer型的[").append(key).append(
				        "]失败，原因是：").append(e.getMessage()).append("从MAP：").append(map).toString());
			}
		}
	}
	/**
	 * 校验Map是否为空
	 * @param map
	 * @param errorMsg
	 */
	public static void checkMapNullErrorThrow(Map map, String errorMsg){
		if(map == null){
			throw new BaseException("Map 转 JavaBean时，非空节点是NULL:" + errorMsg);
		}
	}
	
	/**
	 * 判断指定key值的json对象是否为空
	 * @param key
	 * @param jsonObj
	 * @param errorMsg
	 * @return
	 */
	public static Boolean isJsonObjNull(String key, JSONObject jsonObj, String errorMsg){
		checkJSONObjectNullErrorThrow(jsonObj,new StringBuffer(errorMsg).toString());
		if(!jsonObj.containsKey(key)){
			return false;
		}else if("".equals(jsonObj.getString(key)) || "null".equals(jsonObj.getString(key))){
			return true;		
		}else{
			return false;
		}
	}
}
