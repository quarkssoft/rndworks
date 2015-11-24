package com.imagecomp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	
	EditText phoneNumber;
	Button submit;
	
	  
	   SharedPreferences sharedpreferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		phoneNumber = (EditText) findViewById(R.id.phoneNumber);
		submit =(Button) findViewById(R.id.submit);
		 sharedpreferences = getSharedPreferences(getResources().getString(R.string.shared_preference_name), Context.MODE_PRIVATE);
		 String sharedPhoneNumber = sharedpreferences.getString(getResources().getString(R.string.shr_phoneNumber), "");
		 if (sharedPhoneNumber!=null && !sharedPhoneNumber.equals("")) {
			
		phoneNumber.setText(sharedPhoneNumber);	
		 }
		submit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				SharedPreferences.Editor editor = sharedpreferences.edit();
	            editor.putString(getResources().getString(R.string.shr_phoneNumber), phoneNumber.getText().toString());
	            editor.commit();	
	            Toast.makeText(getApplicationContext(), getResources().getString(R.string.thanks_sms_register), Toast.LENGTH_SHORT).show();
			}
		});
		
		startService(new Intent(getApplicationContext(), BService.class));
	}

	
}