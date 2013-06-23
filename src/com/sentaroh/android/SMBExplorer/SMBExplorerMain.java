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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jcifs.UniAddress;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import static com.sentaroh.android.SMBExplorer.Constants.*;

import com.sentaroh.android.Utilities.*;
import com.sentaroh.android.Utilities.CustomContextMenuItem.CustomContextMenuOnClickListener;

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class SMBExplorerMain extends FragmentActivity {

	private final static String DEBUG_TAG = "SMBExplorer";
	private int debugLevel=0;
	private String SMBExplorerRootDir="/";

	private boolean enableKill = false;

	private String remoteUrl = "", localUrl = "";

//	private static String currentSelectedProfileType="P";

	private boolean result_createFileListView=false;
	
	private MsglistAdapter messageListAdapter =null;
	private ListView messageListView=null;
//	private int posMessageView=0;

	private ProfilelistAdapter profileAdapter=null;
	private ListView profileListView=null;
	private boolean error_CreateProfileListResult=false;
//	private int posProfileView=0;
	
	private TreeFilelistAdapter localFileListAdapter=null;
	private TreeFilelistAdapter remoteFileListAdapter=null;
	private ListView localFileListView=null;
	private ListView remoteFileListView=null;
	private String currentTabName="@P";
	private Spinner localFileListDirBtn=null;
	private Spinner remoteFileListDirBtn=null;
//	private int posLocalFileListView=0;
//	private int posRemoteFileListView=0;
	
	private String defaultSettingUsername,defaultSettingPassword,defaultSettingAddr;
	private boolean defaultSettingExitClean;
	private ArrayList<FileIoLinkParm> fileioLinkParm = 
			new ArrayList<FileIoLinkParm>();
	
	private TabHost tabHost;
	
	@SuppressWarnings("unused")
	private String packageVersionName ="xxx";

	private int restartStatus=0;
	
	private Context currentContext;
	
	private String smbUser, smbPass;
	
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	private CustomContextMenu ccMenu = null;
	
	private String refreshUrl;//,refreshDir;
	
	private Activity currentActivity;
	
	private CommonDialog commonDlg;
	
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
	  super.onSaveInstanceState(outState);  
	  sendDebugLogMsg(1, "I", "onSaveInstanceState entered.");

	  outState.putInt("debugLevel", debugLevel);
	  outState.putString("remoteUrl", remoteUrl);
	  outState.putString("localUrl", localUrl);
//	  outState.putString("currentSelectedProfileType", currentSelectedProfileType);
	  outState.putString("currentTabName", currentTabName);
	  outState.putString("smbUser", smbUser);
	  outState.putString("smbPass", smbPass);
	};  
	  
	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState) {  
	  super.onRestoreInstanceState(savedInstanceState);
	  sendDebugLogMsg(1, "I", "onRestoreInstanceState entered.");
	  debugLevel=savedInstanceState.getInt("debugLevel");
	  remoteUrl=savedInstanceState.getString("remoteUrl");
	  localUrl=savedInstanceState.getString("localUrl");
//	  currentSelectedProfileType=savedInstanceState.getString("currentSelectedProfileType");
	  currentTabName=savedInstanceState.getString("currentTabName");
	  smbUser=savedInstanceState.getString("smbUser");
	  smbPass=savedInstanceState.getString("smbPass");
	  restartStatus=2;
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		applySettingParms();
		currentContext=this;
		currentActivity=this;
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		if (ccMenu ==null) ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());

		commonDlg=new CommonDialog(currentContext, getSupportFragmentManager());

//		localUrl = SMBEXPLORER_ROOT_DIR;
		SMBExplorerRootDir=localUrl=LocalMountPoint.getExternalStorageDir();
		createTabAndView() ;

		profileListView = (ListView) findViewById(R.id.explorer_profile_tab_listview);
		messageListView = (ListView) findViewById(R.id.explorer_message_tab_listview);
		if (messageListAdapter==null) {
			List<MsglistItem> tml = new ArrayList<MsglistItem>(); 
			messageListAdapter = 
				new MsglistAdapter(this,this,R.layout.msg_list_view_item,tml);
		}
		messageListView.setAdapter(messageListAdapter);
		messageListAdapter.setNotifyOnChange(true);

		sendDebugLogMsg(1, "I", "onCreate entered");
		
		getApplVersionName();
		
		enableKill = false;
	};

	@Override
	protected void onStart() {
		super.onStart();
		sendDebugLogMsg(1, "I", "onStart entered");
		
	};

	@Override
	protected void onRestart() {
		super.onRestart();
		sendDebugLogMsg(1, "I", "onRestart entered");
	};

	@Override
	protected void onResume() {
		super.onResume();
		sendDebugLogMsg(1, "I","onResume entered"+
				" restartStatus="+restartStatus);
		
		if (restartStatus==0) {
			profileAdapter = createProfileList(false,"");
			localFileListView=(ListView)findViewById(R.id.explorer_filelist_local_tab_listview);
//			tabHost.getTabWidget().getChildTabViewAt(2).setEnabled(true);
			loadLocalFilelist(localUrl);

			remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
			remoteFileListDirBtn=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
//			remoteFileListDirBtn.setText("Profile not selected");
//			tabHost.getTabWidget().getChildTabViewAt(3).setEnabled(false);
		} else if (restartStatus==1) {

		} else if (restartStatus==2) {
			profileAdapter = createProfileList(false,"");
			profileListView.setAdapter(profileAdapter);

			restoreTaskData();
			
			setPasteItemList();
			if (currentTabName.equals("@M")) tabHost.setCurrentTab(1);
			else if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tabHost.setCurrentTab(2);
			else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) tabHost.setCurrentTab(3);
		}
		restartStatus=1;
		
		localFileListView.setFastScrollEnabled(true);
		remoteFileListView.setFastScrollEnabled(true);
		messageListView.setFastScrollEnabled(true);
		
		setLocalDirBtnListener();
		setRemoteDirBtnListener();
		
		setProfilelistItemClickListener();
		setProfilelistLongClickListener();
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
		setRemoteFilelistItemClickListener();
		setRemoteFilelistLongClickListener();
		setMsglistLongClickListener();
		
		refreshOptionMenu();
	};


	@Override
	protected void onPause() {
		super.onPause();
		sendDebugLogMsg(1, "I","onPause entered");
		sendDebugLogMsg(1, "I","onPause getChangingConfigurations="+getChangingConfigurations());
		saveTaskData();
	};

	@Override
	protected void onStop() {
		super.onStop();
		sendDebugLogMsg(1, "I","onStop entered");
		saveTaskData();
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sendDebugLogMsg(1, "I","onDestroy entered");
		if (enableKill) {
			deleteTaskData();
			if (defaultSettingExitClean) {
				System.gc();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		} else {
			saveTaskData();
		}
	};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    sendDebugLogMsg(1,"I","onConfigurationChanged Entered, "+
	    		"orientation="+newConfig.orientation);
	    
		setContentView(R.layout.main);

		int msgPos,msgPosTop=0,profPos,profPosTop=0,lclPos,lclPosTop=0,remPos=0,remPosTop=0;
		msgPos=messageListView.getFirstVisiblePosition();
		if (messageListView.getChildAt(0)!=null) msgPosTop=messageListView.getChildAt(0).getTop();
		profPos=profileListView.getFirstVisiblePosition();
		if (profileListView.getChildAt(0)!=null) profPosTop=profileListView.getChildAt(0).getTop();
		lclPos=localFileListView.getFirstVisiblePosition();
		if (localFileListView.getChildAt(0)!=null) lclPosTop=localFileListView.getChildAt(0).getTop();
		remPos=remoteFileListView.getFirstVisiblePosition();
		if (remoteFileListView.getChildAt(0)!=null) remPosTop=remoteFileListView.getChildAt(0).getTop();
		
		createTabAndView() ;

		profileListView = (ListView) findViewById(R.id.explorer_profile_tab_listview);
		messageListView = (ListView) findViewById(R.id.explorer_message_tab_listview);
		messageListView.setAdapter(messageListAdapter);
		messageListAdapter.setNotifyOnChange(true);
		messageListView.setAdapter(messageListAdapter);
		messageListView.setSelectionFromTop(msgPos,msgPosTop);
		messageListView.setFastScrollEnabled(true);

		profileListView.setAdapter(profileAdapter);
		profileListView.setSelectionFromTop(profPos,profPosTop);
		
		localFileListView.setAdapter(localFileListAdapter);
		localFileListView.setSelectionFromTop(lclPos,lclPosTop);
		localFileListView.setFastScrollEnabled(true);

		remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
		remoteFileListDirBtn=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
		remoteFileListView.setFastScrollEnabled(true);
		if (remoteUrl.equals("")) {
//				remoteFileListDirBtn.setText("Profile not selected");
//				tabHost.getTabWidget().getChildTabViewAt(3).setEnabled(false);
		} else {
			remoteFileListView.setAdapter(remoteFileListAdapter);
//				tabHost.getTabWidget().getChildTabViewAt(3).setEnabled(true);
		}
		remoteFileListView.setSelectionFromTop(remPos,remPosTop);
		setPasteItemList();
		
		if (currentTabName.equals("@M")) tabHost.setCurrentTab(1);
		else if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tabHost.setCurrentTab(2);
		else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) tabHost.setCurrentTab(3);
		
		setLocalDirBtnListener();
		setRemoteDirBtnListener();
		
		setProfilelistItemClickListener();
		setProfilelistLongClickListener();
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
		setRemoteFilelistItemClickListener();
		setRemoteFilelistLongClickListener();
		setMsglistLongClickListener();

		refreshOptionMenu();

	};
	
	private void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT>=11)
			currentActivity.invalidateOptionsMenu();
	};

	public void getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    packageVersionName=packageInfo.versionName;
		} catch (NameNotFoundException e) {
			sendDebugLogMsg(1, "I","SMBSync package can not be found");        
		}
	};
	
    private boolean isSameMountPoint(String f_fp, String t_fp) {
    	boolean result=false;
    	ArrayList<String> ml=LocalMountPoint.buildLocalMountPointList();
    	if (LocalMountPoint.isExternal2MountPioint(f_fp) || 
    			LocalMountPoint.isExternal2MountPioint(t_fp)) {
        	if (LocalMountPoint.isExternal2MountPioint(f_fp)==
        			LocalMountPoint.isExternal2MountPioint(t_fp))  
        		result=true;
    	} else {
    		int i,j;
    		for (i=ml.size()-1;i>=0;i--) {
    			if (f_fp.startsWith(ml.get(i))) break;
    		}
    		for (j=ml.size()-1;j>=0;j--) {
    			if (t_fp.startsWith(ml.get(j))) break;
    		}
    		if (i==j) result=true;
    	}
    	sendDebugLogMsg(1, "I","isSameMountPoint "+"result="+result+", f_fp="+f_fp+",t_fp="+t_fp);
    	return result;
    };

	private String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = 
	        		NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = 
	            		intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();) {
	            	InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && !(inetAddress.toString().indexOf(":")>=0)) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(DEBUG_TAG, ex.toString());
	    }
	    return null;
	};
	
	private void createTabAndView() {
		tabHost=(TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();
		
		View childview2 = new CustomTabContentView(this, "Profile");
		TabHost.TabSpec tabSpec1=tabHost.newTabSpec("@P")
				.setIndicator(childview2)
				.setContent(R.id.explorer_profile_tab);
		tabHost.addTab(tabSpec1);
		
		
		View childview3 = new CustomTabContentView(this, "Messages");
		TabHost.TabSpec tabSpec2=tabHost.newTabSpec("@M")
				.setIndicator(childview3)
				.setContent(R.id.explorer_message_tab);
		tabHost.addTab(tabSpec2);
		
		View childview4 = new CustomTabContentView(this, SMBEXPLORER_TAB_LOCAL);
		TabHost.TabSpec tabSpec4=tabHost.newTabSpec(SMBEXPLORER_TAB_LOCAL)
				.setIndicator(childview4)
				.setContent(R.id.explorer_filelist_local_tab);
		tabHost.addTab(tabSpec4);
		
		View childview5 = new CustomTabContentView(this, SMBEXPLORER_TAB_REMOTE);
		TabHost.TabSpec tabSpec5=tabHost.newTabSpec(SMBEXPLORER_TAB_REMOTE)
				.setIndicator(childview5)
				.setContent(R.id.explorer_filelist_remote_tab);
		tabHost.addTab(tabSpec5);
		
//		tabHost.setCurrentTab(0);
		tabHost.setOnTabChangedListener(new OnTabChange());
		
		localFileListDirBtn=(Spinner)findViewById(R.id.explorer_filelist_local_tab_dir);
		remoteFileListDirBtn=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
		
		localFileListView=(ListView)findViewById(R.id.explorer_filelist_local_tab_listview);
		remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
	};

	class OnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			sendDebugLogMsg(1, "I","onTabchanged entered. tab="+tabId);
			currentTabName=tabId;
		};
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		sendDebugLogMsg(1, "I","onCreateOptionsMenu entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;
	};

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	sendDebugLogMsg(1, "I","onPrepareOptionsMenu entered");
    	super.onPrepareOptionsMenu(menu);
    	String t_url="";
    	if (currentTabName.startsWith("@")) {
//        	menu.findItem(R.id.menu_top_paste).setEnabled(false);
//        	menu.findItem(R.id.menu_top_paste).setVisible(false);
    	} else {
        	if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) t_url=localUrl+"/";
        	else  t_url=remoteUrl+"/";
        	if (isValidPasteDestination(t_url)) {
//        		menu.findItem(R.id.menu_top_paste).setEnabled(true);
//        		menu.findItem(R.id.menu_top_paste).setVisible(true);
        	} else {
//        		menu.findItem(R.id.menu_top_paste).setEnabled(false);
//        		menu.findItem(R.id.menu_top_paste).setVisible(false);
        	}
    		
    	}
        return true;
    };
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		sendDebugLogMsg(1, "I","onOptionsItemSelected entered");
		switch (item.getItemId()) {
			case R.id.menu_top_refresh:
				refreshFilelistView();
				return true;
//			case R.id.menu_top_paste:
//				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
//					pasteItem(localFileListAdapter,localUrl);
//				else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
//					pasteItem(remoteFileListAdapter,remoteUrl);
//				return true;
			case R.id.menu_top_export:
				exportProfileDlg(SMBExplorerRootDir, SMBEXPLORER_PROFILE_NAME);
				return true;
			case R.id.menu_top_import:
				importProfileDlg(SMBExplorerRootDir, SMBEXPLORER_PROFILE_NAME);
				return true;
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;				
			case R.id.quit:
				confirmTerminateApplication();
				return true;
		}
		return false;
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (!tabHost.getCurrentTabTag().startsWith("@")) {
				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
					if (closeLastLevel(localFileListAdapter)==0)
						confirmTerminateApplication();
//						tabHost.setCurrentTab(0);
				} else {
					if (closeLastLevel(remoteFileListAdapter)==0)
						confirmTerminateApplication();
//						tabHost.setCurrentTab(0);
				}
			} else if (tabHost.getCurrentTabTag().equals("@M")) {
				confirmTerminateApplication();
				return false;
			} else{
				confirmTerminateApplication();
			}
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};

	private int closeLastLevel(TreeFilelistAdapter fa) {
		int maxlevel=0;
		if (fa==null || fa.getDataItemCount()==0) return 0;
		for (int i=0;i<fa.getCount();i++) {
			if (maxlevel<fa.getDataItem(fa.getItem(i)).getListLevel()) 
				maxlevel=fa.getDataItem(fa.getItem(i)).getListLevel();
		}
		if (maxlevel==0) return 0;
		else {//戻り処理
			int nc=fa.getCount();
			for (int i=nc-1;i>=0;i--) {
				if (fa.getDataItem(fa.getItem(i)).getListLevel()==maxlevel) {
					TreeFilelistItem tfai=fa.getDataItem(fa.getItem(i));
					tfai.setHideListItem(true);
					fa.replaceDataItem(fa.getItem(i),tfai);
				} else {
					if (fa.getDataItem(fa.getItem(i)).getListLevel()==maxlevel-1) {
						TreeFilelistItem tfi =fa.getDataItem(fa.getItem(i));
						tfi.setChildListExpanded(false);
						fa.replaceDataItem(fa.getItem(i),tfi);
					}
				}
			}
		}
		fa.createShowList();
		return maxlevel;
	};

	private void confirmTerminateApplication() {
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				terminateApplication();
			}
	
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}
		});
		commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_terminate_confirm),"",ne);
		return;
	
	}

	private void terminateApplication() {
			enableKill = true; // exit cleanly
	//		moveTaskToBack(true);
			finish();
		}

	private void applySettingParms() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String dl = 
				prefs.getString(getString(R.string.settings_debug_level), "0");
		if (dl.compareTo("0")>=0 && dl.compareTo("9")<=0) 
			debugLevel=Integer.parseInt(dl);
		
		defaultSettingUsername=
				prefs.getString(getString(R.string.settings_default_user), "");
		defaultSettingPassword=
				prefs.getString(getString(R.string.settings_default_pass), "");
		defaultSettingAddr=
				prefs.getString(getString(R.string.settings_default_addr), "");
		if(defaultSettingAddr.equals("")) defaultSettingAddr=getLocalIpAddress();
		
		defaultSettingExitClean=
				prefs.getBoolean(getString(R.string.settings_exit_clean), false);
	}
	
	private void invokeSettingsActivity() {
//		setFixedOrientation(true);
		Intent intent = new Intent(this, SMBExpolorerSettings.class);
		startActivityForResult(intent,0);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		applySettingParms();
//		setFixedOrientation(false);
	};
	
	private void refreshFilelistView() {
		setLocalDirBtnListener();
		setRemoteDirBtnListener();
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			int pos = localFileListView.getFirstVisiblePosition();
			int posTop = localFileListView.getChildAt(0).getTop();
			loadLocalFilelist(localUrl+"/");
			localFileListView.setSelectionFromTop(pos,posTop);
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			int pos = remoteFileListView.getFirstVisiblePosition();
			int posTop = remoteFileListView.getChildAt(0).getTop();
			loadRemoteFilelist(remoteUrl);
			remoteFileListView.setSelectionFromTop(pos,posTop);
		} else return; //file list not selected
	};
	
	private void loadLocalFilelist(String lurl) {
		tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL); 
		ArrayList<TreeFilelistItem> tfl = createLocalFileList(false,lurl);
		if (!result_createFileListView) return;

		localFileListAdapter=new TreeFilelistAdapter(this);
		localFileListAdapter.setDataList(tfl);
		localFileListView.setAdapter(localFileListAdapter);
		
