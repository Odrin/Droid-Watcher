package com.droidwatcher.lib;

public enum MessageType {
	SMS,
	CALL,
	GPS,
	ERROR,
	CONNECT,
	RECORD,
	PHOTO,
	CONFIG,
	CONTACT,
	APPLIST,
	SETTINGS_SEND,
	SETTINGS_GET,
	VK,
	WA,
	VB,
	BROWSER,
	
	LOGIN,
	REGISTER,
	/** Get imei and name lists */
	PHONE_LIST,
	RENAME,
	DISABLE,
	
	/** Get call list */
	CALL_LIST,
	/** Get sms list */
	SMS_LIST,
	/** Get gps list */
	GPS_LIST,
	/** Delete all specified logs */
	CLEAR,
	
	/** GCM registration id */
	GCM_REG,
	/** GCM ping command */
	GCM_PING_REQUEST,
	GCM_PING_RESPONSE,
	GCM_PING_CHECK,
	
	ONLINE_LAST_POSITION,
	ONLINE_CHECK_POSITION,
	ONLINE_POSITION,
	ONLINE_REQUEST_UPDATE,
	
	GCM_COMMAND_RESPONSE,
	
	DEVICE_INFO,
	
	FSERROR,
	DIR;
}