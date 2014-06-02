package com.droidwatcher.modules.location;

import com.droidwatcher.Debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocationTimerBroadcastReceiver extends BroadcastReceiver {
	public static int TIMER_REQUEST_CODE = 100;

	@Override
	public void onReceive(Context context, Intent intent) {
		Debug.i("[LocationTimerBroadcastReceiver] broadcast received");
		
		LocationModule.message(LocationModule.REQUEST_SCHEDULED_LOCATION);
	}

}
