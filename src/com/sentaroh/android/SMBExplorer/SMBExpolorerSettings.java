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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

public class SMBExpolorerSettings extends PreferenceActivity {
	private Context mContext=null;
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		mContext=getApplicationContext();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		PreferenceManager pm=getPreferenceManager();
		setRootPrivelegeCBListener(pm, mContext);
		
		initSettingParms(prefs,getString(R.string.settings_exit_clean));
		initSettingParms(prefs,getString(R.string.settings_debug_level));
		initSettingParms(prefs,getString(R.string.settings_msl_scan));
		initSettingParms(prefs,getString(R.string.settings_use_root_privilege));
		
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

	@SuppressWarnings("deprecation")
	@Override  
	protected void onResume() {  
	    super.onResume();  
	    getPreferenceScreen().getSharedPreferences()
	    	.registerOnSharedPreferenceChangeListener(listener);  
	}  
	   
	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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
    	} else if (key.equals(getString(R.string.settings_use_root_privilege))) {
    		isChecked=true;
    	} else if (key.equals(getString(R.string.settings_exit_clean))) {
    		isChecked=true;
    		if (prefs.getBoolean(key, true)) {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_ena));
    		} else {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_dis));
    		}
    	}
		return isChecked;
	};
	
	private void setRootPrivelegeCBListener(PreferenceManager pm, Context c) {
		CheckBoxPreference cbp=(CheckBoxPreference) pm.findPreference(c.getString(R.string.settings_use_root_privilege));
		cbp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {  
	          @Override  
	          public boolean onPreferenceChange( Preference preference, Object newValue) {
	        	  boolean n_v=(Boolean)newValue;
	        	  boolean result=false;
	        	  if (n_v) {
	        		  result=isSuperUserAvailable();
	        		  if (!result)
	        			  Toast.makeText(mContext, 
        					  mContext.getString(R.string.settings_use_root_privilege_summary_not_granted), Toast.LENGTH_LONG).show();
		          } else {
		        	  result=true;
		          }
		          return result;  
	          }      
		});
	};
	
	public static boolean isSuperUserAvailable() {
		  Process p=null;
		  String out_msg="";
		  boolean result=false;
		  try {
			  p = Runtime.getRuntime().exec("su");//command);
			  DataOutputStream cmd_in=new DataOutputStream(p.getOutputStream());
			  cmd_in.writeBytes("id"+"\n");
			  cmd_in.flush();
			  cmd_in.writeBytes("exit\n");
			  cmd_in.flush();
			  BufferedReader std_out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			  BufferedReader std_err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			  String line;
			  while ((line = std_out.readLine()) != null) {
		            out_msg += line + "\n";
			  }
//			  Log.v("","ret="+out_msg);
			  cmd_in.close();
			  std_out.close();
			  std_err.close();
			  p.waitFor();
			  p.destroy();
			  p=null;
			  if (out_msg.equals("")) {
				  //SU not granted
				  result=false;
			  } else {
				  result=true;
			  }
		  } catch (IOException e) {
			  if (p!=null) p.destroy();
			  e.printStackTrace();
		  } catch (InterruptedException e) {
			  if (p!=null) p.destroy();
			  e.printStackTrace();
		  }
		  return result;
	};
	
	@SuppressWarnings("deprecation")
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
	
	@SuppressWarnings("deprecation")
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
	
	@SuppressWarnings("deprecation")
	private boolean checkSmbSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
		if (key.equals(getString(R.string.settings_smb_perform_class))) {
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
    	} else if (key.equals(getString(R.string.settings_smb_log_level))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("0");
        		prefs.edit().putString(key, "0").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	} else if (key.equals(getString(R.string.settings_smb_rcv_buf_size))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("66576");
        		prefs.edit().putString(key, "66576").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	} else if (key.equals(getString(R.string.settings_smb_snd_buf_size))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("66576");
        		prefs.edit().putString(key, "66576").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	} else if (key.equals(getString(R.string.settings_smb_listSize))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("None");
        		prefs.edit().putString(key, "").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	} else if (key.equals(getString(R.string.settings_smb_maxBuffers))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("100");
        		prefs.edit().putString(key, "100").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	} else if (key.equals(getString(R.string.settings_smb_tcp_nodelay))) {
    		isChecked=true;
    		if (prefs.getString(key, "false").equals("false")) {
        		findPreference(key).setSummary("false");
        		prefs.edit().putString(key, "false").commit();
        	} else {
        		findPreference(key).setSummary("true");
        		prefs.edit().putString(key, "true").commit();
        	}
    	} else if (key.equals(getString(R.string.settings_io_buffers))) {
    		isChecked=true;
    		if (prefs.getString(key, "").equals("")) {
        		findPreference(key).setSummary("8");
        		prefs.edit().putString(key, "8").commit();
        	} else {
        		findPreference(key).setSummary(prefs.getString(key, ""));
        	}
    	}
    	return isChecked;
	};
	
	@SuppressWarnings("deprecation")
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
