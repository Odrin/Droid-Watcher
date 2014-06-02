package com.droidwatcher.variables;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.lib.IMessageBody;

public class ErrorMessage implements IMessageBody {
	private Long date;
	private String text;
	
	public ErrorMessage(Long date, String text){
		this.date = date;
		this.text = text;
	}
	
	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("date", date);
			obj.put("text", text);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}

}
