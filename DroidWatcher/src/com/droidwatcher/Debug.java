package com.droidwatcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.droidwatcher.services.AppService;
import com.stericson.RootTools.RootTools;

import android.os.Environment;
import android.util.Log;

public class Debug {
	//TODO: don't forget about debug
	public static final Boolean DEBUG = false;
	
	private static final String sTag = "DEBUG";
	private static StringBuilder sStringBuilder;
	private static SimpleDateFormat sDateFormat;
	
	public static void setRootToolsDebugMode(){
		RootTools.debugMode = DEBUG;
	}
	
	private static StringBuilder getStringBuilder(){
		if (sStringBuilder == null) {
			sStringBuilder = new StringBuilder();
			sDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
			
			sStringBuilder
				.append(sDateFormat.format(new Date())).append("\n")
				.append("version: ").append(AppService.APP_VERSION).append("\n")
				.append("root: ").append(AppService.isRootAvailable()).append("\n")
				.append(android.os.Build.MODEL).append("\n")
				.append(android.os.Build.VERSION.RELEASE).append("\n")
				.append(android.os.Build.BRAND).append("\n")
				.append(android.os.Build.PRODUCT).append("\n")
				.append(android.os.Build.MODEL).append("\n")
				.append("\n").append("- - - - - - - - - - - - - -").append("\n");
		}
		
		return sStringBuilder;
	}
	
	public static synchronized void debugLog(String msg){
		try {
			getStringBuilder().append(sDateFormat.format(new Date())).append(" ").append(msg).append("\n");
			
		} catch (Exception e) {
			exception(e);
		}
	}
	
	public static synchronized void debugLog(Exception exception){
		try {
			getStringBuilder().append(sDateFormat.format(new Date())).append(" ").append(exception.getMessage()).append("\n");
			
			StackTraceElement[] elements = exception.getStackTrace();
			if (elements == null || elements.length == 0) {
				return;
			}
			
			for (StackTraceElement element : elements) {
				sStringBuilder
					.append(element.getClassName())
					.append(" ")
					.append(element.getMethodName())
					.append(" ")
					.append(element.getLineNumber())
					.append("\n");
			}
			
		} catch (Exception e) {
			exception(e);
		}
	}
	
	public static synchronized void dumpDebugLog() {
		if (sStringBuilder == null) {
			return;
		}
		
		try {
			File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/droid_watcher_log.txt");
			
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					exception(e);
				}
			}
			
			BufferedWriter buf = null;
			try {
				buf = new BufferedWriter(new FileWriter(logFile, true));
				buf.append(sStringBuilder.toString());
				buf.newLine();
				
			} catch (IOException e) {
				exception(e);
			} finally{
				if (buf != null) {
					buf.close();
				}
			}
			
		} catch (Exception e) {
			exception(e);
		} finally {
			sStringBuilder = null;
		}
	}
	
	public static void i(String msg){
		i(sTag, msg);
	}
	
	private static void i(String tag, String msg){
		if (DEBUG) {
			Log.i(tag, msg);
		}
	}
	
	public static void w(String msg){
		w(sTag, msg, null);
	}
	
	public static void w(String msg, Throwable tr){
		w(sTag, msg, tr);
	}
	
	private static void w(String tag, String msg, Throwable tr){
		if (DEBUG) {
			if (tr == null) {
				Log.w(tag, msg);
			}
			else{
				Log.w(tag, msg, tr);
			}
		}
	}
	
	public static void exception(Exception e){
		if (DEBUG && e != null) {
			e.printStackTrace();
		}
	}
	
	public static void stackTrace(StackTraceElement[] elements){
		if (DEBUG) {
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement element : elements) {
				sb.append(element.getClassName() + " " + element.getMethodName()).append("\n");
			}
			Log.i(sTag, sb.toString());
		}
	}
	
	public static class Timer{
		private static HashMap<String, Long> sTimers;
		
		public synchronized static void start(String name){
			Log.i("TIMER", name + ": start timer");
			
			if (sTimers == null) {
				sTimers = new HashMap<String, Long>();
			}
			
			sTimers.put(name, System.currentTimeMillis());
		}
		
		public synchronized static void stop(String name){
			if (sTimers != null && sTimers.containsKey(name)) {
				Long begin = sTimers.get(name);
				Log.i("TIMER", name + ": " + (System.currentTimeMillis() - begin) + " ms.");
				sTimers.remove(name);
			}
			else{
				Log.w("TIMER", name + ": timer does not exist");
			}
		}
	}
}