//		currentSelectedProfileType="L";
		
		tabHost.getTabWidget().getChildTabViewAt(2).setEnabled(true);
		
		setFilelistCurrDir(localFileListDirBtn,lurl);
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
	};
	
	private void loadRemoteFilelist(String url) {
		NotifyEvent ne=new NotifyEvent(currentContext);
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				remoteFileListAdapter = (TreeFilelistAdapter)o[0];
				remoteFileListView.setAdapter(remoteFileListAdapter);
				setFilelistCurrDir(remoteFileListDirBtn,remoteUrl);
				setRemoteFilelistItemClickListener();
				setRemoteFilelistLongClickListener();
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				setFilelistCurrDir(remoteFileListDirBtn,
						"Fileist creation error occured");
			}
		});
		createRemoteFileList(url,ne);
	}
	
	private void setLocalDirBtnListener() {
        Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_local_tab_dir);
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.custom_simple_spinner_item);
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(this, R.layout.custom_simple_spinner_item);
        adapter.setTextColor(Color.BLACK);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt("ローカルの選択");
        spinner.setAdapter(adapter);

        int a_no=0;
        ArrayList<String>ml=LocalMountPoint.buildLocalMountPointList();
        for (int i=0;i<ml.size();i++) { 
				adapter.add(ml.get(i));
				if (ml.get(i).equals(localUrl))
			        spinner.setSelection(a_no);
				a_no++;
		}
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
				String turl=(String) spinner.getSelectedItem();
//				Log.v("","l="+localUrl+", t="+turl);
				if (turl.equals(localUrl)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
				else {
					loadLocalFilelist(turl);
					localUrl=turl;
				}
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
	};
	
	private void setRemoteDirBtnListener() {
        Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_remote_tab_dir);
        final CustomSpinnerAdapter spAdapter = new CustomSpinnerAdapter(this, R.layout.custom_simple_spinner_item);
        spAdapter.setTextColor(Color.BLACK);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt("リモートの選択");
        spinner.setAdapter(spAdapter);
		if (remoteUrl.equals("")) spAdapter.add("--- Not selected ---");
        // アイテムを追加します
		int a_no=0;
		for (int i=0;i<profileAdapter.getCount();i++) 
			if (profileAdapter.getItem(i).getType().equals("R") && 
					profileAdapter.getItem(i).getActive().equals("A")) {
				spAdapter.add(profileAdapter.getItem(i).getName());
				String surl="smb://" + profileAdapter.getItem(i).getAddr() +
						"/"+ profileAdapter.getItem(i).getShare() ;
//				Log.v("","surl="+surl+", rurl="+remoteUrl);
				if (surl.equals(remoteUrl))
			        spinner.setSelection(a_no);
				a_no++;
			}
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
                if (((String)spinner.getSelectedItem()).startsWith("---"))
                	return;
