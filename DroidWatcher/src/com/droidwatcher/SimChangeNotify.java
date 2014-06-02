package com.droidwatcher;

import org.acra.ACRA;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimChangeNotify extends Thread{
	private Context context;
	
	public SimChangeNotify(Context context){
		this.context = context;
	}
	
	@Override
	public void run() {
		try {
			SettingsManager mgr = new SettingsManager(context);
			
			String IMSI = mgr.imsi();
			
			TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String currentIMSI = null;
			if (tm != null){
				currentIMSI = tm.getSubscriberId();
			}
			
			if (currentIMSI == null || currentIMSI.length() == 0){
				currentIMSI = "0";
			}
			mgr.imsi(currentIMSI);
			Log.i("DEBUG", "IMSI: " + currentIMSI);
			Log.i("DEBUG", "LAST IMSI: " + IMSI);
			if (IMSI.equals(currentIMSI)){
				return;
			}
			
			Log.i("DEBUG", "SIM CHANGED");
			/*
			 * Notification
			 */
			if (!mgr.isSimChangeNotificationEnabled()){
				return;
			}
			
			String number = mgr.notifyNumber();			
			String text = "SIM-card changed! IMEI: " + mgr.imei() + " IMSI: " + currentIMSI;
			
			SmsNotification.sendSms(context, text, number);
			
			Log.i("DEBUG", "SMS SEND");
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
}
