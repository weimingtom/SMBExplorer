package com.sentaroh.android.SMBExplorer;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SMBExpolorerSettings extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		initSettingParms(prefs,getString(R.string.settings_exit_clean));
		initSettingParms(prefs,getString(R.string.settings_debug_level));
		initSettingParms(prefs,getString(R.string.settings_msl_scan));
		initSettingParms(prefs,getString(R.string.settings_default_user));
		initSettingParms(prefs,getString(R.string.settings_default_pass));
		initSettingParms(prefs,getString(R.string.settings_default_addr));
		
    	initSettingParms(prefs,getString(R.string.settings_smb_perform_class));
    	//Debug only
    	initSettingParms(prefs,getString(R.string.settings_smb_log_level));
    	initSettingParms(prefs,getString(R.string.settings_smb_rcv_buf_size));
    	initSettingParms(prefs,getString(R.string.settings_smb_snd_buf_size));
    	initSettingParms(prefs,getString(R.string.settings_smb_listSize));
    	initSettingParms(prefs,getString(R.string.settings_smb_maxBuffers));
		initSettingParms(prefs,getString(R.string.settings_smb_tcp_nodelay));
    	initSettingParms(prefs,getString(R.string.settings_io_buffers));
	}

	@Override  
	protected void onResume() {  
	    super.onResume();  
	    getPreferenceScreen().getSharedPreferences()
	    	.registerOnSharedPreferenceChangeListener(listener);  
	}  
	   
	@Override  
	protected void onPause() {  
	    super.onPause();  
	    getPreferenceScreen().getSharedPreferences()
	    	.unregisterOnSharedPreferenceChangeListener(listener);  
	}
	
	private void initSettingParms(SharedPreferences prefs, String key) {
		if (!checkBasicSettings(prefs, key)) 
	    	if (!checkLogSettings(prefs, key))
	    	if (!checkMediaScannerSettings(prefs, key))
		    if (!checkSmbSettings(prefs, key))
		    	checkOtherSettings(prefs, key);
	}

	private SharedPreferences.OnSharedPreferenceChangeListener listener =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences prefs, 
		    		String key) {
		    	if (!checkBasicSettings(prefs, key)) 
		    	if (!checkSmbSettings(prefs, key))		    		
			    	checkOtherSettings(prefs, key);
		    }
	};

	private boolean checkBasicSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
		if (key.equals(getString(R.string.settings_msl_scan))) {
    		isChecked=true;
    		if (prefs.getBoolean(key, false)) {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_msl_scan_summary_ena));
    		} else {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_msl_scan_summary_dis));
    		}
    	} else if (key.equals(getString(R.string.settings_exit_clean))) {
    		isChecked=true;
    		if (prefs.getBoolean(key, false)) {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_ena));
    		} else {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_dis));
    		}
    	}

		return isChecked;
	};
	private boolean checkLogSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
    	if (key.equals(getString(R.string.settings_debug_level))) {
    		isChecked=true;
        	findPreference(getString(R.string.settings_debug_level).toString())
    			.setSummary("Current setting="+
    					prefs.getString(getString(R.string.settings_debug_level), "0"));
    	}
    	return isChecked;
	};
	private boolean checkMediaScannerSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
    	if (key.equals(getString(R.string.settings_msl_scan))) {
    		isChecked=true;
    		if (prefs.getBoolean("settings_msl_scan", false)) 
    			findPreference("settings_msl_scan")
    				.setSummary(getString(R.string.settings_msl_scan_summary_ena));
    		else findPreference("settings_msl_scan")
    			.setSummary(getString(R.string.settings_msl_scan_summary_dis));
    	}
    	return isChecked;
	};

	private boolean checkSmbSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
		if (key.equals(getString(R.string.settings_default_user))) {
			isChecked=true;
	    	findPreference(getString(R.string.settings_default_user).toString())
				.setSummary("Current setting="+
						prefs.getString(getString(R.string.settings_default_user), "0"));
		} else if (key.equals(getString(R.string.settings_default_pass))) {
			isChecked=true;
	    	findPreference(getString(R.string.settings_default_pass).toString())
				.setSummary("Current setting=--------");
		} else if (key.equals(getString(R.string.settings_default_addr))) {
			isChecked=true;
	    	findPreference(getString(R.string.settings_default_addr).toString())
				.setSummary("Current setting="+
						prefs.getString(getString(R.string.settings_default_addr), "0"));
    	} else if (key.equals(getString(R.string.settings_smb_perform_class))) {
			isChecked=true;
        	if (prefs.getString(key, "").equals("0")) {
        		findPreference(key)
    				.setSummary(getString(R.string.settings_smb_perform_class_0));
        	} else if (prefs.getString(key, "").equals("1")) {
        		findPreference(key)
        		.setSummary(getString(R.string.settings_smb_perform_class_1));
        	} else if (prefs.getString(key, "").equals("2")) {
        		findPreference(key)
        		.setSummary(getString(R.string.settings_smb_perform_class_2));
        	} else if (prefs.getString(key, "").equals("3")) {
        		findPreference(key)
        		.setSummary(getString(R.string.settings_smb_perform_class_3));
        	} else {
        		findPreference(key)
				.setSummary(getString(R.string.settings_smb_perform_class_0));
        	}
        	if (prefs.getString(key, "").equals("3")) 
       		findPreference("settings_smb_tunning").setEnabled(true);
        	else findPreference("settings_smb_tunning").setEnabled(false);
    	}
    	return isChecked;
	};
	private boolean checkOtherSettings(SharedPreferences prefs, String key) {
		boolean isChecked = true;
    	Preference fpf=findPreference(key);
    	if (fpf!=null) {
    		fpf.setSummary("Current setting="+prefs.getString(key, "0"));
    	} else {
    		Log.v("SMBSyncSettings","key not found. key="+key);
    	}
    	return isChecked;
	};
}