//				Log.v("","l="+localUrl+", t="+turl);
				ProfilelistItem pli=null;
				for (int i=0;i<profileAdapter.getCount();i++) {
					if (profileAdapter.getItem(i).getName()
							.equals((String)spinner.getSelectedItem())) {
						pli=profileAdapter.getItem(i);
					}
				}
				if (spAdapter.getItem(0).startsWith("---")) {
					long ns_id=spinner.getSelectedItemId()-1;
					spAdapter.remove(spAdapter.getItem(0));
					spinner.setSelection((int)ns_id);
				}
				String turl="smb://" + pli.getAddr() + "/"+ pli.getShare() ;
				if (turl.equals(remoteUrl)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
				else {	
					tabHost.getTabWidget().getChildTabViewAt(3).setEnabled(true);
//					currentSelectedProfileType = "R";
					tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
					setJcifsProperties(pli.getUser(), pli.getPass());
					remoteUrl = "smb://"+pli.getAddr()+"/"+pli.getShare() ;
					loadRemoteFilelist(remoteUrl);
				}
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
	};

	private void setProfilelistItemClickListener() {
		profileListView
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			ProfilelistItem item = profileAdapter.getItem(position);
			sendDebugLogMsg(1,"I","Profilelist item Clicked :" + item.getName());
			
			if (item.getActive().equals("A")) { // profile is active
				if (item.getType().equals("R")) {
					String turl="smb://" + item.getAddr() + "/"+ item.getShare() ;
					if (turl.equals(remoteUrl)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
					else {	
						tabHost.getTabWidget().getChildTabViewAt(3).setEnabled(true);
//						currentSelectedProfileType = "R";
						tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
						setJcifsProperties(item.getUser(), item.getPass());
						remoteUrl = "smb://"+item.getAddr()+"/"+item.getShare() ;
						loadRemoteFilelist(remoteUrl);
					}
				} else {
					String turl=item.getName();
//					Log.v("","l="+localUrl+", t="+turl);
					if (turl.equals(localUrl)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
					else {
						loadLocalFilelist(turl);
						localUrl=turl;
					}
				}
			} 
		}});
	};

	private void setProfilelistLongClickListener() {
		profileListView
		.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createProfileContextMenu(arg1, arg2);
				return true;
			}
		});
	};

	private void setLocalFilelistItemClickListener() {
		if (localFileListView==null) return;
		localFileListView
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int pos=localFileListAdapter.getItem(position);
				TreeFilelistItem item = localFileListAdapter.getDataItem(pos);
				sendDebugLogMsg(1,"I","Local filelist item clicked :" + item.getName());
				if (item.isDir()) {
					if (item.getSubDirItemCount()==0) return;
					if(item.isChildListExpanded()) {
						int lv_fpos=localFileListView.getFirstVisiblePosition();
						int lv_fposTop=localFileListView.getChildAt(0).getTop();
						localFileListAdapter.hideChildItem(item,pos);
						localFileListView.setSelectionFromTop(lv_fpos,lv_fposTop);
					} else {
						if (item.isSubDirLoaded()) 
							localFileListAdapter.reshowChildItem(item,pos);
						else {
							ArrayList<TreeFilelistItem> tfl=
								createLocalFileList(false, 
										item.getPath()+"/"+item.getName());
							if (!result_createFileListView) return;
							localFileListAdapter.addChildItem(item,tfl,pos);
							setFilelistCurrDir(localFileListDirBtn,localUrl);
						}
					}
				} else {
					startLocalFileViewerIntent(item);
				}
			}
		});
	};
	
	private void setLocalFilelistLongClickListener() {
		if (localFileListView==null) return;
		localFileListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				int idx = localFileListAdapter.getItem(arg2);
				createFilelistContextMenu(arg1, idx,localFileListAdapter);
				return true;
			}
		});
	};
	
	private void setRemoteFilelistItemClickListener() {
		if (remoteFileListView==null) return;
		remoteFileListView
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					final int position, long id) {
				final int pos=remoteFileListAdapter.getItem(position);
				final TreeFilelistItem item = remoteFileListAdapter.getDataItem(pos);
				sendDebugLogMsg(1,"I","Remote filelist item clicked :" + item.getName());
				if (item.isDir()) {
					if (item.getSubDirItemCount()==0) return;
					if(item.isChildListExpanded()) {
						remoteFileListAdapter.hideChildItem(item,pos);
					} else {
						if (item.isSubDirLoaded()) 
							remoteFileListAdapter.reshowChildItem(item,pos);
						else {
							NotifyEvent ne=new NotifyEvent(currentContext);
							ne.setListener(new ListenerInterface() {
								@Override
								public void eventPositiveResponse(Context c,Object[] o) {
									remoteFileListAdapter.addChildItem(item,
											(TreeFilelistAdapter)o[0],pos);
									setFilelistCurrDir(remoteFileListDirBtn,
											remoteUrl);
								}
								@Override
								public void eventNegativeResponse(Context c,Object[] o) {}
							});
							createRemoteFileList(item.getPath()+"/"+item.getName(),ne);
						}
					}
				} else {
					startRemoteFileViewerIntent(remoteFileListAdapter, item);
					//commonDlg.showCommonDialog(false,false,"E","","Remote file was not viewd.",null);
				}
			}
		});
	};
	
	private void setRemoteFilelistLongClickListener() {
		if (remoteFileListView==null) return;
		remoteFileListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				int idx = remoteFileListAdapter.getItem(arg2);
				createFilelistContextMenu(arg1, idx, remoteFileListAdapter);
				return true;
			}
		});
	};

	private void setMsglistLongClickListener() {
		messageListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createMsglistContextMenu(arg1, arg2);
				return true;
			}
		});
	};

	
	private void createProfileContextMenu(View view, int idx) {
		ProfilelistItem item;
		int scn=0;
		for (int i=0;i<profileAdapter.getCount();i++) {
			if (profileAdapter.getItem(i).isChk()) {
				scn++; 
			}
		}
//		if (scn==0 && adapterMenuInfo.position==0) return ;//local is not select
		if (scn<=1) {//single selection 
			for (int i=0;i<profileAdapter.getCount();i++) {
				item = profileAdapter.getItem(i);
				if (idx==i) {// set checked
					profileAdapter.remove(item);
					item.setChk(true);
					profileAdapter.insert(item,i);
					scn=i;//set new index no
				} else {
					if (item.isChk()) {//reset unchecked
						profileAdapter.remove(item);
						item.setChk(false);
						profileAdapter.insert(item,i);
					}
				}
			}
			profileListView.setAdapter(profileAdapter);
			createProfileContextMenu_Single(view, idx, scn);
		} else createProfileContextMenu_Multiple(view, idx);
	};
	
	private void createProfileContextMenu_Single(View view, int idx, int cin) {
		final int itemno ;
		if (idx>0) itemno = cin;
			else itemno=idx;
		ProfilelistItem item = profileAdapter.getItem(itemno);

		if (item.getActive().equals("I")) {
			ccMenu.addMenuItem("Set to active",R.drawable.menu_active)
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
					setProfileToActive();
					setAllProfileItemUnChecked();
			  }
		  	});
		} else {
			ccMenu.addMenuItem("Set to inactive",R.drawable.menu_inactive)
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
					setProfileToInactive();
					setAllProfileItemUnChecked();
			  }
		  	});
		}
		ccMenu.addMenuItem("Add remote profile",R.drawable.menu_add)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				addRemoteProfile("", "", defaultSettingUsername, 
						defaultSettingPassword,
						defaultSettingAddr, "", "");
		  	}
	  	});

		ccMenu.addMenuItem("Edit remote profile",R.drawable.menu_edit)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				ProfilelistItem item = profileAdapter.getItem(itemno);
				editRemoteProfile(item.getActive(), item.getName(), item.getUser(),
						item.getPass(), item.getAddr(), item.getShare(), "",itemno);
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Delete remote profile",R.drawable.menu_delete)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				ProfilelistItem item = profileAdapter.getItem(itemno);
				deleteRemoteProfile(item.getName(), itemno);
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Select all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllProfileItemChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void createProfileContextMenu_Multiple(View view, int idx) {
		
		ccMenu.addMenuItem("Set to active",R.drawable.menu_active)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setProfileToActive();
			  setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Set to inactive",R.drawable.menu_inactive)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setProfileToInactive();
			  setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Delete selected profiles",R.drawable.menu_delete)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				for (int i=0;i<profileAdapter.getCount();i++) {
					if (profileAdapter.getItem(i).isChk()) {
						ProfilelistItem item = profileAdapter.getItem(i);
						deleteRemoteProfile(item.getName(), i);
					}
				}
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Select all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllProfileItemChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	
	private void createMsglistContextMenu(View view, int idx) {

		ccMenu.addMenuItem("Save log message",R.drawable.menu_create)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  saveLogMessageDlg("/SMBExplorer","log.txt");
		  	}
	  	});
		ccMenu.addMenuItem("Move to top",R.drawable.menu_top)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  messageListView.setSelection(0);
		  	}
	  	});
		ccMenu.addMenuItem("Move to bottom",R.drawable.menu_bottom)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  messageListView.setSelection(messageListView.getCount());
		  	}
	  	});
		ccMenu.addMenuItem("Clear log message",R.drawable.menu_trash)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				messageListAdapter.clear();
				messageListAdapter.setNotifyOnChange(true);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void createFilelistContextMenu(View view, int idx, TreeFilelistAdapter fla) {
		TreeFilelistItem item ;
		
//		currentListViewPosX = profileListView.getFirstVisiblePosition();
//		currentListViewPosY = profileListView.getChildAt(0).getTop();

		fileioLinkParm.clear();
		
		int j=0;
		for (int i=0;i<fla.getDataItemCount();i++) {
			item = fla.getDataItem(i);
			if (item.isChecked()) {
				j++; 
			}
		}
		if (j<=1) {
			for (int i=0;i<fla.getDataItemCount();i++) {
				item = fla.getDataItem(i);
				if (idx==i) {
					item.setChecked(true);
					fla.replaceDataItem(i,item);
					j=i;//set new index no
				} else {
					if (item.isChecked()) {
						item.setChecked(false);
						fla.replaceDataItem(i,item);
					}
				}
			}
//			setListAdapter(fileListAdapter);
			fla.notifyDataSetChanged();
			createFilelistContextMenu_Single(view,idx,j,fla) ;
		} else createFilelistContextMenu_Multiple(view, idx,fla) ;
		
	};

	private void createFilelistContextMenu_Multiple(View view, int idx,final TreeFilelistAdapter fla) {

		ccMenu.addMenuItem("Copy ",R.drawable.menu_copying)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setCopyFrom(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Cut ",R.drawable.menu_move)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setCutFrom(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Delete ",R.drawable.menu_trash)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  deleteItem(fla);
		  	}
	  	});

		String tmp;
    	if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tmp=localUrl;
    		else tmp=remoteUrl;
		if (isPasteEnabled && isValidPasteDestination(tmp+"/")) {
			final String t_url=tmp;
			String fl="",sep="";
			for (int i=0;i<pasteFromList.size();i++) { 
				fl+=sep+pasteFromList.get(i).getName();
				sep=",";
			}
			ccMenu.addMenuItem("Paste to(/"+fl+")",R.drawable.blank)
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
				  pasteItem(fla,t_url);
			  }
		  	});
		};
		ccMenu.addMenuItem("Select all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void createFilelistContextMenu_Single(View view, int idx, int cin,final TreeFilelistAdapter fla) {
		final TreeFilelistItem item = fla.getDataItem(idx);
		
		final int itemno ;
		if (cin>0) itemno = cin;
			else itemno=idx;
		
		ccMenu.addMenuItem("Property",R.drawable.menu_properties)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  showProperty(fla,"C", item.getName(), item.isDir(),itemno);
		  	}
	  	});
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			ccMenu.addMenuItem("TextFileBrowser")
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
				  invokeTextFileBrowser(fla,"C", item.getName(), item.isDir(),itemno);
			  	}
		  	});
		}
		ccMenu.addMenuItem("Create directory",R.drawable.menu_create)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  createItem(fla,"C", item,itemno);
		  	}
	  	});

		ccMenu.addMenuItem("Rename '" + item.getName()+"'",R.drawable.menu_rename)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  renameItem(fla,"C", item.getName(), item.isDir(),itemno);
		  	}
	  	});
		ccMenu.addMenuItem("Copy '" + item.getName()+"'",R.drawable.menu_copying)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setCopyFrom(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Cut '" + item.getName()+"'",R.drawable.menu_move)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setCutFrom(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Delete '" + item.getName()+"'",R.drawable.menu_trash)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  deleteItem(fla);
		  	}
	  	});
		String pd="";
		if (item.isDir()) pd=item.getPath()+"/"+item.getName()+"/";
		else pd=item.getPath()+"/";
//		Log.v("","pd="+pd);
		if (isPasteEnabled && isValidPasteDestination(pd)) {
			ccMenu.addMenuItem("Paste to /"+item.getName()+
					" from ("+pasteItemList+")",R.drawable.blank)
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
				  pasteItem(fla,item.getPath()+"/"+item.getName());
			  	}
		  	});
		};
		
		String t_url="";
    	if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) t_url=localUrl+"/";
    	else  t_url=remoteUrl+"/";
    	if (isValidPasteDestination(t_url)) {
			ccMenu.addMenuItem("Paste to /"+
					" from ("+pasteItemList+")",R.drawable.blank)
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
		    		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
		    			pasteItem(localFileListAdapter,localUrl);
		    		else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
		    			pasteItem(remoteFileListAdapter,remoteUrl);
			  	}
		  	});
    	}		
		
		ccMenu.addMenuItem("Select all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item",R.drawable.blank)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void setAllFilelistItemUnChecked(TreeFilelistAdapter fla) {
		TreeFilelistItem item;
		for (int i=0;i<fla.getDataItemCount();i++) {
			if (fla.getDataItem(i).isChecked()) { 
				item=fla.getDataItem(i);
				item.setChecked(false);
				fla.replaceDataItem(i,item);
			}
		}
	};

	private void setAllFilelistItemChecked(TreeFilelistAdapter fla) {
		TreeFilelistItem item;
		for (int i=0;i<fla.getDataItemCount();i++) {
			item=fla.getDataItem(i);
			item.setChecked(true);
			fla.replaceDataItem(i,item);
		}
	};

	private void setAllProfileItemUnChecked() {
		ProfilelistItem item;
		for (int i=0;i<profileAdapter.getCount();i++) {
			item=profileAdapter.getItem(i);
			profileAdapter.remove(item);
			item.setChk(false);
			profileAdapter.insert(item,i);
		}
	};

	private void setAllProfileItemChecked() {
		ProfilelistItem item;
		for (int i=0;i<profileAdapter.getCount();i++) {
			item=profileAdapter.getItem(i);
			profileAdapter.remove(item);
			item.setChk(true);
			profileAdapter.insert(item,i);
		}
	};

	private void invokeTextFileBrowser(TreeFilelistAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {
		TreeFilelistItem item=fla.getDataItem(item_num);
		try {
			Intent intent;
			intent = new Intent();
			intent.setClassName("com.sentaroh.android.TextFileBrowser",
					"com.sentaroh.android.TextFileBrowser.TextFileBrowserMain");
			intent.setDataAndType(
					Uri.parse("file://"+item.getPath()+"/"+item.getName()), null);
				startActivity(intent);
		} catch(ActivityNotFoundException e) {
			commonDlg.showCommonDialog(false,"E", "TextFileBrowser can not be found.",
					"File name="+item.getName(),null);
		}
	};
	
	private void startLocalFileViewerIntent(TreeFilelistItem item) {
		String mt = null, fid = null;
		sendDebugLogMsg(1,"I","Start Intent: name=" + item.getName());
		if (item.getName().lastIndexOf(".") > 0) {
			fid = item.getName().substring(item.getName().lastIndexOf(".") + 1,
					item.getName().length());
			fid=fid.toLowerCase();
		}
		mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		if (mt==null && fid!=null && fid.equals("log")) mt="text/plain";
		if (mt != null) {
//			setFixedOrientation(true);
			if (mt.startsWith("text")) mt="text/plain";
			try {
				Intent intent;
				if (mt.startsWith("text")) {
					intent = new Intent();
					intent.setClassName("com.sentaroh.android.TextFileBrowser",
							"com.sentaroh.android.TextFileBrowser.TextFileBrowserMain");
				} else {
					intent = new Intent(android.content.Intent.ACTION_VIEW);
				}
				intent.setDataAndType(
						Uri.parse("file://"+item.getPath()+"/"+item.getName()), mt);
					startActivity(intent);
			} catch(ActivityNotFoundException e) {
				commonDlg.showCommonDialog(false,"E", "File viewer can not be found.",
						"File name="+item.getName()+", MimeType="+mt,null);
			}
		} else {
			commonDlg.showCommonDialog(false,"E", "MIME type can not be found.",
					"File name="+item.getName(),null);
		}
	};
	
	private void startRemoteFileViewerIntent(TreeFilelistAdapter fla,
			final TreeFilelistItem item) {
		String fid = null;
		sendDebugLogMsg(1,"I","Start Intent: name=" + item.getName());
		if (item.getName().lastIndexOf(".") > 0) {
			fid = item.getName().substring(item.getName().lastIndexOf(".") + 1,
					item.getName().length());
			fid=fid.toLowerCase();
		}
		String mtx=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		if (mtx==null && fid!=null && fid.equals("log")) mtx="text/plain";

		if (mtx != null) {
			if (mtx.startsWith("text")) mtx="text/plain";
			final String mt=mtx;
			NotifyEvent ntfy=new NotifyEvent(this);
			// set response listener
			ntfy.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
//					setFixedOrientation(true);
					try {
						Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(
							Uri.parse("file://"+
								SMBExplorerRootDir+"/SMBExplorer/download/"+
									item.getName()), mt);
//						startActivityForResult(intent,1);
						startActivity(intent);
					} catch(ActivityNotFoundException e) {
						commonDlg.showCommonDialog(false,"E", "File viewer can not be found.",
								"File name="+item.getName()+", MimeType="+mt,null);
					}
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
				}
		
			});
			downloadRemoteFile(fla, item, remoteUrl, ntfy );

		} else {
			commonDlg.showCommonDialog(false,"E", "MIME type can not be found.",
					"File name="+item.getName(),null);
		}

	};
	
	private void downloadRemoteFile(TreeFilelistAdapter fla, 
			TreeFilelistItem item, 
			String url, NotifyEvent p_ntfy) {
		fileioLinkParm.clear();
		buildFileioLinkParm(fileioLinkParm, item.getPath(),
				SMBExplorerRootDir+"/SMBExplorer/download/",item.getName(),"",
				smbUser,smbPass,false);
		startFileioTask(fla,FILEIO_PARM_DOWLOAD_RMOTE_FILE,fileioLinkParm,
				item.getName(),p_ntfy);
	};
	
	private void startFileioTask(TreeFilelistAdapter fla,
			final int op_cd,final ArrayList<FileIoLinkParm> alp,String item_name,
			final NotifyEvent p_ntfy) {
		setAllFilelistItemUnChecked(fla);
		
		String dst="";
		String dt = null;
		String nitem=item_name;
		switch (op_cd) {
			case FILEIO_PARM_LOCAL_CREATE:
			case FILEIO_PARM_REMOTE_CREATE:
				dt="Create";
				dst=item_name+" was created.";
				nitem="";
				break;
			case FILEIO_PARM_LOCAL_RENAME:
			case FILEIO_PARM_REMOTE_RENAME:
				dt="Rename";
				dst=item_name+" was renamed.";
				nitem="";
				break;
			case FILEIO_PARM_LOCAL_DELETE:
			case FILEIO_PARM_REMOTE_DELETE:
				dt="Delete";
				dst="Following dirs/files were deleted.";
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
			case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
			case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
			case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
				dt="Copy";
				dst="Following dirs/files were copied.";
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
			case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
			case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
			case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
				dt="Move";
				dst="Following dirs/files were moved.";
				break;
			case FILEIO_PARM_DOWLOAD_RMOTE_FILE:
				dt="Download";
				dst="";
			default:
				break;
		}
		
		final ThreadCtrl tc=new ThreadCtrl();
		tc.setEnable();

//		setFixedOrientation(true);
		
		@SuppressWarnings("unused")
		final String fdt=dt, fdst=dst, fdmsg=nitem;
		
		// カスタムダイアログの生成
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.progress_spin_dlg);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_title)).setText(dt);
		final Button btnCancel =(Button)dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);

		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
					// Cancel fileio process
					tc.setDisable();
				}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		dialog.setCancelable(false);
		dialog.show();

		NotifyEvent ne=new NotifyEvent(currentContext);
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				if (!tc.isThreadResultSuccess()) {
					if (p_ntfy!=null) p_ntfy.notifyTolistener(false, null);
					if (tc.isThreadResultCancelled()) {
						commonDlg.showCommonDialog(false,"W","File I/O task was cancelled.","",null);
						sendLogMsg("W","File I/O task was cancelled.");
						refreshFilelistView();
					}
					else {
						commonDlg.showCommonDialog(false,"E","File I/O task was failed.","",null);
						sendLogMsg("E","File I/O task was failed.");
						refreshFilelistView();
					}
				} else {
					if (p_ntfy!=null) p_ntfy.notifyTolistener(true, null);
					else {
						commonDlg.showCommonDialog(false,"I",fdst,fdmsg,null);
						sendLogMsg("I",fdst+"\n"+fdmsg);
//						refreshFilelistView();
						refreshTreeFilelist(alp,op_cd);
					}
				}
