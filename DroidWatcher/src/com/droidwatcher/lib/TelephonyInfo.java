package com.droidwatcher.lib;

import java.lang.reflect.Method;

import com.droidwatcher.Debug;

import android.content.Context;
import android.telephony.TelephonyManager;

public final class TelephonyInfo {
	private static TelephonyInfo telephonyInfo;
	private String imeiSIM1;
	private String imeiSIM2;
	private boolean isSIM1Ready;
	private boolean isSIM2Ready;

	public String getImeiSIM1() {
		return imeiSIM1;
	}

	/*
	 * public static void setImeiSIM1(String imeiSIM1) { TelephonyInfo.imeiSIM1
	 * = imeiSIM1; }
	 */

	public String getImeiSIM2() {
		return imeiSIM2;
	}

	/*
	 * public static void setImeiSIM2(String imeiSIM2) { TelephonyInfo.imeiSIM2
	 * = imeiSIM2; }
	 */

	public boolean isSIM1Ready() {
		return isSIM1Ready;
	}

	/*
	 * public static void setSIM1Ready(boolean isSIM1Ready) {
	 * TelephonyInfo.isSIM1Ready = isSIM1Ready; }
	 */

	public boolean isSIM2Ready() {
		return isSIM2Ready;
	}

	/*
	 * public static void setSIM2Ready(boolean isSIM2Ready) {
	 * TelephonyInfo.isSIM2Ready = isSIM2Ready; }
	 */

	public boolean isDualSIM() {
		return imeiSIM2 != null;
	}

	private TelephonyInfo() {
	}

	public static TelephonyInfo getInstance(Context context) {
		if (telephonyInfo == null) {
			telephonyInfo = new TelephonyInfo();

			TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
			
			if (telephonyManager == null) {
				telephonyInfo.imeiSIM1 = null;
				telephonyInfo.imeiSIM2 = null;
				
				telephonyInfo.isSIM1Ready = false;
				telephonyInfo.isSIM2Ready = false;
				
				return telephonyInfo;
			}

			telephonyInfo.imeiSIM1 = telephonyManager.getDeviceId();
			telephonyInfo.imeiSIM2 = null;

			try {
				telephonyInfo.imeiSIM1 = getDeviceIdBySlot(context, "getDeviceIdGemini", 0);
				telephonyInfo.imeiSIM2 = getDeviceIdBySlot(context, "getDeviceIdGemini", 1);
				
			} catch (GeminiMethodNotFoundException e) {
				Debug.exception(e);
				
				try {
					telephonyInfo.imeiSIM1 = getDeviceIdBySlot(context, "getDeviceId", 0);
					telephonyInfo.imeiSIM2 = getDeviceIdBySlot(context, "getDeviceId", 1);
					
				} catch (GeminiMethodNotFoundException e1) {
					Debug.exception(e1);
				}
			}

			telephonyInfo.isSIM1Ready = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
			telephonyInfo.isSIM2Ready = false;

			try {
				telephonyInfo.isSIM1Ready = getSIMStateBySlot(context, "getSimStateGemini", 0);
				telephonyInfo.isSIM2Ready = getSIMStateBySlot(context, "getSimStateGemini", 1);
			} catch (GeminiMethodNotFoundException e) {
				Debug.exception(e);

				try {
					telephonyInfo.isSIM1Ready = getSIMStateBySlot(context, "getSimState", 0);
					telephonyInfo.isSIM2Ready = getSIMStateBySlot(context, "getSimState", 1);
				} catch (GeminiMethodNotFoundException e1) {
					// Call here for next manufacturer's predicted method name
					// if you wish
					Debug.exception(e1);
				}
			}
		}

		return telephonyInfo;
	}

	private static String getDeviceIdBySlot(Context context, String predictedMethodName, int slotID) throws GeminiMethodNotFoundException {
		String imei = null;
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		try {

			Class<?> telephonyClass = Class.forName(telephony.getClass().getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = int.class;
			Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimID.invoke(telephony, obParameter);

			if (ob_phone != null) {
				imei = ob_phone.toString();

			}
		} catch (Exception e) {
			Debug.exception(e);
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return imei;
	}

	private static boolean getSIMStateBySlot(Context context, String predictedMethodName, int slotID) throws GeminiMethodNotFoundException {
		boolean isReady = false;
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		try {
			Class<?> telephonyClass = Class.forName(telephony.getClass().getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = int.class;
			Method getSimStateGemini = telephonyClass.getMethod(predictedMethodName, parameter);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimStateGemini.invoke(telephony, obParameter);

			if (ob_phone != null) {
				int simState = Integer.parseInt(ob_phone.toString());
				if (simState == TelephonyManager.SIM_STATE_READY) {
					isReady = true;
				}
			}
		} catch (Exception e) {
			Debug.exception(e);
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return isReady;
	}

	private static class GeminiMethodNotFoundException extends Exception {
		private static final long serialVersionUID = -996812356902545308L;

		public GeminiMethodNotFoundException(String info) {
			super(info);
		}
	}

	public static void printTelephonyManagerMethodNamesForThisDevice(Context context) {
		TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		Class<?> telephonyClass;
		try {
			telephonyClass = Class.forName(telephony.getClass().getName());
			Method[] methods = telephonyClass.getMethods();
			for (int idx = 0; idx < methods.length; idx++) {

				System.out.println("\n" + methods[idx] + " declared by " + methods[idx].getDeclaringClass());
			}
		} catch (ClassNotFoundException e) {
			Debug.exception(e);
		}
	}
}
