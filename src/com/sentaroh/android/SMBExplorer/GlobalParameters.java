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

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class GlobalParameters extends Application{
	
	public int debugLevel=0;
	public String SMBExplorerRootDir="/";
	
	public int settingsDebugLevel=1;

	public boolean settingsExitClean=false;
	public boolean settingsMslScan=false;
	
	public boolean fileIoWifiLockRequired=false;
	public boolean fileIoWakeLockRequired=true;
	
	public String smbUser="", smbPass="";
	
	public String jcifs_option_log_level="0", jcifs_option_rcv_buf_size="16644",
			jcifs_option_snd_buf_size="16644",jcifs_option_listSize="",
			jcifs_option_maxBuffers="", jcifs_option_tcp_nodelay="false",
			jcifs_option_performance_class="0", jcifs_option_iobuff="4";

	public boolean settingsUseRootPrivilege=true;
	public boolean useSetLastModifiedByTouchCommand=false;
	public Process mSuCmdProcess=null;
	public boolean useSetLastModifiedByAppSpecificDir=false;

	public TextView progressMsgView=null;
	public Button progressCancelBtn=null;
	public TextView dialogMsgView=null;
	public Button dialogCloseBtn=null;
	
	public GlobalParameters() {};
	
	@Override
	public void  onCreate() {
		super.onCreate();
		Log.v("SMBExplorerGP","onCreate entered");
		
		loadSettingsParm();
	};
	
	@Override
	public void onLowMemory() {
		Log.v("SMBExplorerGP","onLowMemory entered");
	};
	
	public void loadSettingsParm() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String dl = prefs.getString(getString(R.string.settings_debug_level), "0");
		if (dl.compareTo("0")>=0 && dl.compareTo("9")<=0) debugLevel=Integer.parseInt(dl);
		settingsExitClean=prefs.getBoolean(getString(R.string.settings_exit_clean), true);
		settingsMslScan=prefs.getBoolean(getString(R.string.settings_msl_scan), false);
		settingsUseRootPrivilege=prefs.getBoolean(getString(R.string.settings_use_root_privilege), false);

		jcifs_option_performance_class=prefs.getString(getString(R.string.settings_smb_perform_class), "");
		if (jcifs_option_performance_class.equals("0") || jcifs_option_performance_class.equals("")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="16644";
			jcifs_option_snd_buf_size="16644";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="";
			jcifs_option_tcp_nodelay="false";
			jcifs_option_iobuff="4";
		} else if (jcifs_option_performance_class.equals("1")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="33288";
			jcifs_option_snd_buf_size="33288";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="100";
			jcifs_option_tcp_nodelay="false";
			jcifs_option_iobuff="4";
		} else if (jcifs_option_performance_class.equals("2")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="66576";
			jcifs_option_snd_buf_size="66576";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="100";
			jcifs_option_tcp_nodelay="true";
			jcifs_option_iobuff="8";
		} else {
			jcifs_option_log_level=prefs.getString(getString(R.string.settings_smb_log_level), "0");
			if (jcifs_option_log_level.length()==0) jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size=
					prefs.getString(getString(R.string.settings_smb_rcv_buf_size),"66576");
			jcifs_option_snd_buf_size=
					prefs.getString(getString(R.string.settings_smb_snd_buf_size),"66576");
			jcifs_option_listSize=
					prefs.getString(getString(R.string.settings_smb_listSize), "");
			jcifs_option_maxBuffers=
					prefs.getString(getString(R.string.settings_smb_maxBuffers), "100");
			jcifs_option_tcp_nodelay=
					prefs.getString(getString(R.string.settings_smb_tcp_nodelay),"false");
			jcifs_option_iobuff=
					prefs.getString(getString(R.string.settings_io_buffers), "8");
		}
//		jcifs_option_rcv_buf_size="60416";//"60416";
//		jcifs_option_snd_buf_size="60416";
//		jcifs_option_listSize="";
//		jcifs_option_maxBuffers="100";
//		jcifs_option_tcp_nodelay="true";
//		jcifs_option_iobuff="64";

	};

}


