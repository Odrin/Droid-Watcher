package com.droidwatcher.variables;

import java.util.ArrayList;
import java.util.Iterator;

import com.droidwatcher.lib.IMessageBody;

public class DBResult {
	private ArrayList<Long> mIdList;
	private ArrayList<IMessageBody> mBodyList;
	
	private Iterator<Long> mIdIterator;
	private Iterator<IMessageBody> mBodyIterator;
	
	public DBResult(int capacity){
		mBodyList = new ArrayList<IMessageBody>(capacity);
		mIdList = new ArrayList<Long>(capacity);
	}
	
	public void add(Long id, IMessageBody message){
		mIdList.add(id);
		mBodyList.add(message);
	}
	
	public Boolean hasElements(){
		if (mIdIterator == null) {
			mIdIterator = mIdList.iterator();
			mBodyIterator = mBodyList.iterator();
		}
		
		return mIdIterator.hasNext();
	}
	
	public void getElements(int count, ArrayList<Long> idList, ArrayList<IMessageBody> bodyList){
		if (mIdIterator == null) {
			mIdIterator = mIdList.iterator();
			mBodyIterator = mBodyList.iterator();
		}
		
		while (count > 0 && mIdIterator.hasNext()) {
			idList.add(mIdIterator.next());
			bodyList.add(mBodyIterator.next());
			count--;
		}
	}
	
	public ArrayList<Long> getIdList(){
		return mIdList;
	}
	
	public ArrayList<IMessageBody> getBodyList(){
		return mBodyList;
	}
}
