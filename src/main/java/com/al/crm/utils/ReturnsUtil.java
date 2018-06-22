package com.al.crm.utils;

import com.al.common.exception.BaseException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;


/**
 * 协议处理工具类
 * 
 * @author 
 * 
 */
public class ReturnsUtil {
	
	/**
	 * 处理成功返回协议对象
	 * @param jsonObject 
	 * @return
	 */
	public static JSONObject returnSuccess(Object jsonObject, boolean isFilteNull){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("resultCode","0");
		jsonObj.put("resultMsg","处理成功!");
		if (jsonObject==null) return jsonObj;
		if(jsonObject instanceof Collection || jsonObject.getClass().isArray()){
			jsonObj.put("result",JSONArray.fromObject(jsonObject,isFilteNull?JsonUtil.FILTENULL_AND_DATTE_JSONCONF:JsonUtil.DEFAULT_AND_DATTE_JSONCONF));
		}else{
			jsonObj.put("result",JSONObject.fromObject(jsonObject,isFilteNull?JsonUtil.FILTENULL_AND_DATTE_JSONCONF:JsonUtil.DEFAULT_AND_DATTE_JSONCONF));
		}
		return jsonObj;
	}
	
	/**
	 * 能力开放已生成正式单，返回协议对象
	 * @param jsonObject 
	 * @return
	 */
	public static JSONObject returnHasFormalorder(){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("resultCode","2");
		jsonObj.put("resultMsg","已生成正式单!");
		JSONObject resultMsgjsonObj = new JSONObject();
		jsonObj.put("result", resultMsgjsonObj);
		return jsonObj;
	}
	
	/**
	 * 能力开放未找到暂存单，返回协议对象
	 * @param jsonObject 
	 * @return
	 */
	public static JSONObject returnNoTSOrder(){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("resultCode","1");
		jsonObj.put("resultMsg","未找到provTransId对应的暂存单!");
		JSONObject resultMsgjsonObj = new JSONObject();
		jsonObj.put("result", resultMsgjsonObj);
		return jsonObj;
	}

	/**
	 * 处理成功返回协议对象
	 * @param jsonObject 
	 * @return
	 */
	public static JSONObject returnSuccess(Object jsonObject){
		return returnSuccess(jsonObject, false);
	}

	/**
	 * 处理异常
	 * @param msg 异常信息
	 * @return
	 */
	public static JSONObject returnException(String msg, Exception e){
		return returnException(null,msg,e);
		/*JSONObject jsonObj = new JSONObject();
		jsonObj.put("resultCode","-1");
		jsonObj.put("resultMsg",msg);
		if(e!=null){
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(out));
			jsonObj.put("errorStack", new String(out.toByteArray()));
		}
		return jsonObj;*/
	}
	
	/**
	 * 处理异常
	 * @param msg 异常信息
	 * @return
	 */
	public static JSONObject returnException(String code , String msg, Exception e){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("resultCode","-1");
		msg = msg == null ? "" : msg;
		if(e != null){
			msg=msg+CommonUtil.getRealErrorMsgDeeplest(e);
			msg=msg.length() > 300 ? msg.substring(0, 300-1):msg;
		}
		jsonObj.put("resultMsg",msg);
		if(code!=null){
			jsonObj.put("errCode", code);
		}
		if(e!=null){
			if(e instanceof BaseException){
				String errorStr = ((BaseException) e).getError();
				Integer errorCode = ((BaseException) e).getCode();
				if(errorCode!=null){
					jsonObj.put("errCode", errorCode);
				}
				if(StringUtils.isNotBlank(errorStr)&&!"RuntimeException".equals(errorStr)){
					jsonObj.put("errCode", errorStr);
				}
			}
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			e.printStackTrace(new PrintStream(out));
//			try {
//				byte[] lens = out.toByteArray();
//				jsonObj.put("errorStack", new String(lens,MDA.ERROR_INFO_ENCODE));
//			} catch (UnsupportedEncodingException e1) {
//			}
			jsonObj.put("errorStack", getStackTrackMessage(e));
		}
		if(!jsonObj.containsKey("errCode")){
			//如果业务代码中没有异常编码，即使用受理的通用异常编码
			jsonObj.put("errCode","020064");
		}
		String errCode = JsonUtil.getStringFromJSON(jsonObj, "errCode", "异常出参");
//		if(StringUtils.isNotBlank(errCode)&&errCode.startsWith("06"))jsonObj.put("resultMsg", "业务规则校验失败");
		if(StringUtils.isNotBlank(errCode)&&"-2".equals(errCode)){
			jsonObj.element("resultCode", errCode);
			jsonObj.element("resultMsg", msg);
			jsonObj.element("errCode", "029999");
		}
		return jsonObj;
	}
	/**
	 * 提供给综合生单/订单提交/集约化校验单/一点等接口处理异常
	 * @param msg 异常信息
	 * @author ZeuSier
	 * @return
	 */
	public static JSONObject returnCommitException(String code , String msg, Exception e){
		JSONObject jsonObj = new JSONObject();
		JSONObject returnObj = new JSONObject();
		String resultCode="-1";
		String errorStack=null;
		if(e!=null){
			if(e instanceof BaseException){
				String errorStr = ((BaseException) e).getError();
				code = String.valueOf(((BaseException) e).getCode());
				if(StringUtils.isNotBlank(errorStr)&&!"RuntimeException".equals(errorStr)){
					code=errorStr;
				}
			}
			errorStack=getStackTrackMessage(e);
		}
		//异常统一处理
		if(StringUtils.isNotBlank(code)){
				if(code.startsWith("06"))msg="业务规则校验失败_";
				if(code.startsWith("6"))msg="业务规则异常_";
				if(code.startsWith("08"))msg="省份异常_";
				if(code.startsWith("04"))msg="资源异常_";
				if(code.startsWith("03"))msg="接口或能力平台异常_";
				if(code.equals("-90000"))code="020064";
				if("-2".equals(code)){resultCode="-2";code="029999";}
			}else{code="020064";}
		msg=msg+CommonUtil.getRealErrorMsgDeeplest(e);
		msg=msg.length() > 300 ? msg.substring(0, 300-1):msg;
		returnObj.put("resultCode",resultCode);
		returnObj.put("errCode", code);
		returnObj.put("resultMsg",msg);
		returnObj.put("errorStack", errorStack);
		return returnObj;
	}
		
