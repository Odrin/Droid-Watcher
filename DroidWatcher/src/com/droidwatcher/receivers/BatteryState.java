package com.droidwatcher.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryState extends BroadcastReceiver {
	private static int sBatteryLevel = 100;
	
	public static int getBatteryLevel(){
		return sBatteryLevel;
	}
	
	private static volatile BatteryState instance;
    
    public static BatteryState getInstance() {
    	BatteryState localInstance = instance;
    	if (localInstance == null) {
    		synchronized (BatteryState.class) {
    			localInstance = instance;
    			if (localInstance == null) {
    				instance = localInstance = new BatteryState();
    			}
        	}
    	}
    	return localInstance;
    }
    
    public static void dispose(){
    	instance = null;
    }
    
    private BatteryState(){}

	@Override
	public void onReceive(Context context, Intent intent) {
		int rawlevel = intent.getIntExtra("level", -1);
		int scale = intent.getIntExtra("scale", -1);
		int lev = -1;
		
		if (rawlevel >= 0 && scale > 0) {
			lev = (rawlevel * 100) / scale;
		}

		sBatteryLevel = lev;
	}

}
