package com.droidwatcher.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.acra.ACRA;

import com.droidwatcher.Debug;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
	
	public static boolean isBlack(byte[] bytes){
		Bitmap bmp = null;
		
		try {
			bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			return isBlack(bmp);
			
		} finally {
			if (bmp != null) {
				bmp.recycle();
			}
		}
	}
	
	/**
	 * Check ~30% of pixels and return "true" if all is black
	 */
	public static boolean isBlack(Bitmap bmp){
		try {
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			
			int offsetX = Math.round(width * 0.3f);
			int offsetY = Math.round(height * 0.3f);
			
			int pixel;
			
			for (int x = 0; x < width; x += offsetX) {
				for (int y = 0; y < height; y += offsetY) {
					pixel = bmp.getPixel(x, y);
					
					if (Color.red(pixel) > 20 && Color.green(pixel) > 20 && Color.blue(pixel) > 20) {
						return false;
					}
					
//					if (pixel != Color.BLACK) {
//						return false;
//					}
				}
			}
			
			return true;
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
			return false;
		}
	}
}
