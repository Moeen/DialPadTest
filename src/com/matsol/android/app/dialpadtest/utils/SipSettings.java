package com.matsol.android.app.dialpadtest.utils;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.matsol.android.app.dialpadtest.R;

/**
 * 
 * Setting class to invoke preference activity in application
 * 
 * @author MatSol
 * 
 */
public class SipSettings extends SherlockPreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// adding preference xml here
		addPreferencesFromResource(R.xml.preferences);
	}
}
