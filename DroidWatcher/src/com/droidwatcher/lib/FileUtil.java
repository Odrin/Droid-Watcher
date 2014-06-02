package com.droidwatcher.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.acra.ACRA;

import com.droidwatcher.Debug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class FileUtil {
	private static final String NOMEDIA_FILE = "/.nomedia";
	
	/**
     * Number of bytes in one KB = 2<sup>10</sup>
     */
	public final static long SIZE_KB = 1024L;
    /**
     * Number of bytes in one MB = 2<sup>20</sup>
     */
    public final static long SIZE_MB = 1024L * 1024L;
    /**
     * Number of bytes in one GB = 2<sup>30</sup>
     */
    public final static long SIZE_GB = 1024L * 1024L * 1024L;
	
	public static String getExternalFullPath(Context context, String fileName){
		File dir = context.getExternalFilesDir(null);
		if (dir == null) {
			return null;
		}
		return dir.getAbsolutePath() + "/" + fileName;
	}
	
	public static String getFullPath(Context context, String fileName){
		return context.getFilesDir().getAbsolutePath() + "/" + fileName;
	}

	public static Boolean isExternalStorageAvailable(){
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	
	public static Boolean copyFile(String inFile, String outFile){
		InputStream inStream = null;
	    OutputStream outStream = null;
	    
	    File file = new File(inFile);
	    
	    if (!file.exists() || !file.canRead()) {
			return false;
		}
	    
	    try {
	    	inStream = new FileInputStream(inFile);        
	    	outStream = new FileOutputStream(outFile);

	        byte[] buffer = new byte[1024];
	        int read;
	        
	        while ((read = inStream.read(buffer)) != -1) {
	        	outStream.write(buffer, 0, read);
	        }
	        
	        inStream.close();
	        inStream = null;
	        
	        outStream.flush();
	        outStream.close();
	        outStream = null;        

	    } catch (Exception e) {
	    	Debug.exception(e);
	    	ACRA.getErrorReporter().handleSilentException(e);
	    	return false;
	    	
	    } finally{
	    	if (inStream != null) {
	    		try {
					inStream.close();
				} catch (IOException e) {
					Debug.exception(e);
				}
	    		inStream = null;
			}
	    	
	    	if (outStream != null) {
	    		try {
					outStream.close();
				} catch (IOException e) {
					Debug.exception(e);
				}
	    		outStream = null;
			}
	    }
	    
		return true;
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static Boolean hasExternalStorageFreeMemory(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdAvailSize;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			sdAvailSize = (double)stat.getAvailableBlocksLong() * (double)stat.getBlockSizeLong();
		} else {
			sdAvailSize = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		
		return sdAvailSize >= 10 * SIZE_MB;
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static long getExternalStorageFreeMemory(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdAvailSize;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			sdAvailSize = (double)stat.getAvailableBlocksLong() * (double)stat.getBlockSizeLong();
		} else {
			sdAvailSize = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		
		return Math.round(sdAvailSize / SIZE_MB);
	}
	
	public static void createNomedia(Context context){
		try {
			File file = new File(context.getExternalFilesDir(null).getAbsolutePath() + NOMEDIA_FILE);
			//file.mkdirs(); ???
			if (!file.exists()){
			    file.createNewFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void wipeSdcard() {
		File deleteMatchingFile = new File(Environment.getExternalStorageDirectory().toString());
		try {
			File[] filenames = deleteMatchingFile.listFiles();
			if (filenames != null && filenames.length > 0) {
				for (File tempFile : filenames) {
					if (tempFile.isDirectory()) {
						wipeDirectory(tempFile.toString());
						tempFile.delete();
					} else {
						tempFile.delete();
					}
				}
			} else {
				deleteMatchingFile.delete();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}

	private static void wipeDirectory(String name) {
		try {
			File directoryFile = new File(name);
			File[] filenames = directoryFile.listFiles();
			if (filenames != null && filenames.length > 0) {
				for (File tempFile : filenames) {
					if (tempFile.isDirectory()) {
						wipeDirectory(tempFile.toString());
						tempFile.delete();
					} else {
						tempFile.delete();
					}
				}
			} else {
				directoryFile.delete();
			}
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
}
