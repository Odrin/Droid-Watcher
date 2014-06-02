package com.droidwatcher.receivers;

import com.droidwatcher.Debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class ScreenStateReceiver extends BroadcastReceiver {
	private static Boolean screenOff = null;
	private Context context;
	
	public static final String SCREEN_EVENT = "screen_state_changed";
	public static final String SCREEN_STATE_EXTRA = "screen_state";
	
	public static final int SCREEN_STATE_OFF = 0;
	public static final int SCREEN_STATE_ON = 1;
	public static final int SCREEN_STATE_UNKNOWN = 2;
	
	public ScreenStateReceiver(Context context){
		this.context = context;
	}
	
	public void start(){
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		try {
			filter.setPriority(Integer.MAX_VALUE);
			
		} catch (Exception e) {
			Debug.exception(e);
			filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		}
		
		context.registerReceiver(this, filter);
	}
	
	public void dispose(){
		try {
			context.unregisterReceiver(this);
			screenOff = null;
		} catch (Exception e) {
			Debug.exception(e);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			screenOff = true;
			LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(SCREEN_EVENT).putExtra(SCREEN_STATE_EXTRA, SCREEN_STATE_OFF)
			);
		} 
		else {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				screenOff = false;
				LocalBroadcastManager.getInstance(context).sendBroadcast(
					new Intent(SCREEN_EVENT).putExtra(SCREEN_STATE_EXTRA, SCREEN_STATE_ON)
				);
			}
		}
	}
	
	public static int getScreenState(){
		if (screenOff == null) {
			return SCREEN_STATE_UNKNOWN;
		}
		
		return screenOff ? SCREEN_STATE_OFF : SCREEN_STATE_ON;
	}

}
