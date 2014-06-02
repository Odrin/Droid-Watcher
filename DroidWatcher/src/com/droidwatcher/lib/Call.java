package com.droidwatcher.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.Debug;


public class Call implements IMessageBody {
	public String number;
	public long date;
	public long duration;
	public int type;
	public String name;
	public double lat;
	public double lon;
	
	public Call(String number, long date, long duration, int type, String name, double lat, double lon){
		this.number = number;
		this.duration = duration;
		this.type = type;
		this.date = date;
		
		if (name == null || name.length() == 0){
			this.name = "unknown";
		}
		else{
			this.name = name;
		}
		
		this.lat = lat;
		this.lon = lon;
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("number", number);
			obj.put("date", date);
			obj.put("duration", duration);
			obj.put("type", type);
			obj.put("name", name);
			
			if (lat != 0 && lon != 0) {
				obj.put("lat", lat);
				obj.put("lon", lon);
			}
			
		} catch (JSONException e) {
			Debug.exception(e);
		}
		return obj;
	}
	
	public String getStringDate(){
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, new Locale("ru","RU")).format(new Date(date));
	}
}