//				setFixedOrientation(false);
				alp.clear();
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				
			}
		});
		
		Thread th = new Thread(new FileIo(messageListView,
				messageListAdapter, dialog, op_cd, alp,tc, debugLevel,ne,this));
		tc.initThreadCtrl();
		th.setPriority(Thread.MIN_PRIORITY);
		th.start(); 
		
		showDelayedProgDlg(200,dialog, tc);
	};
	
	private void refreshTreeFilelist(ArrayList<FileIoLinkParm> alp, int op_cd) {
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			// Local Process
			ArrayList<TreeFilelistItem> tfl=
					createLocalFileList(false, refreshUrl);
			if (!result_createFileListView) return;
			for (int i=localFileListAdapter.getDataItemCount()-1;i>=0;i--) {
//				Log.v("","i="+i);
				if (localFileListAdapter.getDataItem(i).getPath()
						.startsWith(refreshUrl)) {
					localFileListAdapter.removeDataItem(i);
//					localFileListAdapter.createShowList();
				}
			}
			if (localFileListAdapter.getDataItemCount()>0) {
				for (int i=0;i<localFileListAdapter.getDataItemCount();i++) {
					String d_dir=localFileListAdapter.getDataItem(i).getPath()+
							"/"+localFileListAdapter.getDataItem(i).getName();
					if (d_dir.equals(refreshUrl)) {
						localFileListAdapter.addChildItem(
								localFileListAdapter.getDataItem(i), tfl, i);
						break;
					}
				}
			} else {
				for (int i=0;i<tfl.size();i++)
					localFileListAdapter.addDataItem(tfl.get(i));
				localFileListAdapter.createShowList();
			}
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			// Remote process
			NotifyEvent ne=new NotifyEvent(currentContext);
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					TreeFilelistAdapter tadpt = (TreeFilelistAdapter)o[0];
					for (int i=remoteFileListAdapter.getDataItemCount()-1;i>=0;i--) {
//						Log.v("","r_dir="+refreshUrl+", p="+remoteFileListAdapter.getDataItem(i).getPath());
						if (remoteFileListAdapter.getDataItem(i).getPath()
								.startsWith(refreshUrl)) {
//							Log.v("","delete="+remoteFileListAdapter.getDataItem(i).getPath()+
//									"/"+remoteFileListAdapter.getDataItem(i).getName());
							remoteFileListAdapter.removeDataItem(i);
						}
					}
					if (remoteFileListAdapter.getDataItemCount()!=0) {
						for (int i=0;i<remoteFileListAdapter.getDataItemCount();i++) {
							String d_dir=remoteFileListAdapter.getDataItem(i).getPath()+
									"/"+remoteFileListAdapter.getDataItem(i).getName();
//							Log.v("","r_dir="+refreshUrl+", d="+d_dir);
							if (d_dir.equals(refreshUrl)) {
								remoteFileListAdapter.addChildItem(
										remoteFileListAdapter.getDataItem(i), tadpt, i);
								break;
							}
						}
					} else {
						for (int i=0;i<tadpt.getDataItemCount();i++)
							remoteFileListAdapter.addDataItem(tadpt.getDataItem(i));
						remoteFileListAdapter.createShowList();
					}
//					remoteFileListAdapter.createShowList();
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {}
			});
			createRemoteFileList(refreshUrl,ne);
		} else return; //file list not selected
	};
	
	private ArrayList<FileIoLinkParm> buildFileioLinkParm(ArrayList<FileIoLinkParm> alp, 
			String tgt_url1, String tgt_url2,  
			String tgt_name, String tgt_new, String username, String password) {
		FileIoLinkParm fiop=new FileIoLinkParm();
		fiop.setUrl1(tgt_url1);
		fiop.setUrl2(tgt_url2);
		fiop.setName(tgt_name);
		fiop.setNew(tgt_new);
		fiop.setUser(username);
		fiop.setPass(password);
		
		alp.add(fiop);

		return alp;
	};
	
	private ArrayList<FileIoLinkParm> buildFileioLinkParm(ArrayList<FileIoLinkParm> alp, 
			String tgt_url1, String tgt_url2,  
			String tgt_name, String tgt_new, String username, String password,
			boolean allcopy) {
		FileIoLinkParm fiop=new FileIoLinkParm();
		fiop.setUrl1(tgt_url1);
		fiop.setUrl2(tgt_url2);
		fiop.setName(tgt_name);
		fiop.setNew(tgt_new);
		fiop.setUser(username);
		fiop.setPass(password);
		fiop.setAllCopy(allcopy);
		
		alp.add(fiop);

		return alp;
	};

	private void createItem(final TreeFilelistAdapter fla,
			final String item_optyp, final TreeFilelistItem fi, final int item_num) {
		sendDebugLogMsg(1,"I","createItem entered.");
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.file_rename_create_dlg);
		final EditText newName = 
				(EditText) dialog.findViewById(R.id.file_rename_create_dlg_newname);
		final Button btnOk = 
				(Button) dialog.findViewById(R.id.file_rename_create_dlg_ok_btn);
		final Button btnCancel = 
				(Button) dialog.findViewById(R.id.file_rename_create_dlg_cancel_btn);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		((TextView)dialog.findViewById(R.id.file_rename_create_dlg_title))
			.setText("Create directory");
		((TextView)dialog.findViewById(R.id.file_rename_create_dlg_subtitle))
			.setText("Enter new name");

		// newName.setText(item_name);

		btnOk.setEnabled(false);
		// btnCancel.setEnabled(false);
		newName.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.toString().length() < 1) btnOk.setEnabled(false);
				else btnOk.setEnabled(true);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
				if (!checkDuplicateDir(fla,newName.getText().toString())) {
					commonDlg.showCommonDialog(false,"E","Create","Duplicate directory name specified",null);
				} else {
					int cmd=0;
					if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
						String td="";
						if (fi.isDir()) td=fi.getPath()+"/"+fi.getName();
						else td=fi.getPath();
						fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								td,"",newName.getText().toString(),"",
								smbUser,smbPass); 
//						refreshUrl=localUrl;
						refreshUrl=td;
						cmd=FILEIO_PARM_LOCAL_CREATE;
					} else {
						String td="";
						cmd=FILEIO_PARM_REMOTE_CREATE;
						if (fi.isDir()) td=fi.getPath()+"/"+fi.getName();
						else td=fi.getPath();
						fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								td,"",newName.getText().toString(),"",
								smbUser,smbPass);
//						refreshUrl=remoteUrl;
						refreshUrl=td;
					}
					sendDebugLogMsg(1,"I","createItem FILEIO task invoked.");
					startFileioTask(fla,cmd,fileioLinkParm,
							newName.getText().toString(),null);
				}
			}
		});
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
				sendDebugLogMsg(1,"W","createItem cancelled.");
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		setFixedOrientation(true);
//		dialog.setCancelable(false);
		dialog.show();
	};

	private boolean checkDuplicateDir(TreeFilelistAdapter fla,String ndir) {
		for (int i = 0; i < fla.getCount(); i++) {
			if (ndir.equals(fla.getDataItem(fla.getItem(i)).getName()))
				return false; // duplicate dir
		}
		return true;
	};

	private void showProperty(TreeFilelistAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		TreeFilelistItem item = fla.getDataItem(item_num);
		
		String info;
		
		info = "Path="+item.getPath()+"\n";
		info = info+
				"Name="+item.getName()+"\n"+
				"Directory : "+item.isDir()+"\n"+
				"Hidden : "+item.isHidden()+"\n"+
				"Length : "+item.getLength()+"\n"+
				"Last modified : "+df.format(item.getLastModified())+"\n"+
				"Last modified(ms):"+item.getLastModified();
		commonDlg.showCommonDialog(false,"I","Property",info,null);

	}
	
	private void renameItem(final TreeFilelistAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {

		sendDebugLogMsg(1,"I","renameItem entered.");
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.file_rename_create_dlg);
		final EditText newName = 
				(EditText) dialog.findViewById(R.id.file_rename_create_dlg_newname);
		final Button btnOk = 
				(Button) dialog.findViewById(R.id.file_rename_create_dlg_ok_btn);
		final Button btnCancel = 
				(Button) dialog.findViewById(R.id.file_rename_create_dlg_cancel_btn);

		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		((TextView) dialog.findViewById(R.id.file_rename_create_dlg_title))
			.setText("Rename");
		((TextView) dialog.findViewById(R.id.file_rename_create_dlg_subtitle))
			.setText("Enter new name");

		newName.setText(item_name);

		btnOk.setEnabled(false);
		newName.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.toString().length() < 1 || item_name.equals(s.toString())) btnOk.setEnabled(false);
				else btnOk.setEnabled(true);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
				if (item_name.equals(newName.getText().toString())) {
					commonDlg.showCommonDialog(false,"E","Rename", "Duplicate file name specified",null);
				} else {
					int cmd=0;
					if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
						fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								fla.getDataItem(item_num).getPath() , 
								fla.getDataItem(item_num).getPath(),
								item_name,newName.getText().toString(),"","");
