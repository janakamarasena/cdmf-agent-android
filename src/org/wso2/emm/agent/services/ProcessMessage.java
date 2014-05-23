/*
 ~ Copyright (c) 2014, WSO2 Inc. (http://wso2.com/) All Rights Reserved.
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
*/
package org.wso2.emm.agent.services;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wso2.emm.agent.R;
import org.wso2.emm.agent.parser.PayloadParser;
import org.wso2.emm.agent.utils.CommonUtilities;
import org.wso2.emm.agent.utils.LoggerCustom;
import org.wso2.emm.agent.utils.ServerUtils;
import org.wso2.mobile.idp.proxy.APIController;
import org.wso2.mobile.idp.proxy.APIResultCallBack;
import org.wso2.mobile.idp.proxy.APIUtilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class ProcessMessage  implements APIResultCallBack{
	Operation operation;
	Map<String, String> params;
	AsyncTask<Void, Void, String> sendReply;
	Map<String, String> responsePayload;
	Context context;
	String replyPayload;
	public static boolean stillProcessing=false;
	
	public ProcessMessage(Context context, int mode, String message, String recepient) {
		// TODO Auto-generated constructor stub
		JSONParser jp = new JSONParser();
		params = new HashMap<String, String>();
    	try {
    		
			JSONObject jobj = new JSONObject(message);
			Log.v("JSON OUTPUT : ", jobj.toString());
            params.put("code", (String)jobj.get("message"));
            if(jobj.has("data")){
            	params.put("data", ((JSONObject)jobj.get("data")).toString());
            }
            
            operation = new Operation(context, mode, params, recepient);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
	}
	
	
	
	// local notification message handler
	public ProcessMessage(Context context) {
		this.context = context;
	}
	
	public void getOperations(String replyData) {
		Log.e("stillProcessing",stillProcessing+" ");
		if(stillProcessing==false){
    		String isActive = CommonUtilities.getPref(context, context.getResources().getString(R.string.shared_pref_device_active));
    		if (isActive.equals("1")) {
    			try {
    				SharedPreferences mainPref =
    						context.getSharedPreferences(context.getResources()
    				                                                     .getString(R.string.shared_pref_package),
    				                                                    Context.MODE_PRIVATE);
    				String regId =
    				               mainPref.getString(context.getResources()
    				                                   .getString(R.string.shared_pref_regId), "");
    				Map<String, String> requestParams = new HashMap<String, String>();
    				if (replyData != null) {
    					requestParams.put("data", replyPayload);
    				}
    				requestParams.put("regId", regId);
    
    				ServerUtils.callSecuredAPI(context,
    				   						CommonUtilities.NOTIFICATION_ENDPOINT,
    				   						CommonUtilities.POST_METHOD, requestParams,
    				   						ProcessMessage.this,
    				   						CommonUtilities.NOTIFICATION_REQUEST_CODE);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}

		}
		
	}

	@Override
	public void onReceiveAPIResult(Map<String, String> result, int requestCode) {
		String responseStatus = "";
		String response = "";
		Log.e("onReceiveAPIResult",requestCode +" ");
		if (requestCode == CommonUtilities.NOTIFICATION_REQUEST_CODE) { 
			Log.e("onReceiveAPIResult",responseStatus +"2");
			if (result != null) {
				responseStatus = result.get(CommonUtilities.STATUS_KEY);
				Log.e("onReceiveAPIResult",responseStatus +" ");
				if (responseStatus.equals(CommonUtilities.REQUEST_SUCCESSFUL)) {
					response = result.get("response");
					//processMsg = new ProcessMessage(context, CommonUtilities.MESSAGE_MODE_LOCAL, response);
					if(response!=null && !response.equals("") && !response.equals("null")){
			    		Log.e("responseresponse", " "+response);
			    		messageExecute(response);
					}
				}
			}
		}
		
	}
	
	
	
	
	
	

	private void messageExecute(String msg) {
		stillProcessing=true;
		JSONArray repArray =new JSONArray();
		JSONObject jsReply=null;
		String msgId="";
		
		
		JSONArray dataReply=null;
		try {
	        JSONArray jArr=new JSONArray(msg.trim());
	        for(int i=0;i<jArr.length();i++){
	        	JSONArray innerArr=new JSONArray(jArr.getJSONObject(i).getString("data"));
	        	String featureCode=jArr.getJSONObject(i).getString("code");
	        	dataReply=new JSONArray();
	        	jsReply=new JSONObject();
	        	jsReply.put("code",featureCode);
	        	
	        	
	        	for(int x=0;x<innerArr.length();x++){
    	        		msgId=innerArr.getJSONObject(x).getString("messageId");
    	        		jsReply.put("messageId", msgId);
    	        		
    	        		if (featureCode.equals(CommonUtilities.OPERATION_POLICY_BUNDLE)) {
    						SharedPreferences mainPrefp =
    						                              context.getSharedPreferences("com.mdm",
    						                                                    Context.MODE_PRIVATE);
    						
    						Editor editorp = mainPrefp.edit();
    						editorp.putString("policy", "");
    						editorp.commit();

    						SharedPreferences mainPref =
    						                             context.getSharedPreferences("com.mdm",
    						                                                    Context.MODE_PRIVATE);
    						Editor editor = mainPref.edit();
    						String arrToPut=innerArr.getJSONObject(0).getJSONArray("data").toString();
    						
    						LoggerCustom l =new LoggerCustom(context);
    						l.writeStringAsFile(arrToPut, "wso2log.txt");
    						
    						Log.e("aslkdmlamsd",arrToPut);
    						editor.putString("policy", arrToPut);
    						editor.commit();
    		        	}
    	        		
    	        		String msgData=innerArr.getJSONObject(x).getString("data");
    	        		JSONObject dataObj=new JSONObject("{}");
    	        		operation = new Operation(context);
    	        		if(featureCode.equalsIgnoreCase(CommonUtilities.OPERATION_POLICY_REVOKE)){
    	        			 operation.operate(featureCode,jsReply);
    	        			 jsReply.put("status", msgId);
    	        		}else{
        	        		if(msgData.charAt(0)=='['){
        	        			JSONArray dataArr=new JSONArray(msgData);
        	        			for(int a=0;a<dataArr.length();a++){
        	        				JSONObject innterDataObj=dataArr.getJSONObject(a);
        	        				featureCode=innterDataObj.getString("code");
        	        				String dataTemp=innterDataObj.getString("data");
        	        				if(!dataTemp.isEmpty() && dataTemp!=null && !dataTemp.equalsIgnoreCase("null"))
        	        					dataObj =innterDataObj.getJSONObject("data");
        	        				
        	        				dataReply= operation.operate(featureCode,dataObj);
        	        				//dataReply.put(resultJson);
        	        			}
        	        		}else {
        	        			if(!msgData.isEmpty() && msgData!=null && !msgData.equalsIgnoreCase("null"))
        	        				dataObj =new JSONObject(msgData);
        	        			dataReply= operation.operate(featureCode,dataObj);
        	        			//dataReply.put(resultJson);
        	        		}
    	        		}
	        		
	        	}
	        	jsReply.put("data", dataReply);
        		repArray.put(jsReply);
	        }
        } catch (JSONException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		catch (Exception e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		finally{
			SharedPreferences mainPref = context.getSharedPreferences( context.getResources().getString(R.string.shared_pref_package), Context.MODE_PRIVATE);
			String regId=mainPref.getString(context.getResources().getString(R.string.shared_pref_regId), "");
			PayloadParser ps=new PayloadParser();
			
			replyPayload=ps.generateReply(repArray,regId);
			Log.e("reply Payload",replyPayload);
			stillProcessing=false;
			getOperations(replyPayload);
			
		}
	    
    }

	
}