	/**
	 * 生成BaseException，主要用来将字符串的异常编码塞到其中的error属性中，后续统一返回
	 * */
	public static BaseException throwBaseException(String code , String msg, Exception e){
		BaseException exc = new BaseException(msg,e);
		exc.setError(code);
		return exc;
	}
	
	/**
	 * 生成BaseException，主要用来将字符串的异常编码塞到其中的error属性中，后续统一返回（无父级异常）
	 * */
	public static BaseException throwBaseException(String code , String msg){
		BaseException exc = new BaseException(msg);
		exc.setError(code);
		return exc;
	}
	
	/**
	 * 生成错误堆栈信息
	 * @param e
	 * @return
	 */
	public static String getStackTrackMessage(Exception e) {
        StringBuffer messsage = new StringBuffer();
        if (e != null) {
        	messsage.append(e.getClass()).append(": ").append(e.getMessage()).append("\n");
            StackTraceElement[] elements = e.getStackTrace();
            for (StackTraceElement stackTraceElement : elements) {
                messsage.append("\t").append(stackTraceElement.toString()).append("\n");
            }
            Throwable localThrowable = e.getCause();
            if (localThrowable != null) {
            	getStackTraceAsCause(messsage, localThrowable);
            }
        }
        return messsage.toString();
    }

	private static void getStackTraceAsCause(StringBuffer messsage, Throwable localThrowable) {
		messsage.append(localThrowable.getClass()).append(": ").append(localThrowable.getMessage()).append("\n");
        StackTraceElement[] elements = localThrowable.getStackTrace();
        for (StackTraceElement stackTraceElement : elements) {
            messsage.append("\t").append(stackTraceElement.toString()).append("\n");
        }
		localThrowable = localThrowable.getCause();
		if (localThrowable != null) {
        	getStackTraceAsCause(messsage, localThrowable);
        }
	}
	
	public static JSONObject returnResultOrganize(Map<String,Object> resultMap, Boolean singleResult){
		JSONObject jsonObj = new JSONObject();
		String resultCode = resultMap.get("resultCode").toString();
		jsonObj.put("resultCode", resultCode);
		String resultMsg = "";
		if(!resultMap.containsKey("resultMsg")||resultMap.get("resultMsg")==null||StringUtils.isBlank(resultMap.get("resultMsg").toString())){
			if("0".equals(resultCode)){
				resultMsg = "处理成功!";
			}
			else {
				resultMsg = "处理失败!";
			}
		}
		else{
			resultMsg = resultMap.get("resultMsg").toString();
		}
		jsonObj.put("resultMsg", resultMsg);
		
		if(resultMap.containsKey("errCode")){
			jsonObj.put("errCode", resultMap.get("errCode"));
		}
		if(resultMap.containsKey("errorStack")){
			jsonObj.put("errorStack", resultMap.get("errorStack"));
		}
		if(!singleResult){
			if(resultMap.containsKey("result")&&resultMap.get("result")!=null){
				if(Collection.class.isAssignableFrom(resultMap.get("result").getClass())){
					jsonObj.put("result", resultMap.get("result"));
				}
				else{
					JSONArray jsonArray = new JSONArray();
					jsonArray.add(resultMap.get("result"));
					jsonObj.put("result", jsonArray);
				}
			}
			else{
				JSONArray jsonArray = new JSONArray();
				jsonObj.put("result",jsonArray);
			}
		}
		else{
			jsonObj.put("result", resultMap.get("result"));
		}
		
		return jsonObj;
	}
	
}
