package com.droidwatcher;

import java.util.HashMap;

import com.stericson.RootTools.RootTools;

import android.util.Log;

public class Debug {
	//TODO: debug
	public static final Boolean DEBUG = true;
	private static final String tag = "DEBUG";
	
	public static void setRootToolsDebugMode(){
		RootTools.debugMode = DEBUG;
	}
	
	public static void i(String msg){
		i(tag, msg);
	}
	
	private static void i(String tag, String msg){
		if (DEBUG) {
			Log.i(tag, msg);
		}
	}
	
	public static void w(String msg){
		w(tag, msg, null);
	}
	
	public static void w(String msg, Throwable tr){
		w(tag, msg, tr);
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
			Log.i(tag, sb.toString());
		}
	}
	
	public static class Timer{
		private static HashMap<String, Long> timers = new HashMap<String, Long>();
		
		public synchronized static void start(String name){
			Log.i("TIMER", name + ": start timer");
			timers.put(name, System.currentTimeMillis());
		}
		
		public synchronized static void stop(String name){
			if (timers.containsKey(name)) {
				Long begin = timers.get(name);
				Log.i("TIMER", name + ": " + (System.currentTimeMillis() - begin) + " ms.");
				timers.remove(name);
			}
			else{
				Log.w("TIMER", name + ": timer does not exist");
			}
		}
	}
}
