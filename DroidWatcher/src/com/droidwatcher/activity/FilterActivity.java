package com.droidwatcher.activity;

import java.util.ArrayList;

import com.droidwatcher.R;
import com.droidwatcher.SettingsManager;
import com.droidwatcher.modules.FilterModule;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class FilterActivity extends Activity {
	private SettingsManager settings;
	
	@Override
	protected void onPause() {
		super.onPause();
		
		FilterModule.reset();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter);
		
		settings = new SettingsManager(this);
		setupButtons();
		renderList();
	}
	
	private void renderList(){
		String fList = settings.filterList();
		String[] arr = {};
		if (fList.length() > 0){
			arr = fList.split(",");
		}
		ArrayList<String> list = new ArrayList<String>();
		for (Integer i = 0; i < arr.length; i++){
			list.add(arr[i]);
		}
		ListView lv = (ListView) findViewById(R.id.number_list);
		final NumberListAdapter adapter = new NumberListAdapter(this, list);
		lv.setAdapter(adapter);
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				String num = (String) adapter.getItem(position);
				settings.filterDel(num);
				renderList();
				return true;
			}
		});
	}
	
	private class NumberListAdapter extends BaseAdapter{
		LayoutInflater lInflater;
		ArrayList<String> objects;
		
		public NumberListAdapter(Context context, ArrayList<String> list){
			objects = list;
			lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public int getCount() {
			return objects.size();
		}

		public Object getItem(int position) {
			return objects.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = lInflater.inflate(R.layout.filter_item, parent, false);
			}
			
			String number = (String) getItem(position);
			((TextView) view.findViewById(R.id.item_number)).setText(number);
			
			return view;
		}
		
	}
	
	private void setupButtons(){
		CheckBox cb = (CheckBox) findViewById(R.id.usefilterCB);
		cb.setChecked(settings.isFilterEnabled());
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				settings.useFilter(isChecked);
			}
		});
		
		RadioGroup rg = (RadioGroup) findViewById(R.id.filterType);
		String type = settings.filterType();
		rg.clearCheck();
		if (type.equals("0")){
			((RadioButton) findViewById(R.id.type0)).setChecked(true);
		}
		else{
			((RadioButton) findViewById(R.id.type1)).setChecked(true);
		}
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.type0:
					settings.filterType("0");
					break;
				case R.id.type1:
					settings.filterType("1");
					break;
				default:
					break;
				}
				
			}
		});
		
		Button add = (Button) findViewById(R.id.addButton);
		add.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String num = ((EditText) findViewById(R.id.editNum)).getText().toString();
//				if (num.length() != 10){
//					Toast.makeText(FilterActivity.this, "Необходимо ввести ДЕСЯТИЗНАЧНЫЙ номер телефона без кода страны. Например: 9031234567", Toast.LENGTH_LONG).show();
//					return;
//				}
				
				settings.filterAdd(num);
				renderList();
			}
		});
	}
}