//						refreshUrl=localUrl;
						refreshUrl=fla.getDataItem(item_num).getPath();
						cmd=FILEIO_PARM_LOCAL_RENAME;
					} else {
						cmd=FILEIO_PARM_REMOTE_RENAME;
//						refreshUrl=remoteUrl;
						refreshUrl=fla.getDataItem(item_num).getPath();
						if (item_isdir)
							fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								fla.getDataItem(item_num).getPath(), 
								fla.getDataItem(item_num).getPath(),
								item_name,newName.getText().toString(),
								smbUser,smbPass);
						else
							fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								fla.getDataItem(item_num).getPath(), 
								fla.getDataItem(item_num).getPath(),
								item_name,newName.getText().toString(),smbUser,smbPass);
					}
					sendDebugLogMsg(1,"I","renameItem FILEIO task invoked.");
					startFileioTask(fla,cmd,fileioLinkParm,item_name,null);
				}
			}
		});
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
				sendDebugLogMsg(1,"W","renameItem cancelled.");
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		setFixedOrientation(true);
//		dialog.setCancelable(false);
		dialog.show();
	};

	private void deleteItem(final TreeFilelistAdapter fla) {
		sendDebugLogMsg(1,"I","deleteItem entered.");
		String di ="";
		for (int i=0;i<fla.getCount();i++) {
			TreeFilelistItem item = fla.getDataItem(fla.getItem(i));
			if (item.isChecked()) di=di+item.getName()+"\n";
		}

		final String item_name=di;
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] t) {
				for (int i=fla.getCount()-1;i>=0;i--) {
					TreeFilelistItem item = fla.getDataItem(fla.getItem(i));
					if (item.isChecked()) {
						if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
							buildFileioLinkParm(fileioLinkParm,item.getPath(),
									"",item.getName(),"","","");
							refreshUrl=item.getPath();
						} else {
							refreshUrl=item.getPath();
							buildFileioLinkParm(fileioLinkParm,item.getPath(),
										"",item.getName(),"",smbUser,smbPass);
						}
					}
				}				
				sendDebugLogMsg(1,"I","deleteItem invokw FILEIO task.");
				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL))
					startFileioTask(fla,FILEIO_PARM_LOCAL_DELETE,fileioLinkParm,item_name,null);
				else startFileioTask(fla,FILEIO_PARM_REMOTE_DELETE,fileioLinkParm,item_name,null);
			}
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				sendDebugLogMsg(1,"W","deleteItem canceled");
			}
		});
		commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_delete_file_dirs_confirm),di,ne);
	};
	
	private ArrayList<TreeFilelistItem> pasteFromList=new ArrayList<TreeFilelistItem>();
	private String pasteFromUrl, pasteItemList="";
	private boolean isPasteCopy=false,isPasteEnabled=false, isPasteFromLocal=false;
	private void setCopyFrom(TreeFilelistAdapter fla) {
		pasteItemList="";
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			pasteFromUrl=localUrl;
//			pasteFromDir=localCurrDir;
			isPasteFromLocal=true;
		} else {
			pasteFromUrl=remoteUrl;
//			pasteFromDir=remoteCurrDir;
			isPasteFromLocal=false;
		}
		//Get selected item names
		isPasteCopy=true;
		isPasteEnabled=true;
		TreeFilelistItem fl_item;
		pasteFromList.clear();
		String sep="";
		for (int i = 0; i < fla.getCount(); i++) {
			fl_item = fla.getDataItem(fla.getItem(i));
			if (fl_item.isChecked()) {
				pasteItemList=pasteItemList+sep+fl_item.getName();
				sep=",";
				pasteFromList.add(fl_item);
			}
		}
		setAllFilelistItemUnChecked(fla);
		setPasteItemList();
		sendDebugLogMsg(1,"I","setCopyFrom fromUrl="+pasteFromUrl+
				", num_of_list="+pasteFromList.size());
	};
	
	private void setCutFrom(TreeFilelistAdapter fla) {
		pasteItemList="";
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			pasteFromUrl=localUrl;
//			pasteFromDir=localCurrDir;
			isPasteFromLocal=true;
		} else {
			pasteFromUrl=remoteUrl;
//			pasteFromDir=remoteCurrDir;
			isPasteFromLocal=false;
		}
		//Get selected item names
		isPasteCopy=false;
		isPasteEnabled=true;
		TreeFilelistItem fl_item;
		pasteFromList.clear();
		String sep="";
		for (int i = 0; i < fla.getCount(); i++) {
			fl_item = fla.getDataItem(fla.getItem(i));
			if (fl_item.isChecked()) {
				pasteItemList=pasteItemList+sep+fl_item.getName();
				sep=",";
				pasteFromList.add(fl_item);
			}
		}
		setAllFilelistItemUnChecked(fla);
		setPasteItemList();
		sendDebugLogMsg(1,"I","setCutFrom fromUrl="+pasteFromUrl+
				", num_of_list="+pasteFromList.size());
	};
	
	private void setPasteItemList() {
		TextView pp=(TextView)findViewById(R.id.explorer_filelist_profile_tab_pastelist);
		TextView pm=(TextView)findViewById(R.id.explorer_filelist_message_tab_pastelist);
		TextView pl=(TextView)findViewById(R.id.explorer_filelist_local_tab_pastelist);
		TextView pr=(TextView)findViewById(R.id.explorer_filelist_remote_tab_pastelist);
		if (isPasteEnabled) {
			String op="Copy : ";
			if (!isPasteCopy) op="Cut : ";
			pp.setText(op+pasteItemList);
			pm.setText(op+pasteItemList);
			pl.setText(op+pasteItemList);
			pr.setText(op+pasteItemList);
		}
	};
	
	private void clearPasteItemList() {
		isPasteEnabled=false;
		pasteItemList="";
		TextView pp=(TextView)findViewById(R.id.explorer_filelist_profile_tab_pastelist);
		TextView pm=(TextView)findViewById(R.id.explorer_filelist_message_tab_pastelist);
		TextView pl=(TextView)findViewById(R.id.explorer_filelist_local_tab_pastelist);
		TextView pr=(TextView)findViewById(R.id.explorer_filelist_remote_tab_pastelist);
		pp.setText(pasteItemList);
		pm.setText(pasteItemList);
		pl.setText(pasteItemList);
		pr.setText(pasteItemList);
	};
	
	private boolean isValidPasteDestination(String tdir) {
		sendDebugLogMsg(1,"I","isValidPasteDestination entered");
		boolean valid=true;
		if (!isPasteEnabled) valid=false;
		else {
			if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				if(isPasteFromLocal) {
					if (isSameMountPoint(pasteFromUrl,tdir)) 
						valid=checkValidDestination(tdir);
				}
			} else {
				if(!isPasteFromLocal) {
//					Log.v("","pf="+pasteFromUrl+", t="+remoteUrl);
					if (pasteFromUrl.equals(remoteUrl)) {
						valid=checkValidDestination(tdir);
					}
				}
			}
		}
		sendDebugLogMsg(1,"I","isValidPasteDestination exited, result="+valid+
				", currentTabName="+currentTabName+
				", isPasteFromLocal="+isPasteFromLocal+
				", pasteFromUrl="+pasteFromUrl);
		return valid;
	};

	private boolean checkValidDestination(String tdir) {
		boolean valid=true;
		String toDir="", fromDir="";
		for (int i=0;i<pasteFromList.size();i++) {
			TreeFilelistItem pfli=pasteFromList.get(i);
			if (pfli.isDir()) fromDir=pfli.getPath()+"/"+pfli.getName()+"/";						
			else fromDir=pfli.getPath();
			if (pfli.isDir()) toDir=tdir+pfli.getName()+"/"; 
			else toDir=tdir;	
			toDir=toDir.replaceAll("//", "/");
//			Log.v("","fromDir="+fromDir+", tdir="+tdir+",toDir="+toDir);
			if (toDir.equals(fromDir)) {
				valid=false;
				break;
			}
		}
		sendDebugLogMsg(1,"I","checkValidDestination exited, result="+valid+
				", currentTabName="+currentTabName+
				", toDir="+toDir+", tdir="+tdir+", fromDir="+fromDir+
				", isPasteFromLocal="+isPasteFromLocal+
				", pasteFromUrl="+pasteFromUrl);
		return valid;
	};

	private void pasteItem(final TreeFilelistAdapter fla, final String to_dir) {
		//Get selected item names
		TreeFilelistItem fl_item;
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			String fl_name="",fl_exists="";
			boolean fl_conf_req=false;
			for (int i = 0; i < pasteFromList.size(); i++) {
				fl_item = pasteFromList.get(i);
				fl_name=fl_name+fl_item.getName()+"\n";
				File lf=new File(to_dir+"/"+fl_item.getName());
				if (lf.exists()) {
					fl_conf_req=true;
					fl_exists=fl_exists+fl_item.getName()+"\n";
				}
			}
//			Log.v("","t="+to_dir);
			pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req);
			refreshUrl=to_dir;
		} else {
			final ArrayList<String> d_list=new ArrayList<String>();
			for (int i = 0; i < pasteFromList.size(); i++)
				d_list.add(to_dir+pasteFromList.get(i).getName());
			NotifyEvent ntfy=new NotifyEvent(this);
			// set commonDialog response 
			ntfy.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					String fl_name="",fl_exists="";
					boolean fl_conf_req=false;
					for (int i=0;i<d_list.size();i++) 
						if (!d_list.get(i).equals("")) 
							fl_exists=fl_exists+d_list.get(i)+"\n";
					if (!fl_exists.equals("")) fl_conf_req=true;
					for (int i = 0; i < pasteFromList.size(); i++) 
						fl_name=fl_name+pasteFromList.get(i).getName()+"\n";
					pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req);
					refreshUrl=to_dir;
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {	}
			});
			checkRemoteFileExists(remoteUrl, smbUser,smbPass, d_list, ntfy);
		}
	};

	private void pasteCreateIoParm(TreeFilelistAdapter fla, String to_dir, 
			String fl_name, String fl_exists,boolean fl_conf_req) {
		
		TreeFilelistItem fi ;
		for (int i=0;i<pasteFromList.size();i++) {
			fi=pasteFromList.get(i);
			buildFileioLinkParm(fileioLinkParm, fi.getPath(),
					to_dir,fi.getName(),"",smbUser,smbPass);
		}
		// copy process
		if (isPasteCopy) {
			if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				// Local to Local copy localCurrDir->curr_dir
				copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);
			} else if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Local to Remote copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);

			} else if (!isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Remote to Remote copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);

			} else {
				// Remote to Local copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);
			}
		} else {
			// process move
			clearPasteItemList();
			if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				// Local to Local 
				moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);
			} else if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Local to Remote 
				moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);

			} else if (!isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Remote to Remote 
				moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);

			} else {
				// Remote to Local 
				moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists);
			}
		}

	};
	
	private void copyConfirm(final TreeFilelistAdapter fla,
			final int cmd_cd, ArrayList<FileIoLinkParm> alp, 
			final String selected_name, boolean conf_req, String conf_msg) {
			
		if (conf_req) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","copyConfirm File I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null);
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Ccopy override confirmation cancelled.");
				}
			});
			commonDlg.showCommonDialog(true,"W","Copy following dirs/files are overrides?",
					conf_msg,ne);

		} else {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","copyConfirm FILE I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null);
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Copy cancelled."+"\n"+selected_name);
				}
			});
			commonDlg.showCommonDialog(true,"I","Following dirs/files are copy?",selected_name,ne);
		}
		return;
	};
	
	private void moveConfirm(final TreeFilelistAdapter fla,
			final int cmd_cd, ArrayList<FileIoLinkParm> alp, 
			final String selected_name, boolean conf_req, String conf_msg) {
			
		if (conf_req) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","moveConfirm File I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null);
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Move override confirmation cancelled.");
				}
			});
			commonDlg.showCommonDialog(true,"W","Move following dirs/files are overrides?",
					conf_msg,ne);

		} else {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","moveConfirm FILE I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null);
				}
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Move cancelled."+"\n"+selected_name);
				}
			});
			commonDlg.showCommonDialog(true,"I","Following dirs/files are move?",selected_name,ne);
		}
		return;
	};
	
	private void setFilelistCurrDir(Spinner tv,String dir_text) {
//		tv.setText(dir_text);
	};

	private ArrayList<TreeFilelistItem> createLocalFileList(boolean dir_only, String url) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		TreeFilelistAdapter filelist;
		
		result_createFileListView=true;

		ArrayList<TreeFilelistItem> dir = new ArrayList<TreeFilelistItem>();
		ArrayList<TreeFilelistItem> fls = new ArrayList<TreeFilelistItem>();

		sendDebugLogMsg(1,"I","create local file list local url=" + url);

		File sf = new File(url);

		// File List

		File[] dirs = sf.listFiles();
		if (dirs!=null) {
			try {
				for (File ff : dirs) {
					if (ff.canRead()) {
						String tfs=GeneralUtilities.convertFileSize(ff.length());
						if (ff.isDirectory()) {
							File tlf=new File(url+"/"+ff.getName());
							String[] tfl=tlf.list();
							TreeFilelistItem tfi=new TreeFilelistItem(ff.getName(),
									sdf.format(ff.lastModified())+", ", true, 0,0,false,
									ff.canRead(),ff.canWrite(),
									ff.isHidden(),ff.getParent(),0);
							tfi.setSubDirItemCount(tfl.length);
							dir.add(tfi);
						} else {
							fls.add(new TreeFilelistItem(ff.getName(), sdf.format(ff
									.lastModified())+","+tfs, false, ff.length(), ff
									.lastModified(),false,
									ff.canRead(),ff.canWrite(),
									ff.isHidden(),ff.getParent(),0));
						}
						sendDebugLogMsg(2,"I","File :" + ff.getName()+","+
							"length: " + ff.length()+","+
							"Lastmod: " + sdf.format(ff.lastModified())+","+
							"Lastmod: " + ff.lastModified()+","+
							"isdir: " + ff.isDirectory()+","+
							"parent: " + ff.getParent());
					} else {
						TreeFilelistItem fli=
								new TreeFilelistItem(ff.getName(), sdf.format(ff
								.lastModified())+","+0, false, ff.length(), ff
								.lastModified(),false,
								ff.canRead(),ff.canWrite(),
								ff.isHidden(),ff.getParent(),0);
						fli.setEnableItem(false);
						fls.add(fli);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				sendDebugLogMsg(0,"E",e.toString());
				commonDlg.showCommonDialog(false,"E",getString(R.string.msgs_local_file_list_create_error),
							e.getMessage(),null);
				result_createFileListView=false;
				return null;
			}
		}

		Collections.sort(dir);
		if (!dir_only) {
			Collections.sort(fls);
			dir.addAll(fls);
		}

//		filelist = new TreeFilelistAdapter(this,this,treeListImage);
//		filelist.setDataList(dir);
		return dir;
	};

	private void createRemoteFileList(String url, final NotifyEvent parent_event) {
		result_createFileListView = true;
		sendDebugLogMsg(1,"I","Create remote filelist remote url:"+url);
	
		// File List
		final NotifyEvent n_event=new NotifyEvent(this);
		// set listener 
		n_event.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				String itemname = "";
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				TreeFilelistAdapter filelist;
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> sf_item=(ArrayList<TreeFilelistItem>)o[0];
				
				List<TreeFilelistItem> dir = new ArrayList<TreeFilelistItem>();
				List<TreeFilelistItem> fls = new ArrayList<TreeFilelistItem>();
				
				for (int i = 0; i < sf_item.size(); i++) {
					itemname = sf_item.get(i).getName();
					if (itemname.equals("IPC$")) {
						// ignore IPC$
					} else {
						if (sf_item.get(i).canRead()) {
							if (sf_item.get(i).isDir()) {
								TreeFilelistItem fi= new TreeFilelistItem(itemname, 
										sdf.format(sf_item.get(i).getLastModified())+", ",
										true, 0,0,false,
										sf_item.get(i).canRead(),sf_item.get(i).canWrite(),
										sf_item.get(i).isHidden(),sf_item.get(i).getPath(),
										sf_item.get(i).getListLevel());
								fi.setSubDirItemCount(sf_item.get(i).getSubDirItemCount());
								dir.add(fi);
							} else {
							    String tfs = GeneralUtilities.convertFileSize(sf_item.get(i).getLength());
	
								fls.add(new TreeFilelistItem(itemname, 
									sdf.format(sf_item.get(i).getLastModified())+","+tfs, false,
									sf_item.get(i).getLength(),sf_item.get(i).getLastModified(),false,
									sf_item.get(i).canRead(),sf_item.get(i).canWrite(),
									sf_item.get(i).isHidden(),sf_item.get(i).getPath(),
									sf_item.get(i).getListLevel()));
							}
						} else {
							TreeFilelistItem fli=
									new TreeFilelistItem(itemname, 
									sdf.format(sf_item.get(i).getLastModified())+","+0, false,
									sf_item.get(i).getLength(),sf_item.get(i).getLastModified(),false,
									sf_item.get(i).canRead(),sf_item.get(i).canWrite(),
									sf_item.get(i).isHidden(),sf_item.get(i).getPath(),
									sf_item.get(i).getListLevel());
							fli.setEnableItem(false);
							fls.add(fli);
						}
					}
				}
				// addFileListItem("---- End of file ----", false, 0, 0);
	
				Collections.sort(dir);
				Collections.sort(fls);
				dir.addAll(fls);
				filelist = new TreeFilelistAdapter(c);
				filelist.setDataList(dir);
				parent_event.notifyTolistener(true, new Object[]{filelist});
			}
	
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				parent_event.notifyTolistener(false, o);
				commonDlg.showCommonDialog(false,"E",
						getString(R.string.msgs_remote_file_list_create_error),(String)o[0],null);
			}
		});		
		readRemoteFile(url+"/",smbUser,smbPass,n_event);
			
	};

	private void setSpinDialog(Dialog dialog, final ThreadCtrl tc) {
		// カスタムダイアログの生成
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.progress_spin_dlg);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_title))
			.setText(R.string.msgs_progress_spin_dlg_title3);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_msg))
			.setVisibility(TextView.GONE);
		final Button btnCancel = (Button) dialog
			.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btnCancel.setText(R.string.msgs_progress_spin_dlg_title4);

		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisable();//disableAsyncTask();
				sendDebugLogMsg(1,"W","Filelist is cancelled.");
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		setFixedOrientation(true);
//		dialog.setCancelable(false);
	};
	
	private void checkRemoteFileExists(String url, String user, String pass,
			ArrayList<String> d_list, final NotifyEvent n_event) {
		final ArrayList<TreeFilelistItem> remoteFileList=new ArrayList<TreeFilelistItem>();
		
		final ThreadCtrl tc = new ThreadCtrl();
		remoteFileList.clear();
		tc.setEnable();
		final Dialog dialog = new Dialog(this);
		
		setSpinDialog(dialog,tc);
		
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				dialog.dismiss();
//				setFixedOrientation(false);
				if (tc.isThreadResultSuccess()) {
					n_event.notifyTolistener(true, o);
				} else {
					String err="";
					if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
					else err=tc.getThreadMessage();
					n_event.notifyTolistener(false, new Object[]{err});
				}
			}
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {	}
		});
		
		Thread th = new Thread(new RetrieveFilelist(this, messageListAdapter, 
				messageListView, dialog, tc, debugLevel, url, d_list,user,pass,ne));
		th.start();
		
		showDelayedProgDlg(200,dialog,tc);
	
	}

	private void readRemoteFile(String url,String user, String pass,
				final NotifyEvent n_event) {
		final ArrayList<TreeFilelistItem> remoteFileList=new ArrayList<TreeFilelistItem>();
		
		final ThreadCtrl tc = new ThreadCtrl();
		remoteFileList.clear();
		tc.setEnable();
		final Dialog dialog = new Dialog(this);
		
		setSpinDialog(dialog,tc);

		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				dialog.dismiss();
//				setFixedOrientation(false);
				if (tc.isThreadResultSuccess()) {
					n_event.notifyTolistener(true, new Object[]{remoteFileList});
				} else {
					String err="";
					if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
					else err=tc.getThreadMessage();
					n_event.notifyTolistener(false, new Object[]{err});
				}
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}
		});
		
		Thread th = new Thread(new RetrieveFilelist(this, messageListAdapter, 
				messageListView, dialog, tc, debugLevel, url, 
				remoteFileList,user,pass,ne));
		th.start();
		
		showDelayedProgDlg(1000,dialog,tc);

	};

	private void showDelayedProgDlg(final int wt, final Dialog dialog, final ThreadCtrl tc) {
    	final Handler handler=new Handler();

       	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(wt);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (tc.isEnable()) {
							if (dialog!=null) {
								CommonDialog.setDlgBoxSizeCompact(dialog);
								dialog.show();
							}
						}
					}
				});
			}
		})
       	.start();		
	};
	
	private void setRemoteShare(final String prof_user, final String prof_pass,
			final String prof_addr, final NotifyEvent p_ntfy) {
		final ArrayList<String> rows = new ArrayList<String>();

		NotifyEvent ntfy=new NotifyEvent(currentContext);
		ntfy.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				TreeFilelistAdapter tfl = (TreeFilelistAdapter)o[0]; 
				
				for (int i=0;i<tfl.getCount();i++){
					TreeFilelistItem item=tfl.getDataItem(i);
					if (item.isDir() && item.canRead() &&
							!item.getName().startsWith("IPC$")) {
						String tmp=item.getName();
						rows.add(tmp);
					}
				}
				if (rows.size()<1) rows.add(getString(R.string.msgs_no_shared_resource));
				
		    	//カスタムダイアログの生成
		        final Dialog dialog=new Dialog(c);
		        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		    	dialog.setContentView(R.layout.item_select_list_dlg);
		        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
		        	.setText("Select remote share");
		        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
		        	.setText("");
		        
		        CommonDialog.setDlgBoxSizeLimit(dialog,false);
		        
		        ListView lv = (ListView) dialog.findViewById(android.R.id.list);
		        lv.setAdapter(new ArrayAdapter<String>(c, R.layout.simple_list_item_1m, rows));
		        lv.setScrollingCacheEnabled(false);
		        lv.setScrollbarFadingEnabled(false);
		        
		        lv.setOnItemClickListener(new OnItemClickListener(){
		        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
		        		if (rows.get(idx).startsWith("---")) return;
			        	dialog.dismiss();
//			        	setFixedOrientation(false);
						// リストアイテムを選択したときの処理
			        	p_ntfy.notifyTolistener(true,  
			        			new Object[]{rows.get(idx).toString()});
		        	}
		        });	 
		        //CANCELボタンの指定
		        final Button btnCancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
		        btnCancel.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		                dialog.dismiss();
