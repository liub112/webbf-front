package com.al.crm.utils;

import com.al.common.exception.BaseException;
import com.al.common.utils.StringUtil;
import com.al.persistence.PagingInfo;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/** 
 * 订单调度的公共工具类. 
 * @version 0.5
 * @author 景伯文 
 * @author 魏铁胜 
 */

public class CommonUtil {


	private static Logger LOG = LoggerFactory.getLogger(CommonUtil.class);
	//日期类型的属性
	private static final int DATE_TYPE_ITEM = 4;
	//时间类型的属性
	private static final int TIME_TYPE_ITEM = 16;
    public  static ThreadLocal custOrderType=new ThreadLocal<String>();
	/**
	 * 将字CLOB转成STRING类型
	 * @param clob CLOB的数据对象
	 * @return CLOB读取转化后的字符串对象
	 * @throws SQLException
	 * @throws IOException
	 */
	public static String clobToString(oracle.sql.CLOB clob) throws SQLException, IOException {
		String reString = "";
		// 得到流
		Reader is = clob.getCharacterStream();
		BufferedReader br = new BufferedReader(is);
		String s = br.readLine();
		StringBuffer sb = new StringBuffer();
		//执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
		while (s != null) {
			sb.append(s);
			s = br.readLine();
		}
		reString = sb.toString();
		return reString;
	}

	/**
	 * 获取指定长度(按字节长度获取)的字符串
	 * @param src 源字符串
	 * @param length 长度
	 * @return
	 */
	public static String getSubStr(String src, int length) {
		if (StringUtil.isEmpty(src)) {
			return null;
		}
		if (src.getBytes().length > length) {
			byte[] b = src.getBytes();
			byte[] s = new byte[length];
			for (int i = 0; i < length; i++) {
				s[i] = b[i];
			}
			return new String(s);
		} else {
			return src;
		}
	}

