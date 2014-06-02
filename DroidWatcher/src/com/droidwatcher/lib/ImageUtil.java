package com.droidwatcher.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.acra.ACRA;

import com.droidwatcher.Debug;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class ImageUtil {
	public static Bitmap getResizedImage(String path, int size) {
		Bitmap sourceBmp = null;
		Bitmap resultBmp = null;
    	try {
			File f = new File(path);
			
			if (!f.exists()) {
				Debug.i("[ImageUtil] getResizedImage; File not exists"); 
				return null;
			}
			//Decode image size
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, options);

			//Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while(options.outWidth / scale / 2 >= size && options.outHeight / scale / 2 >= size){
			    scale *= 2;
			}

			//Decode with inSampleSize
			options = new BitmapFactory.Options();
			options.inSampleSize = scale;
			sourceBmp = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
			
			if (sourceBmp == null) {
				return null;
			}
			
			//resize
			int width = (options.outWidth < size) ? options.outWidth : size;
			int height = options.outHeight * width / options.outWidth;
			if (height > size)
			{
			    width = options.outWidth * size / options.outHeight;
			    height = size;
			}
			
			resultBmp = Bitmap.createScaledBitmap(sourceBmp, width, height, true);
			return resultBmp;
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
			
		} finally{
			if (sourceBmp != null) {
				if (sourceBmp != resultBmp) {
					sourceBmp.recycle();
				}
				
				sourceBmp = null;
			}
		}

    	return null;
	}
	
	public static byte[] convertToByteArray(Bitmap bmp){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
		byte[] byteArray = stream.toByteArray();
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return byteArray;
	}
	
	public static String convertToBase64(Bitmap bmp){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
		byte[] byteArray = stream.toByteArray();
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Base64.encodeToString(byteArray, Base64.DEFAULT);
	}
}
