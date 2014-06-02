package com.droidwatcher.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.droidwatcher.SettingsManager;

import android.content.Context;

public class FilterModule {
	private static List<String> list = null;
	/**
	 * Watch for sms/call from numbers
	 * 0 - only from list
	 * 1 - except list
	 */
	private static int type = 0;
	private static Boolean enabled = false;
	
	public static synchronized Boolean isNumberFiltered(Context context, String number){
		if (list == null) {
			init(context);
		}
		
		if (!enabled) {
			return false;
		}
		
		for (String n : list) {
			if (n.equals(number)) {
				return type == 1;
			}
		}
		
		return type == 0;
	}
	
	private static void init(Context context){
		list = new ArrayList<String>();
		try {
			SettingsManager settings = new SettingsManager(context);
			String filterString = settings.filterList();
			if (filterString.length() > 0) {
				list = Arrays.asList(filterString.split(","));
			}
			
			enabled = settings.isFilterEnabled();
			type = Integer.parseInt(settings.filterType());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized void reset(){
		list = null;
	}
	
}
