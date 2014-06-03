package com.droidwatcher.activity;

import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidwatcher.Debug;
import com.droidwatcher.R;
import com.droidwatcher.ServerMessanger;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.ServerConst;
import com.droidwatcher.variables.ServerMessage;
import com.droidwatcher.variables.SimpleServerMessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StartupActivity extends Activity {
	
	private SettingsManager settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_startup);
		
		settings = new SettingsManager(this);
		
		if (settings.isConnected()) {
			startActivity(new Intent(this, MainMenuActivity.class));
			finish();
			return;
		}
		
		settings.remove(SettingsManager.KEY_IMEI);
		
		findViewById(R.id.startup_btn_enter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				enter();
			}
		});
		
		findViewById(R.id.startup_btn_register).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				register();
			}
		});
	} 
	
	private void enter(){
		if (!networkAvailable()) {
			Toast.makeText(this, R.string.connUnavailable, Toast.LENGTH_LONG).show();
			return;
		}
		
		LayoutInflater lInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = lInflater.inflate(R.layout.dialog_email, null);
		final EditText input = (EditText) view.findViewById(R.id.dialog_input_email);
		
		final AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	connectionRequset(input.getText().toString());
	            }
	        })
        	.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	dialog.cancel();
	            }
        	})
        	.setTitle(R.string.enterYourLogin)
        	.create();
		
		dialog.show();
	}
	
	private void register(){
		if (!networkAvailable()) {
			Toast.makeText(this, R.string.connUnavailable, Toast.LENGTH_LONG).show();
			return;
		}
		
		LayoutInflater lInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = lInflater.inflate(R.layout.dialog_registration, null);
		
		final EditText emailInput = (EditText) view.findViewById(R.id.dialog_input_email);
		final EditText pwdInput = (EditText) view.findViewById(R.id.dialog_input_pwd);
		
		final AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	
	            }
	        })
        	.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	dialog.cancel();
	            }
        	})
        	.setTitle(R.string.registration)
        	.create();
		
		dialog.show();
		
		Button dialogBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		dialogBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailInput.getText().toString();
            	String pwd = pwdInput.getText().toString();
            	
            	if (!isEmailCorrect(email)) {
            		Toast.makeText(StartupActivity.this, R.string.mailNotMatch, Toast.LENGTH_SHORT).show();
            		return;
				}
            	
            	if (pwd.length() < 6){
            		Toast.makeText(StartupActivity.this, R.string.shortPwd, Toast.LENGTH_SHORT).show();
                	return;
                }
            	
            	registrationRequest(email, pwd, dialog);
			}
		});
	}
	
	private void registrationRequest(final String login, String pwd, final AlertDialog dialog){
		final ProgressDialog wait = ProgressDialog.show(this, "Registration", "Please wait...", false, false);
		
		new ServerMessanger(this,
			new SimpleServerMessage(MessageType.REGISTER)
				.addParam("login", login)
				.addParam("pwd", pwd),
			new ServerMessanger.ICallBack() {
				@Override
				public boolean onFinished(String response) {
					wait.dismiss();
					
					final VSResponse r = new VSResponse(response);
					StartupActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							if (r.status()){
								Toast.makeText(StartupActivity.this, R.string.registrationSuccess, Toast.LENGTH_LONG).show();
								dialog.dismiss();
								connectionRequset(login);
							}
							else{
								Toast.makeText(StartupActivity.this, r.message(), Toast.LENGTH_LONG).show();
							}
						}
					});
					
					return true;
				}

				@Override
				public void onError() { }
				@Override
				public void onSuccess() { }
		}).start();
	}
	
	private void connectionRequset(String login){
		settings.login(login);
		final ProgressDialog wait = ProgressDialog.show(this, "Connecting", "Please wait...", false, false);
    	
    	new ServerMessanger(this, new ServerMessage(MessageType.CONNECT, settings.imei(), login), new ServerMessanger.ICallBack() {
			@Override
			public boolean onFinished(String response) {
				wait.dismiss();
				return false;
			}

			@Override
			public void onError() {
				StartupActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(StartupActivity.this, R.string.connLogin, Toast.LENGTH_LONG).show();
					}
				});
				
			}

			@Override
			public void onSuccess() {
				StartupActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						startActivity(new Intent(StartupActivity.this, StartupFinalActivity.class));
		        		StartupActivity.this.finish();
					}
				});
			}
		}).start();
	}
	
	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		return (info != null && info.isConnectedOrConnecting());
	}
	
	private Boolean isEmailCorrect(String email){
		Pattern pattern = Pattern.compile(
		          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
		          "\\@" +
		          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
		          "(" +
		          "\\." +
		          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
		          ")+"
		      );
		 return pattern.matcher(email).matches();
	}
	
	private class VSResponse {
	    private JSONObject jObj;

	    public VSResponse(String response) {
	        try {
	            jObj = new JSONObject(response);
	        } catch (JSONException e) {
	            jObj = new JSONObject();
	        }
	    }

	    public Boolean status() {
	        if (!jObj.isNull("status")) {
	            try {
	                return jObj.getString("status").equals(ServerConst.OK);
	            } catch (JSONException e) {
	                return false;
	            }
	        } else {
	            return false;
	        }
	    }

	    public String message() {
	        if (!jObj.isNull("message")) {
	            try {
	                return jObj.getString("message");
	            } catch (Exception e) {
	                return "";
	            }
	        } else {
	            return "";
	        }
	    }
	}
	
	
}