//		                setFixedOrientation(false);
		                p_ntfy.notifyTolistener(true, new Object[]{""});
		            }
		        });
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btnCancel.performClick();
					}
				});
//		        dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		        setFixedOrientation(true);
//		        dialog.setCancelable(false);
		        dialog.show();
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				p_ntfy.notifyTolistener(false, 
						new Object[]{"Remote file list creation error"});
			}
		});
		createRemoteFileList("smb://" + prof_addr + "/",ntfy);
		return;
	};

	private void addRemoteProfile(final String prof_act,
			final String prof_name, final String prof_user,
			final String prof_pass, final String prof_addr,
			final String prof_share, final String msg_text) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.edit_remote_profile);
		final TextView dlg_msg = (TextView) dialog
				.findViewById(R.id.remote_profile_dlg_msg);
		if (msg_text.length() != 0) {
			dlg_msg.setText(msg_text);
		}

		dialog.setTitle("Add remote profile");

		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);
		editname.setText(prof_name);
		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		editaddr.setText(prof_addr);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		edituser.setText(prof_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		editpass.setText(prof_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		editshare.setText(prof_share);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);

		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.remote_profile_active);
		if (prof_act.equals("A"))
			tg.setChecked(true);
		else
			tg.setChecked(false);
		
		// addressボタンの指定
		Button btnAddr = (Button) dialog.findViewById(R.id.remote_profile_addrbtn);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(currentContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new ListenerInterface() {
					@Override
					public void eventPositiveResponse(Context arg0, Object[] arg1) {
						editaddr.setText((String)arg1[0]);
					}

					@Override
					public void eventNegativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText((String)arg1[0]);
					}
					
				});
				setRemoteAddr(ntfy);
			}
		});

		final Button btnGet1 = (Button) dialog.findViewById(R.id.remote_profile_get_btn1);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnGet1.setEnabled(false);
				String prof_addr, prof_user, prof_pass;
				editaddr.selectAll();
				prof_addr = editaddr.getText().toString();
				edituser.selectAll();
				prof_user = edituser.getText().toString();
				editpass.selectAll();
				prof_pass = editpass.getText().toString();

				setJcifsProperties(prof_user, prof_pass);

				NotifyEvent ntfy=new NotifyEvent(currentContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new ListenerInterface() {
					@Override
					public void eventPositiveResponse(Context arg0, Object[] arg1) {
						editshare.setText((String)arg1[0]);
					}

					@Override
					public void eventNegativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText((String)arg1[0]);
					}
					
				});
				setRemoteShare(prof_user,prof_pass, prof_addr,ntfy);
			}
		});

		// CANCELボタンの指定
		final Button btnCancel = (Button) dialog.findViewById(R.id.remote_profile_cancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
			}
		});
		// OKボタンの指定
		Button btnOK = (Button) dialog.findViewById(R.id.remote_profile_ok);
		btnOK.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String new_act, new_name;
				editaddr.selectAll();
				edituser.selectAll();
				editpass.selectAll();
				editshare.selectAll();
				editname.selectAll();
				new_name = editname.getText().toString();

				if (tg.isChecked()) new_act = "A";
				else new_act = "A";

				if (isProfileDuplicated("R", new_name)) {
					dlg_msg.setText(getString(R.string.msgs_add_remote_profile_duplicate));
				} else {
					dialog.dismiss();
//					setFixedOrientation(false);
					int pos = profileListView.getFirstVisiblePosition();
					int topPos = profileListView.getChildAt(0).getTop();
					profileAdapter.add(new ProfilelistItem(
							"R",new_name, new_act,prof_user , prof_pass,prof_addr,
									prof_share,false));
					saveProfile(false,"","");
					profileAdapter = 
							createProfileList(false,""); // create profile list

					profileListView.setSelectionFromTop(pos,topPos);
					profileAdapter.setNotifyOnChange(true);
				}
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		setFixedOrientation(true);
//		dialog.setCancelable(false);
		dialog.show();
	}

	private void editRemoteProfile(String prof_act, String prof_name,
			String prof_user, String prof_pass, String prof_addr,
			String prof_share, String msg_text, final int item_num) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.edit_remote_profile);
		final TextView dlg_msg = (TextView) dialog
				.findViewById(R.id.remote_profile_dlg_msg);

		if (msg_text.length() != 0) {
			dlg_msg.setText(msg_text);
		}

		dialog.setTitle("Edit remote profile");

		CommonDialog.setDlgBoxSizeLimit(dialog,false);
		
		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);
		editname.setText(prof_name);
		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		editaddr.setText(prof_addr);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		edituser.setText(prof_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		editpass.setText(prof_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		editshare.setText(prof_share);

		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.remote_profile_active);
		if (prof_act.equals("A"))
			tg.setChecked(true);
		else
			tg.setChecked(false);
		
		// addressボタンの指定
		Button btnAddr = (Button) dialog.findViewById(R.id.remote_profile_addrbtn);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(currentContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new ListenerInterface() {
					@Override
					public void eventPositiveResponse(Context arg0, Object[] arg1) {
						editaddr.setText((String)arg1[0]);
					}

					@Override
					public void eventNegativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText((String)arg1[0]);
					}
					
				});
				setRemoteAddr(ntfy);
			}
		});


		Button btnGet1 = (Button) dialog.findViewById(R.id.remote_profile_get_btn1);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_addr, prof_user, prof_pass;
				editaddr.selectAll();
				prof_addr = editaddr.getText().toString();
				edituser.selectAll();
				prof_user = edituser.getText().toString();
				editpass.selectAll();
				prof_pass = editpass.getText().toString();

				setJcifsProperties(prof_user, prof_pass);

				NotifyEvent ntfy=new NotifyEvent(currentContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new ListenerInterface() {
					@Override
					public void eventPositiveResponse(Context arg0, Object[] arg1) {
						editshare.setText((String)arg1[0]);
					}

					@Override
					public void eventNegativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText((String)arg1[0]);
					}
					
				});
				setRemoteShare(prof_user,prof_pass, prof_addr,ntfy);
			}
		});

		// CANCELボタンの指定
		final Button btnCancel = (Button) dialog.findViewById(R.id.remote_profile_cancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				setFixedOrientation(false);
			}
		});
		// OKボタンの指定
		Button btnOK = (Button) dialog.findViewById(R.id.remote_profile_ok);
		btnOK.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String new_user, new_pass, new_addr, new_share, new_act, new_name;

				dialog.dismiss();
//				setFixedOrientation(false);

				editaddr.selectAll();
				new_addr = editaddr.getText().toString();
				edituser.selectAll();
				new_user = edituser.getText().toString();
				editpass.selectAll();
				new_pass = editpass.getText().toString();
				editshare.selectAll();
				new_share = editshare.getText().toString();
				editname.selectAll();
				new_name = editname.getText().toString();

				if (tg.isChecked()) new_act = "A";
				else new_act = "I";

				int pos = profileListView.getFirstVisiblePosition();
				int topPos = profileListView.getChildAt(0).getTop();
				ProfilelistItem item=profileAdapter.getItem(item_num);

				profileAdapter.remove(item);
				profileAdapter.insert(new ProfilelistItem("R",
						new_name, new_act, new_user, new_pass, new_addr,new_share,false),
						item_num);
				
				saveProfile(false,"","");
//				appendProfile();
//				profileAdapter = 
//						createProfileList(false); // create profile list
				profileListView.setSelectionFromTop(pos,topPos);
				profileAdapter.setNotifyOnChange(true);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		setFixedOrientation(true);