	/**
	 * 获取异常信息内容
	 * @param e 异常对象
	 * @param length 指定长度
	 * @return 返回异常信息内容
	 */
	public static String getExceptionString(Exception e, int length) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		String msg = os.toString();
		if (msg.length() > length) {
			msg = getSubStr(msg, length);
		}
		return msg;
	}

	/**
	 * 获取异常信息内容
	 * @param e 异常对象
	 * @return 返回异常信息内容
	 */
	public static String getExceptionString(Exception e) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		String msg = os.toString();
		return msg;
	}



	public static String formatDate(Date date, String formatStr) {
		SimpleDateFormat dateformat = new SimpleDateFormat(formatStr);
		String dateStr = dateformat.format(date);
		return dateStr;
	}

	/**
	 * 用于处理前台通过GET提交中文乱码的问题，做一个编码转换
	 * @param originalString  源字符串
	 * @return 编码转换后的字符串
	 */
	public static String changeEncode(String originalString) {
		try {
			return new String(originalString.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 复制List对象
	 * @param list
	 * @return
	 */
	public static List copyList(List list) {
		try {
			List listBak = new ArrayList();
			for (Object obj : list) {
				listBak.add(BeanUtils.cloneBean(obj));
			}
			return listBak;
		} catch (Exception e) {
			LOG.error("对象拷贝异常:" + e.getMessage());
			throw new BaseException("数据拷贝异常:" + e.getMessage());
		}
	}

	/**
	 * 拷贝2个对象的一般属性，不包括集合类属性,目标对象的集合类被初始化为空集合
	 * add by lsw
	 * @param newObj
	 * @param srcObj
	 * @throws IllegalAccessException, InvocationTargetException, NoSuchMethodException 
	 * 
	 */
	public static void copyProperties(Object newObj, Object srcObj) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {

		if (newObj == null || srcObj == null) {
			return;
		}
		Field[] fields = srcObj.getClass().getDeclaredFields();
		String name = null;
		for (int i = 0; i < fields.length; i++) {
			name = fields[i].getName();
			//过滤掉集合属性

			if (!(fields[i].getType().isAssignableFrom(Set.class) || fields[i].getType().isAssignableFrom(
					Collection.class))) {

				PropertyUtils.setSimpleProperty(newObj, name, PropertyUtils.getSimpleProperty(srcObj, name));

			} else {
				PropertyUtils.setSimpleProperty(newObj, name, new HashSet());
			}

		}

	}

	/**
	 * 深度拷贝2个对象,包扩关联对象
	 * add by lsw
	 * @param srcObj
	 * @throws IOException, ClassNotFoundException
	 * 
	 */
//	public static Object copyObject(Object srcObj) throws IOException, ClassNotFoundException {
//		//利用对象序列化技术
//		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//		ObjectOutputStream out = new ObjectOutputStream(byteOut);
//		out.writeObject(srcObj);
//		ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
//		ObjectInputStream in = new ObjectInputStream(byteIn);
//		return in.readObject();
//
//	}

	/**
	 * 深度拷贝对象,包扩关联对象,返回拷贝数组
	 * add by lsw
	 * @param srcObj
	 * @param count 拷贝个数
	 * @throws IOException, ClassNotFoundException
	 * 
	 */
//	public static Object[] copyObject(Object srcObj, int count) throws IOException, ClassNotFoundException {
//		//利用对象序列化技术
//		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//		ObjectOutputStream out = new ObjectOutputStream(byteOut);
//		out.writeObject(srcObj);
//		ByteArrayInputStream byteIn = null;
//		ObjectInputStream in = null;
//		Object[] retObj = new Object[count];
//		for (int i = 0; i < count; i++) {
//			byteIn = new ByteArrayInputStream(byteOut.toByteArray());
//			in = new ObjectInputStream(byteIn);
//			retObj[i] = in.readObject();
//		}
//		return retObj;
//	}

	/**
	 * 判断字符串中是否只包含数字
	 * @param 字符串
	 * @return true-只包含数字,false-其他任何情况
	 */
	public static boolean isContainsNumberOnly(String str) {
		boolean result = true;
		if (!StringUtils.isEmpty(str)) {
			for (int i = 0; i < str.length(); i++) {
				if (!Character.isDigit(str.charAt(i))) {
					result = false;
				}
			}
		} else {
			result = false;
		}
		return result;
	}
	
	/** 
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指 
	 * 定精度，以后的数字四舍五入。 
	 * @param vf 被除数 
	 * @param vs 除数 
	 * @param scale 表示需要精确到小数点以后几位。 
	 * @return 两个参数的商 
	 * @throws Exception 
	 */
	public static String div(String vf, String vs, int scale) throws Exception {
		if (scale < 0) {
			throw new Exception("除法精度必须大于0!");
		}
		BigDecimal bf = new BigDecimal(vf.trim());
		BigDecimal bs = new BigDecimal(vs.trim());
		return (bf.divide(bs, scale, BigDecimal.ROUND_HALF_UP)).toString().trim();
	}
	
	/**
	 * 从前台传的Map参数中构建分页类实例
	 * @param confMap {"pageSize":1,"curPage":1}
	 * */
	public static PagingInfo buildPagingInfo(Map confMap) {
		int limit = 10,offset = 0;
		if (confMap.get("pageSize") != null) {
			limit = Integer.valueOf((String) confMap.get("pageSize")).intValue();
		}
		if (confMap.get("curPage") != null) {
			offset = ((Integer.valueOf((String) confMap.get("curPage")).intValue())-1)*limit;
		}
		return new PagingInfo(offset,limit);
	}
	
	/**
	 * 将分页结果转换为回传给前台的Map
	 * @param confMap {"pageSize":1,"curPage":1,"totalSize":1}
	 * */
	public static Map<String,Integer> convertOldPageMap(PagingInfo page, Long totalSize){
		Map<String,Integer> pageParam = new HashMap<String,Integer>();
		pageParam.put("curPage", (page.getOffset()/page.getLimit())+1);
		pageParam.put("totalSize", totalSize.intValue());
		pageParam.put("pageSize", page.getLimit());
		return pageParam;
	}
	
	/**
	 * des加密
	 * 
	 * @param srcMsg
	 * @param sKey
	 * @return
	 */
	public static String desEncrypt(String srcMsg,String sKey){
		String encMsg = null;
		// 补齐8位
		int modLen = 8 - srcMsg.length() % 8;
		char a = 0;
		while(modLen > 0){
			srcMsg = srcMsg.concat(String.valueOf(a));
			modLen--;
		}
		SecretKey secretKey;
		sun.misc.BASE64Encoder encoder = null;
		try{
			encoder = new sun.misc.BASE64Encoder();
			SecretKeyFactory keyFac = SecretKeyFactory.getInstance("DES");
			DESKeySpec desKeySpec;
			desKeySpec = new DESKeySpec(sKey.getBytes());
			secretKey = keyFac.generateSecret(desKeySpec);
			Cipher desCipher = Cipher.getInstance("DES/ECB/NoPadding");
			desCipher.init(Cipher.ENCRYPT_MODE,secretKey);
			byte[] encText = desCipher.doFinal(srcMsg.getBytes());
			encMsg = byte2HexStr(encText);
		}catch(Exception ex){
			throw new BaseException("DES加密异常：",ex);
		}
		return encMsg;
	}
	/**
	 * bytes转换成十六进制字符串
	 */
	public static String byte2HexStr(byte[] b){
		String hs = "";
		String stmp = "";
		for(int n = 0;n < b.length;n++){
			stmp = Integer.toHexString(b[n] & 0XFF);
			if(stmp.length() == 1){
				hs = hs + "0" + stmp;
			}else{
				hs = hs + stmp;
			}
		}
		return hs.toUpperCase();
	}

	
	public static String parseToString(Object obj){
		if(obj==null) return null;
		return obj.toString();
	}
	
	/**
	 * 获取异常堆栈中的真实异常描述，取异常堆栈中不为NULL的异常消息
	 * */
	public static String getRealErrorMsg(Throwable e){
		String errorMsg = e.getMessage();
		if(errorMsg==null){
			Throwable e1 = e;
			while(e1!=null){
				if(e1.getMessage()==null)
					e1 = e1.getCause();
				else{
					errorMsg = e1.getMessage();
					break;
				}
			}
		}
		return errorMsg;
	}
	
	/**
	 * 获取异常堆栈中最底层真实的异常消息
	 * */
	public static String getRealErrorMsgDeeplest(Throwable e){
		//用于记录每一层异常的errorMsg
		List<String> errorMsgs = new ArrayList<String>();
		String errorMsg = e.getMessage();
		errorMsg = errorMsg == null ? "" : errorMsg;
		errorMsgs.add(errorMsg);
		Throwable e1 = e.getCause();
		while(e1!=null){
			errorMsg = e1.getMessage();
			errorMsgs.add(errorMsg);
			e1 = e1.getCause();
		}
		if(errorMsgs.size() > 1){
			//返回最底的两层errorMsg拼接成的字符串
			return errorMsg+"_"+errorMsgs.get(errorMsgs.size() - 2);
		}else{
			return errorMsg;
		}
	}
	
	public static String getRealErrorMsgInner(Throwable e){
		Stack<String> msgStack = new Stack<String>();
		String msgErr = null;
		while(e!=null){
			msgStack.push(e.getMessage());
			e = e.getCause();
		}

		while(!msgStack.empty()){
			msgErr = msgStack.pop();
			if(msgErr!=null&&StringUtils.isNotBlank(msgErr)){
				break;
			}
		}
		
		return msgErr==null?"":msgErr;
		
	}
	
	/**
	 * 获取封装异常中的BaseException
	 * */
	public static BaseException getBaseException(Throwable e){
		int i = 0;
		Throwable t = e;
		while(t != null && i++<5){
			if(t instanceof BaseException){
				return (BaseException)t;
			}
			t = t.getCause();
		}
		return null;
	}
	
	/**
     * 获取字符串的长度，如果有中文，则每个中文字符计为2位
     * 
     * @param value
     *            指定的字符串
     * @return 字符串的长度
     */
    public static int length(String value) {
    	if(value==null) return 0;
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        /* 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1 */
        for (int i = 0; i < value.length(); i++) {
            /* 获取一个字符 */
            String temp = value.substring(i, i + 1);
            /* 判断是否为中文字符 */
            if (temp.matches(chinese)) {
                /* 中文字符长度为2 */
                valueLength += 2;
            } else {
                /* 其他字符长度为1 */
                valueLength += 1;
            }
        }
        return valueLength;
    }

	public static String getCustOrderType () {
		return (String) custOrderType.get();
	}

	public static void setCustOrderType(String a) {
		custOrderType.set(a);
	}
}
