package com.droidwatcher;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.droidwatcher.security.SecuriryInfo;

import android.app.Application;

@ReportsCrashes(
        formKey = "",
        formUri = SecuriryInfo.FormUri,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = SecuriryInfo.FormUriBasicAuthLogin,
        formUriBasicAuthPassword = SecuriryInfo.FormUriBasicAuthPassword,
        mode = ReportingInteractionMode.SILENT
        )
public class MyApplication extends Application {
	@Override
    public void onCreate() {
        super.onCreate();
        
        Debug.setRootToolsDebugMode();
        
        if (!Debug.DEBUG) {
        	ACRA.init(this);
		}
    }
}