//		dialog.setCancelable(false);
		dialog.show();

	};

	private void setProfileToActive() {
		ProfilelistItem item ;

		for (int i=0;i<profileAdapter.getCount();i++) {
			item = profileAdapter.getItem(i);

			if (item.isChk()) {
				profileAdapter.remove(item);
				profileAdapter.insert(new ProfilelistItem(
					item.getType(), item.getName(), 
					"A",item.getUser(), item.getPass(),item.getAddr(),
					item.getShare(),false),i);
			} 
		}
		
		saveProfile(false,"","");
		//createProfileList(false);
		profileAdapter.setNotifyOnChange(true);
		
	}
	
	private void setProfileToInactive() {
		ProfilelistItem item ;

		for (int i=0;i<profileAdapter.getCount();i++) {
			item = profileAdapter.getItem(i);

			if (item.isChk()) {
				profileAdapter.remove(item);
				profileAdapter.insert(new ProfilelistItem(
					item.getType(), item.getName(), 
					"I",item.getUser(), item.getPass(),item.getAddr(),
					item.getShare(),false),i);
			} 
		}
		
		saveProfile(false,"","");
		//createProfileList(false);
		profileAdapter.setNotifyOnChange(true);
	}
	
	private void deleteRemoteProfile(String item_name, final int item_num) {
		
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				ProfilelistItem item = profileAdapter
						.getItem(item_num);

				int pos = profileListView.getFirstVisiblePosition();
				int topPos = profileListView.getChildAt(0).getTop();
				profileAdapter.remove(item);
				profileAdapter.setNotifyOnChange(true);

				saveProfile(false,"","");

				profileListView.setSelectionFromTop(pos,topPos);
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {
				
			}
		});
		commonDlg.showCommonDialog(true,"W",
				String.format(getString(R.string.msgs_delete_confirm),item_name),"",ne);


	}

	private ProfilelistAdapter createProfileList(boolean sdcard, String fp) {

		ProfilelistAdapter pfl = null;
		BufferedReader br = null;
		error_CreateProfileListResult = false;

		sendDebugLogMsg(1,"I","Create profilelist");
		
		List<ProfilelistItem> lcl = new ArrayList<ProfilelistItem>();
		ArrayList<String> ml=LocalMountPoint.buildLocalMountPointList();
		for (int i=0;i<ml.size();i++) {
			ProfilelistItem pli=new ProfilelistItem(
					"L",ml.get(i), "A", 
					"", "", "", 
					"", false);
			lcl.add(pli);
		}
		
		List<ProfilelistItem> rem = new ArrayList<ProfilelistItem>();

		try {
			if (sdcard) {
				File sf = new File(fp);

				if (sf.exists()) {
					br = new BufferedReader(new FileReader(fp));
				} else {
					commonDlg.showCommonDialog(false,"E",
							String.format(getString(R.string.msgs_local_file_list_create_nfound),
									fp),"",null);
					error_CreateProfileListResult=true;
				}
				
			} else {
				InputStream in = openFileInput(SMBEXPLORER_PROFILE_NAME);
				br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			}
			if (!error_CreateProfileListResult) {
				String pl;
				String[] alp;
				while ((pl = br.readLine()) != null && !error_CreateProfileListResult) {
					alp = parseProfileString(pl);
					rem.add(new ProfilelistItem(alp[0], alp[1], alp[2],
							alp[3], alp[4], alp[5], alp[6],false));
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			commonDlg.showCommonDialog(false,"E",
				getString(R.string.msgs_exception),e.toString(),null);
			error_CreateProfileListResult=true;
		} catch (IOException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			commonDlg.showCommonDialog(false,"E",
					getString(R.string.msgs_exception),e.toString(),null);
			error_CreateProfileListResult=true;
		}
		Collections.sort(rem);
		lcl.addAll(rem);
		if (lcl.size()==0) lcl.add(new ProfilelistItem("", "No profiles", "I", "", "", "", "",false));
		// profileListView = (ListView)findViewById(android.R.id.list);
		pfl = new ProfilelistAdapter(this, R.layout.profile_list_view_item, lcl);
		profileListView.setAdapter(pfl);
		pfl.setNotifyOnChange(true);

		return pfl;
	};

	private void saveProfile(boolean sdcard, String fd, String fn) {
		sendDebugLogMsg(1,"I","Save profile");
		PrintWriter pw;
		BufferedWriter bw = null;
		try {
			if (sdcard) {
				File lf = new File(fd);
				if (!lf.exists())
					lf.mkdir();
				bw = new BufferedWriter(new FileWriter(fd+"/"+fn));
				pw = new PrintWriter(bw);
			} else {
				OutputStream out = openFileOutput(SMBEXPLORER_PROFILE_NAME,
						MODE_PRIVATE);
				pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
			}

			if (profileAdapter!=null) {
				for (int i = 0; i <= (profileAdapter.getCount() - 1); i++) {
					ProfilelistItem item = profileAdapter.getItem(i);
					if (item.getType().equals("R")) {
						String pl = item.getType() + "\t" + item.getName() + "\t"
								+ item.getActive() + "\t" + item.getUser() + "\t"
								+ item.getPass() + "\t" + item.getAddr() + "\t"
								+ item.getShare();
						sendDebugLogMsg(9,"I","saveProfileToFile=" + pl);
						pw.println(pl);
					}
				}
			}
			pw.close();
			if (bw!=null) bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			commonDlg.showCommonDialog(false,"E",
					getString(R.string.msgs_exception),e.toString(),null);
		}
	}

	private boolean isProfileDuplicated(String prof_type, String prof_name) {
		boolean dup = false;

		for (int i = 0; i <= profileAdapter.getCount() - 1; i++) {
			ProfilelistItem item = profileAdapter.getItem(i);
			if (item.getName().equals(prof_name)) {
				dup = true;
			}
		}
		return dup;
	}

	private String[] parseProfileString(String text) {

		String[] parm = new String[30];
		String[] parm_t = text.split("\t");

		for (int i = 0; i < parm.length; i++) {
			if (i<parm_t.length) {
				if (parm_t[i].length()==0) parm[i] = "";
				else parm[i] = parm_t[i];
			} else parm[i] = "";
		}
		return parm;

	};
	
	private void setRemoteAddr(final NotifyEvent p_ntfy) {
		final ArrayList<String> rows = new ArrayList<String>();
		NotifyEvent ntfy=new NotifyEvent(currentContext);
		ntfy.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
				if (rows.size()<1) rows.add(getString(R.string.msgs_ip_address_no_address));
				//カスタムダイアログの生成
			    final Dialog dialog=new Dialog(c);
			    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.item_select_list_dlg);
			    ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
			    	.setText(getString(R.string.msgs_ip_address_select_title));
			    TextView filetext= (TextView)dialog.findViewById(R.id.item_select_list_dlg_itemtext);
			    filetext.setText(currentContext.getString(R.string.msgs_ip_address_range_dlg_timeout));
			    filetext.setVisibility(TextView.VISIBLE);
			    Button btnRescan=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
			    btnRescan.setVisibility(TextView.VISIBLE);
			    btnRescan.setText(getString(R.string.msgs_ip_address_range_dlg_rescan));
			    
			    final EditText toEt = (EditText)dialog.findViewById(R.id.item_select_list_dlg_itemname);
			    toEt.setVisibility(EditText.VISIBLE);
			    toEt.setInputType(InputType.TYPE_CLASS_NUMBER);
			    toEt.setText(""+scanIpAddrTimeout);
			    
			    CommonDialog.setDlgBoxSizeLimit(dialog,false);
			    
			    final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
			    lv.setAdapter(new ArrayAdapter<String>(currentContext, R.layout.simple_list_item_1o, rows));
			    lv.setScrollingCacheEnabled(false);
			    lv.setScrollbarFadingEnabled(false);
			    
			    lv.setOnItemClickListener(new OnItemClickListener(){
			    	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
			    		if (rows.get(idx).startsWith("---")) return;
			        	// リストアイテムを選択したときの処理
			            dialog.dismiss();
//			            setFixedOrientation(false);
						p_ntfy.notifyTolistener(true,new Object[]{rows.get(idx)});
			        }
			    });
			    //RESCANボタンの指定
			    btnRescan.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			        	String toVal=toEt.getText().toString();
			        	if (!toVal.equals("")) 
			        		scanIpAddrTimeout = Integer.parseInt(toVal);
			            rows.clear();
			            NotifyEvent ntfy=new NotifyEvent(currentContext);
			    		ntfy.setListener(new ListenerInterface() {
			    			@Override
			    			public void eventPositiveResponse(Context c,Object[] o) {
			    			    lv.setAdapter(new ArrayAdapter<String>(currentContext, R.layout.simple_list_item_1o, rows));
			    			    lv.setScrollingCacheEnabled(false);
			    			    lv.setScrollbarFadingEnabled(false);
			    			}
			    			@Override
			    			public void eventNegativeResponse(Context c,Object[] o) {}

			    		});
			    		createRemoteIpAddrList(rows,ntfy);
			        }
			    });

			    //CANCELボタンの指定
			    final Button btnCancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
			    btnCancel.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			            dialog.dismiss();
//			            setFixedOrientation(false);
			            p_ntfy.notifyTolistener(false, null);
			        }
			    });
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btnCancel.performClick();
					}
				});
//			    dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//			    setFixedOrientation(true);        
//			    dialog.setCancelable(false);
			    dialog.show();
			}
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}

		});
		getScanAddressRange(rows,ntfy);
		
	};

	private String scanIpAddrSubnet;
	private int scanIpAddrBeginAddr,scanIpAddrEndAddr, scanIpAddrTimeout=0;
	private void getScanAddressRange(final ArrayList<String> rows, 
			final NotifyEvent p_ntfy) {
		final String from=getLocalIpAddress();
		String subnet=from.substring(0,from.lastIndexOf("."));
		String subnet_o1, subnet_o2,subnet_o3;
		subnet_o1=subnet.substring(0,subnet.indexOf("."));
		subnet_o2=subnet.substring(subnet.indexOf(".")+1,subnet.lastIndexOf("."));
		subnet_o3=subnet.substring(subnet.lastIndexOf(".")+1,subnet.length());
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.scan_address_range_dlg);
		TextView tvtitle=(TextView) dialog.findViewById(R.id.scan_address_range_title);
		tvtitle.setText(R.string.msgs_ip_address_range_dlg_title);
		final EditText toEt = (EditText) dialog.findViewById(R.id.scan_address_range_timeout);
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o4);
		final EditText eaEt1 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o1);
		final EditText eaEt2 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o2);
		final EditText eaEt3 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o3);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o4);
		toEt.setText("150");
		baEt1.setText(subnet_o1);
		baEt2.setText(subnet_o2);
		baEt3.setText(subnet_o3);
		baEt4.setText("1");
		eaEt1.setText(subnet_o1);
		eaEt2.setText(subnet_o2);
		eaEt3.setText(subnet_o3);
		eaEt4.setText("254");
		
		baEt1.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				eaEt1.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		baEt2.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				eaEt2.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		baEt3.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				eaEt3.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		
		final Button btnCancel = (Button) dialog.findViewById(R.id.scan_address_range_btn_cancel);
		final Button btnOk = (Button) dialog.findViewById(R.id.scan_address_range_btn_ok);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (auditScanAddressRangeValue(dialog)) {
					toEt.selectAll();
					String to=toEt.getText().toString();
					baEt1.selectAll();
					String ba1=baEt1.getText().toString();
					baEt2.selectAll();
					String ba2=baEt2.getText().toString();
					baEt3.selectAll();
					String ba3=baEt3.getText().toString();
					baEt4.selectAll();
					String ba4=baEt4.getText().toString();
					eaEt4.selectAll();
					String ea4=eaEt4.getText().toString();
//					setFixedOrientation(false);
					scanIpAddrSubnet=ba1+"."+ba2+"."+ba3;
					scanIpAddrBeginAddr = Integer.parseInt(ba4);
					scanIpAddrEndAddr = Integer.parseInt(ea4);
					scanIpAddrTimeout = Integer.parseInt(to);
					dialog.dismiss();
					createRemoteIpAddrList(rows,p_ntfy);
				} else {
					//error
				}
			}
		});
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				setFixedOrientation(false);
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		setFixedOrientation(true);
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		dialog.setCancelable(false);
		dialog.show();
	};
	
	boolean cancelIpAddressListCreation =false;
	private void createRemoteIpAddrList(final ArrayList<String> rows,
			final NotifyEvent p_ntfy) {
		final Handler handler=new Handler();
		final String curr_ip=getLocalIpAddress();
		cancelIpAddressListCreation =false;
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.progress_spin_dlg);
		TextView tvtitle=(TextView) dialog.findViewById(R.id.progress_spin_dlg_title);
		tvtitle.setText(R.string.msgs_progress_spin_dlg_addr_listing);
		final TextView tvmsg=(TextView) dialog.findViewById(R.id.progress_spin_dlg_msg);
		final Button btnCancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btnCancel.setText(R.string.msgs_progress_spin_dlg_addr_cancel);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				btnCancel.setEnabled(false);
				sendDebugLogMsg(1,"W","IP Address list creation was cancelled");
				cancelIpAddressListCreation=true;
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(currentContext));
//		dialog.setCancelable(false);
		dialog.show();
		
		sendDebugLogMsg(1,"I","Scan IP address ransge is "+scanIpAddrSubnet+
				"."+scanIpAddrBeginAddr+" - "+scanIpAddrEndAddr);
       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				for (int i=scanIpAddrBeginAddr; i<=scanIpAddrEndAddr;i++) {
					if (cancelIpAddressListCreation) break;
					final int ix=i;
					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							tvmsg.setText(scanIpAddrSubnet+"."+ix);
						}
					});
					if (checkIpAddrReachable(scanIpAddrSubnet+"."+i,scanIpAddrTimeout) && 
							!curr_ip.equals(scanIpAddrSubnet+"."+i)) 
