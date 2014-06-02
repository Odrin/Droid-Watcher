package com.droidwatcher.modules.location;

import com.droidwatcher.lib.GPS;

import android.location.Location;

public interface ILocationResultListener {
	public void onLocationResult(Location location);
	public void onGsmLocationResult(GPS gps);
	public void onNoResult();
}
