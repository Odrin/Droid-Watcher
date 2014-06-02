package com.droidwatcher.lib;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.receivers.BatteryState;

import android.location.Location;


public class GPS implements IMessageBody {
	public double acc;
	public int alt;
	public double lat;
	public double lon;
	public long date;
	public int battery;
	public String provider;
	
	public GPS(Location location){
		this.acc = (double) Math.round(location.getAccuracy() * 100) / 100;
		this.alt = (int) Math.round(location.getAltitude());
		this.lat = location.getLatitude();
		this.lon = location.getLongitude();
		this.date = location.getTime();
		this.battery = BatteryState.getBatteryLevel();
		this.provider = location.getProvider();
	}
	
	public GPS(double acc, double alt, double lat, double lon, long date){
		this.acc = acc;
		this.alt = (int) Math.round(alt);
		this.lat = lat;
		this.lon = lon;
		this.date = date;
		this.battery = BatteryState.getBatteryLevel();
		this.provider = "NETWORK";
	}
	
	public GPS(double acc, double alt, double lat, double lon, long date, int battery, String provider){
		this.acc = acc;
		this.alt = (int) Math.round(alt);
		this.lat = lat;
		this.lon = lon;
		this.date = date;
		this.battery = battery;
		this.provider = provider;
	}

	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("acc", acc);
			obj.put("alt", alt);
			obj.put("lat", lat);
			obj.put("lon", lon);
			obj.put("date", date);
			obj.put("battery", battery);
			obj.put("provider", provider);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
}