//						if (checkIpAddrSmbActive(scanIpAddrSubnet+"."+i))
						if (isSmbHost(scanIpAddrSubnet+"."+i))
							rows.add(scanIpAddrSubnet+"."+i);
				}
				// dismiss progress bar dialog
				handler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						dialog.dismiss();
						if (p_ntfy!=null)
							p_ntfy.notifyTolistener(true, null);
					}
				});
			}
		})
       	.start();
	};

	private boolean isSmbHost(String address) {
		boolean found=false;
    	try {
			UniAddress ua = UniAddress.getByName(address);
			String cn;
	        cn = ua.firstCalledName();
	        do {
	            if (!cn.startsWith("*")) found=true; 
            	sendDebugLogMsg(1,"I","isSmbHost Address="+address+
	            		", cn="+cn+", found="+found);
	        } while(( cn = ua.nextCalledName() ) != null );
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	return found;
 	};
 	
 	private boolean auditScanAddressRangeValue(Dialog dialog) {
		boolean result=false;
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_address_range_begin_address_o4);
		final EditText eaEt1 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o1);
		final EditText eaEt2 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o2);
		final EditText eaEt3 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o3);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_address_range_end_address_o4);
		final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_address_range_msg);

		baEt1.selectAll();
		String ba1=baEt1.getText().toString();
		baEt2.selectAll();
		String ba2=baEt2.getText().toString();
		baEt3.selectAll();
		String ba3=baEt3.getText().toString();
		baEt4.selectAll();
		String ba4=baEt4.getText().toString();
		eaEt1.selectAll();
		String ea1=eaEt1.getText().toString();
		eaEt2.selectAll();
		String ea2=eaEt2.getText().toString();
		eaEt3.selectAll();
		String ea3=eaEt3.getText().toString();
		eaEt4.selectAll();
		String ea4=eaEt4.getText().toString();
		if (ba1.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt1.requestFocus();
			return false;
		} else if (ba2.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt2.requestFocus();
			return false;
		} else if (ba3.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt3.requestFocus();
			return false;
		} else if (ba4.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt4.requestFocus();
			return false;
		} else if (ea1.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt1.requestFocus();
			return false;
		} else if (ea2.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt2.requestFocus();
			return false;
		} else if (ea3.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt3.requestFocus();
			return false;
		} else if (ea4.equals("")) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt4.requestFocus();
			return false;
		}
		int iba1 = Integer.parseInt(ba1);
		if (iba1<=0||iba1>=255) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
			baEt1.requestFocus();
			return false;
		}
		int iba2 = Integer.parseInt(ba2);
		if (iba2<=0||iba2>=255) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
			baEt2.requestFocus();
			return false;
		}
		int iba3 = Integer.parseInt(ba3);
		if (iba3<=0||iba3>=255) {
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
			baEt3.requestFocus();
			return false;
		}
		int iba4 = Integer.parseInt(ba4);
		int iea4 = Integer.parseInt(ea4);
		if (iba4>0 && iba4<255) {
			if (iea4>0 && iea4<255) {
				if (iba4<=iea4) {
					result=true;
				} else {
					baEt4.requestFocus();
					tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_addr_gt_end_addr));
				}
			} else {
				eaEt4.requestFocus();
				tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_end_range_error));
			}
		} else {
			baEt4.requestFocus();
			tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
		}

		if (iba1==192&&iba2==168) {
			//class c private
		} else {
			if (iba1==10) {
				//class a private
			} else {
				if (iba1==172 && (iba2>=16&&iba2<=31)) {
					//class b private
				} else {
					//not private
					result=false;
					tvmsg.setText(getString(R.string.msgs_ip_address_range_dlg_not_private));
				}
			}
		}
		
		return result;
	};
	
	private boolean checkIpAddrReachable(String address,int timeout) {
		try {
	        InetAddress ip = InetAddress.getByName(address);
            
            if (ip.isReachable(timeout)) { // Try for one tenth of a second
                return true;
            }
	        return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	};

	public void importProfileDlg(final String curr_dir, String file_name) {

		sendDebugLogMsg(1,"I","Import profile dlg.");

		NotifyEvent ne=new NotifyEvent(currentContext);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
    			String fpath=(String)o[0];
    			
				ProfilelistAdapter tfl = createProfileList(true, fpath);
				if (!error_CreateProfileListResult) {
					profileAdapter = tfl;
					saveProfile(false,"","");
					commonDlg.showCommonDialog(false,"I",
							String.format(getString(R.string.msgs_select_import_dlg_success),
									fpath),"",null);
    			}

			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(curr_dir,
				"/SMBExplorer",file_name,"Select import file.",ne);
	};

	public void exportProfileDlg(final String curr_dir, final String ifn) {
		sendDebugLogMsg(1,"I","Export profile.");

		NotifyEvent ne=new NotifyEvent(currentContext);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
    			String fpath=(String)o[0];
    			String fd=fpath.substring(0,fpath.lastIndexOf("/"));
    			String fn=fpath.replace(fd+"/","");
				exportProfileToFile(fd,fn);
			}

			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(curr_dir,
				"/SMBExplorer",ifn,"Select export file.",ne);
	};

	public void exportProfileToFile(final String profile_dir,
			final String profile_filename) {

		sendDebugLogMsg(1,"I","Export profile to file");
		
		File lf = new File(profile_dir + "/" + profile_filename);
		if (lf.exists()) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
					saveProfile(true,profile_dir,profile_filename);
					commonDlg.showCommonDialog(false,"I",
							String.format(getString(R.string.msgs_select_export_dlg_success),
									profile_dir+"/"+profile_filename),"",null);
				}
				
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {}
			});
			commonDlg.showCommonDialog(true,"I",
					String.format(getString(R.string.msgs_select_export_dlg_override),
							profile_dir+"/"+profile_filename),"",ne);
			return;
		} else {
			saveProfile(true,profile_dir,profile_filename);
			commonDlg.showCommonDialog(false,"I",
					String.format(getString(R.string.msgs_select_export_dlg_success),
							profile_dir+"/"+profile_filename),"",null);
		}
	};

	private void setJcifsProperties(String user, String pass) {
		
//		System.setProperty("jcifs.util.loglevel", "10");
//		System.setProperty("jcifs.smb.lmCompatibility", "0");
//		System.setProperty("jcifs.smb.client.useExtendedSecurity", "false");
//		jcifsProp = new Properties();

//		jcifsProp.clear();
//		jcifsProp.setProperty("jcifs.smb.client.username", user);
//		jcifsProp.setProperty("jcifs.smb.client.password", pass);
		// prop.setProperty("jcifs.smb.client.rcv_buf_size", "120832");//60416
		// prop.setProperty("jcifs.smb.client.snd_buf_size", "120832");//16644
		// prop.setProperty("jcifs.smb.maxBuffers","500");
//		Config.setProperties(jcifsProp);
		
		smbUser=user;
		smbPass=pass;
		

		// listJcifsProperty(prop);
	};

	public void saveLogMessageDlg(final String curr_dir, final String ifn) {

		NotifyEvent ne=new NotifyEvent(currentContext);
		// set commonDialog response 
		ne.setListener(new ListenerInterface() {
			@Override
			public void eventPositiveResponse(Context c,Object[] o) {
    			String fpath=(String)o[0];
    			String fd=fpath.substring(0,fpath.lastIndexOf("/"));
    			String fn=fpath.replace(fd+"/","");
    			saveLogMessageToFile(fd,fn);
			}
			@Override
			public void eventNegativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(SMBExplorerRootDir,
				curr_dir,ifn,"Select a destination for log messages.",ne);
	};

	public void saveLogMessageToFile(final String profile_dir,
			final String profile_filename) {

		File lf = new File(profile_dir + "/" + profile_filename);
		if (lf.exists()) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new ListenerInterface() {
				@Override
				public void eventPositiveResponse(Context c,Object[] o) {
	
					writeLogMessageToFile(profile_dir,profile_filename);
					commonDlg.showCommonDialog(false,"I", 
							String.format(getString(R.string.msgs_save_log_msg_file),
							profile_dir+profile_filename),"",null);
				}
			
				@Override
				public void eventNegativeResponse(Context c,Object[] o) {
					
				}
			});
			commonDlg.showCommonDialog(false,"W",
					String.format(getString(R.string.msgs_save_log_msg_override_confirm),
					profile_dir + "/" + profile_filename),"",ne);
			return;
		} else {
			writeLogMessageToFile(profile_dir,profile_filename);
			commonDlg.showCommonDialog(false,"I", 
					String.format(getString(R.string.msgs_save_log_msg_file),
					profile_dir+profile_filename),"",null);

		}
	};

	private void writeLogMessageToFile(String profile_dir, String profile_filename) {
		PrintWriter pw;
		try {
			File lf = new File(profile_dir);
			if (!lf.exists())
				lf.mkdir();
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					profile_dir+"/"+profile_filename));
			pw = new PrintWriter(bw);
			if (messageListAdapter.getCount() > 0) {
				for (int i = 0; i <= (messageListAdapter.getCount() - 1); i++) {
					String item = messageListAdapter.getItem(i).toString();
					pw.println(item);
				}
			}
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
			commonDlg.showCommonDialog(false,"W",
					getString(R.string.msgs_exception)+
					profile_dir + "/" + profile_filename,e.toString(),null);
		}
	};

	private void sendLogMsg(String cat, String logmsg) {

			synchronized(messageListAdapter) {
				Calendar cd = Calendar.getInstance();
				messageListAdapter.add(
			    		 new MsglistItem(cat,sdfDate.format(cd.getTime()),
									sdfTime.format(cd.getTime()),"MAIN",logmsg));}
	};
	
	private void sendDebugLogMsg(int lvl, String cat, String logmsg) {

		if (debugLevel>=lvl) {
			Calendar cd = Calendar.getInstance();
			Log.v(DEBUG_TAG,cat+" "+"DEBUG-M"+" "+logmsg);
			if (messageListAdapter==null) return;
			synchronized(messageListAdapter) {
				
				messageListAdapter.add(
			    		 new MsglistItem(cat,sdfDate.format(cd.getTime()),
									sdfTime.format(cd.getTime()),"DEBUG-M",logmsg));}
		}
	};

	private void saveTaskData() {
		sendDebugLogMsg(1,"I", "saveTaskData entered");
		
		if (isTaskDataExisted() && !messageListAdapter.resetDataChanged()) return; 
		
		SMBExplorerTaskDataHolder data = new SMBExplorerTaskDataHolder();

		data.msglist=messageListAdapter.getAllItem();
		
		data.local_tfl=localFileListAdapter.getDataList();
		if (remoteFileListAdapter!=null)
			data.remote_tfl=remoteFileListAdapter.getDataList();
		
		data.paste_list=pasteFromList;
		data.paste_from_url=pasteFromUrl;
//		data.paste_to_url=pasteToUrl;
		data.paste_item_list=pasteItemList;
		data.is_paste_copy=isPasteCopy;
		data.is_paste_enabled=isPasteEnabled;
		data.is_paste_from_local=isPasteFromLocal;
		
		data.msgPos=messageListView.getFirstVisiblePosition();
		if (messageListView.getChildAt(0)!=null) data.msgPosTop=messageListView.getChildAt(0).getTop();
		data.profPos=profileListView.getFirstVisiblePosition();
		if (profileListView.getChildAt(0)!=null) data.profPosTop=profileListView.getChildAt(0).getTop();
		data.lclPos=localFileListView.getFirstVisiblePosition();
		if (localFileListView.getChildAt(0)!=null) data.lclPosTop=localFileListView.getChildAt(0).getTop();
		data.remPos=remoteFileListView.getFirstVisiblePosition();
		if (remoteFileListView.getChildAt(0)!=null) data.remPosTop=remoteFileListView.getChildAt(0).getTop();

		
		try {
		    FileOutputStream fos = openFileOutput(SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(data);
		    oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", 
		    		"saveTaskData error, "+e.toString());
		}
	};
	
	private static final String SERIALIZABLE_FILE_NAME="Serial.dat";
	private void restoreTaskData() {
		sendDebugLogMsg(1,"I", "restoreTaskData entered");
		try {
		    File lf =
		    	new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
//		    FileInputStream fis = openFileInput(SMBSYNC_SERIALIZABLE_FILE_NAME);
		    FileInputStream fis = new FileInputStream(lf); 
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    SMBExplorerTaskDataHolder data = (SMBExplorerTaskDataHolder) ois.readObject();
		    ois.close();
		    lf.delete();
		    
		    ArrayList<MsglistItem> o_ml=new ArrayList<MsglistItem>(); 
			for (int i=0;i<messageListAdapter.getCount();i++) o_ml.add(messageListAdapter.getItem(i));
		    messageListAdapter.clear();
			messageListAdapter.setAllItem(data.msglist);
			messageListView.setAdapter(messageListAdapter);

			for (int i=0;i<o_ml.size();i++) messageListAdapter.add(o_ml.get(i));
			messageListAdapter.notifyDataSetChanged();
			messageListAdapter.resetDataChanged();
			
			localFileListAdapter =new TreeFilelistAdapter(currentContext);
			localFileListAdapter.setDataList(data.local_tfl);

			remoteFileListAdapter =new TreeFilelistAdapter(this);
			remoteFileListAdapter.setDataList(data.remote_tfl);
//			
			localFileListView.setAdapter(localFileListAdapter);
			remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
			remoteFileListDirBtn=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
			remoteFileListView.setAdapter(remoteFileListAdapter);
			
			pasteFromList=data.paste_list;
			pasteFromUrl=data.paste_from_url;
//			pasteToUrl=data.paste_to_url;
			pasteItemList=data.paste_item_list;
			isPasteCopy=data.is_paste_copy;
			isPasteEnabled=data.is_paste_enabled;
			isPasteFromLocal=data.is_paste_from_local;
			
			profileListView.setSelectionFromTop(data.profPos,data.profPosTop);
			messageListView.setSelectionFromTop(data.msgPos,data.msgPosTop);
			localFileListView.setSelectionFromTop(data.lclPos,data.lclPosTop);
			remoteFileListView.setSelectionFromTop(data.remPos,data.remPosTop);

		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", 
		    		"restoreTaskData error, "+e.toString());
		}
	};
	
	private boolean isTaskDataExisted() {
    	File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
	    return lf.exists();
	};

	private void deleteTaskData() {
		File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
	    lf.delete();
	}

	public class CustomTabContentView extends FrameLayout {  
        LayoutInflater inflater = (LayoutInflater) getApplicationContext()  
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
      
        public CustomTabContentView(Context context) {  
            super(context);  
        }  
        
        public CustomTabContentView(Context context, String title) {  
            this(context);  
            View childview1 = inflater.inflate(R.layout.tab_widget1, null);  
            TextView tv1 = (TextView) childview1.findViewById(R.id.tab_widget1_textview);  
            tv1.setText(title);  
            addView(childview1);  
       }  
    }
}
