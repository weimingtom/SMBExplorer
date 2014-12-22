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
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import static com.sentaroh.android.SMBExplorer.Constants.*;

import com.sentaroh.android.Utilities.*;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.DialogBackKeyListener;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class SMBExplorerMain extends ActionBarActivity {

	private final static String DEBUG_TAG = "SMBExplorer";

	private GlobalParameters mGp=null;
	
	private boolean enableKill = false;

	private String remoteBase = "", localBase = "";
	private String remoteDir = "", localDir = "";
	private ArrayList<String> remoteDirHist=new ArrayList<String>();
	private ArrayList<String> localDirHist=new ArrayList<String>();

	private ArrayList<FileListCacheItem> remoteFileListCache=new ArrayList<FileListCacheItem>();
	private FileListCacheItem remoteCurrFLI=null;
	private ArrayList<FileListCacheItem> localFileListCache=new ArrayList<FileListCacheItem>();
	private FileListCacheItem localCurrFLI=null;

	private ProfileListAdapter profileAdapter=null;
	private ListView profileListView=null;

	private ArrayList<FileListItem> movedFileListForRefresh=new ArrayList<FileListItem>();
	private boolean mIsMovedListAvailable=false;
	
	private FileListAdapter localFileListAdapter=null;
	private FileListAdapter remoteFileListAdapter=null;
	private ListView localFileListView=null;
	private ListView remoteFileListView=null;
	private String currentTabName="@P";
	private Spinner localFileListDirSpinner=null;
	private Spinner remoteFileListDirSpinner=null;

	private Button localFileListPathBtn=null;
	private TextView localFileListEmptyView=null;
	private Button localFileListUpBtn=null, localFileListTopBtn=null, 
			localFileListPasteBtn=null, localFileListCreateBtn=null,
			localFileListReloadBtn=null;

	private int dialogBackgroundColor=0xff111111;

	private Button remoteFileListPathBtn=null;
	private TextView remoteFileListEmptyView=null;
	private Button remoteFileListUpBtn=null;
	private Button remoteFileListTopBtn=null,
			remoteFileListPasteBtn=null, remoteFileListCreateBtn=null,
			remoteFileListReloadBtn=null;

	private ArrayList<FileIoLinkParm> fileioLinkParm=new ArrayList<FileIoLinkParm>();
	
	private TextView mLocalProgressMsg=null;
	private Button mLocalProgressCancel=null;
	private TextView mRemoteProgressMsg=null;
	private Button mRemoteProgressCancel=null;
	private OnClickListener mProgressOnClickListener=null;
	private LinearLayout mLocalProgressView=null;
	private LinearLayout mRemoteProgressView=null;
	private LinearLayout mLocalDialogView=null;
	private LinearLayout mRemoteDialogView=null;

	private OnClickListener mDialogOnClickListener=null;
	private TextView mLocalDialogMsg=null;
	private Button mLocalDialogCloseBtn=null;
	private TextView mRemoteDialogMsg=null;
	private Button mRemoteDialogCloseBtn=null;
	private String mDialogMsgCat="";
	
	private TabHost tabHost=null;
	
	private int restartStatus=0;
	
	private Context mContext=null;
	
	private String smbUser=null, smbPass=null;
	
	private CustomContextMenu ccMenu = null;
	
	private Activity mActivity=null;
	
	private ActionBar mActionBar=null;
	 
	private CommonDialog commonDlg=null;
	
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
	  super.onSaveInstanceState(outState);  
	  sendDebugLogMsg(1, "I", "onSaveInstanceState entered.");

	  outState.putString("remoteBase", remoteBase);
	  outState.putString("localBase", localBase);
	  outState.putString("remoteDir", remoteDir);
	  outState.putString("localDir", localDir);
	  outState.putString("currentTabName", currentTabName);
	  outState.putString("smbUser", smbUser);
	  outState.putString("smbPass", smbPass);
	};  
	  
	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState) {  
	  super.onRestoreInstanceState(savedInstanceState);
	  sendDebugLogMsg(1, "I", "onRestoreInstanceState entered.");
	  remoteBase=savedInstanceState.getString("remoteBase");
	  localBase=savedInstanceState.getString("localBase");
	  remoteDir=savedInstanceState.getString("remoteDir");
	  localDir=savedInstanceState.getString("localDir");
	  currentTabName=savedInstanceState.getString("currentTabName");
	  smbUser=savedInstanceState.getString("smbUser");
	  smbPass=savedInstanceState.getString("smbPass");
	  restartStatus=2;
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGp=(GlobalParameters)getApplication();
		mGp.loadSettingsParm();
		mContext=this;
		mActivity=this;
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
//		// Set up the action bar.
//		final ActionBar actionBar = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//		
//		OnTabListener otl=new OnTabListener();
//		actionBar.addTab(actionBar.newTab()
//				.setText("tab1")
//				.setTabListener(otl));
//		actionBar.addTab(actionBar.newTab()
//				.setText("tab2") 
//				.setTabListener(otl));
		
		mActionBar = getSupportActionBar();
		mActionBar.setHomeButtonEnabled(false);

		mGp.SMBExplorerRootDir=localBase=LocalMountPoint.getExternalStorageDir();
		
		if (ccMenu ==null) ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());

		commonDlg=new CommonDialog(mContext, getSupportFragmentManager());

		myUncaughtExceptionHandler.init();
		
//		localUrl = SMBEXPLORER_ROOT_DIR;
		createTabAndView() ;

		profileListView = (ListView) findViewById(R.id.explorer_profile_tab_listview);
		
		sendDebugLogMsg(1, "I", "onCreate entered");

		if (mGp.settingsUseRootPrivilege) {
			if (initSuCmdProcess()) {
				sendDebugLogMsg(1, "I", "su command process was created");
			} else {
				sendDebugLogMsg(1, "I", "su command process can not created");
			}
		}


		enableKill = false;
		
		initJcifsOption();
		
//		DocumentFile df=DocumentFile.fromFile(new File("/sdcard"));
//		Log.v("","df="+df.getName());
//		df.createDirectory("test");

//		Log.v("","getDataDirectory()="+Environment.getDataDirectory()+
//				", getDownloadCacheDirectory()="+Environment.getDownloadCacheDirectory()+
//				", getExternalStorageDirectory()="+Environment.getExternalStorageDirectory()+
//				", getExternalStoragePublicDirectory()="+Environment.getExternalStoragePublicDirectory("test")+
//				", getRootDirectory()="+Environment.getRootDirectory()
//				);
		
		ContextCompat.getExternalFilesDirs(mContext, null);
		
//		File[] fl= getExternalMediaDirs();
//		if (fl!=null) {
//			for(int i=0;i<fl.length;i++) Log.v("","n="+fl[i].getPath());
//		}

//		System.setProperty( "jcifs.netbios.retryTimeout", "200");
//		Log.v("","esd="+LocalMountPoint.getExternalStorageDir());
//		ArrayList<String>mpl=LocalMountPoint.getLocalMountPointList();
//		for (int i=0;i<mpl.size();i++) Log.v("","mp="+mpl.get(i));
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
		sendDebugLogMsg(1, "I","onResume entered"+ " restartStatus="+restartStatus);
		if (restartStatus==0) {
			profileAdapter = createProfileList(false,"");
			localFileListView=(ListView)findViewById(R.id.explorer_filelist_local_tab_listview);
			loadLocalFilelist(localBase,localDir);
			setEmptyFolderView();
			remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
			remoteFileListDirSpinner=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
		} else if (restartStatus==1) {

		} else if (restartStatus==2) {
			profileAdapter = createProfileList(false,"");
			profileListView.setAdapter(profileAdapter);
			restoreTaskData();
			setPasteItemList();
			setEmptyFolderView();
			if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tabHost.setCurrentTab(1);
			else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) tabHost.setCurrentTab(2);
		}
		restartStatus=1;
		deleteTaskData();
		
		setLocalDirBtnListener();
		setRemoteDirBtnListener();
		
		setProfilelistItemClickListener();
		setProfilelistLongClickListener();
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
		setRemoteFilelistItemClickListener();
		setRemoteFilelistLongClickListener();
		
		refreshOptionMenu();
	};


	@Override
	protected void onPause() {
		super.onPause();
		sendDebugLogMsg(1, "I","onPause entered, enableKill="+enableKill+
				", getChangingConfigurations="+String.format("0x%08x", getChangingConfigurations()));
		if (!enableKill) saveTaskData();
	};

	@Override
	protected void onStop() {
		super.onStop();
		sendDebugLogMsg(1, "I","onStop entered, enableKill="+enableKill);
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sendDebugLogMsg(1, "I","onDestroy entered, enableKill="+enableKill+
				", getChangingConfigurations="+String.format("0x%08x", getChangingConfigurations()));
		remoteFileListCache=null;
		if (enableKill) {
			deleteTaskData();
			terminateSuCmdProcess();
			if (mGp.settingsExitClean) {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
	};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    sendDebugLogMsg(1,"I","onConfigurationChanged Entered, "+"orientation="+newConfig.orientation);
	    
	    ViewSaveArea vsa=new ViewSaveArea();
	    saveViewStatus(vsa);
	    
		setContentView(R.layout.main);

		createTabAndView() ;

		restoreViewStatus(vsa);
		
		setPasteItemList();
		
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tabHost.setCurrentTab(1);
		else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) tabHost.setCurrentTab(2);
		
		setLocalDirBtnListener();
		setRemoteDirBtnListener();
		
		setProfilelistItemClickListener();
		setProfilelistLongClickListener();
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
		setRemoteFilelistItemClickListener();
		setRemoteFilelistLongClickListener();
		
		setEmptyFolderView();
		
		refreshOptionMenu();
	};
	
	private void saveViewStatus(ViewSaveArea vsa) {
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (mLocalProgressView.getVisibility()!=LinearLayout.GONE) {
				if (mLocalProgressView!=null) vsa.progressVisible=mLocalProgressView.getVisibility();
				if (mGp.progressCancelBtn!=null) vsa.progressCancelBtnText=mGp.progressCancelBtn.getText().toString();
				if (mGp.progressMsgView!=null) vsa.progressMsgText=mGp.progressMsgView.getText().toString();
			}
			if (mLocalDialogView.getVisibility()!=LinearLayout.GONE) {
				if (mLocalDialogView!=null) vsa.dialogVisible=mLocalDialogView.getVisibility();
				if (mGp.dialogMsgView!=null) vsa.dialogMsgText=mGp.dialogMsgView.getText().toString();
			}
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (mRemoteProgressView.getVisibility()!=LinearLayout.GONE) {
				if (mRemoteProgressView!=null) vsa.progressVisible=mRemoteProgressView.getVisibility();
				if (mGp.progressCancelBtn!=null) vsa.progressCancelBtnText=mGp.progressCancelBtn.getText().toString();
				if (mGp.progressMsgView!=null) vsa.progressMsgText=mGp.progressMsgView.getText().toString();
			}
			if (mRemoteDialogView.getVisibility()!=LinearLayout.GONE) {
				if (mRemoteDialogView!=null) vsa.dialogVisible=mRemoteDialogView.getVisibility();
				if (mGp.dialogMsgView!=null) vsa.dialogMsgText=mGp.dialogMsgView.getText().toString();
			}
		}
		vsa.profPos=profileListView.getFirstVisiblePosition();
		if (profileListView.getChildAt(0)!=null) vsa.profPosTop=profileListView.getChildAt(0).getTop();
		vsa.lclPos=localFileListView.getFirstVisiblePosition();
		if (localFileListView.getChildAt(0)!=null) vsa.lclPosTop=localFileListView.getChildAt(0).getTop();
		vsa.remPos=remoteFileListView.getFirstVisiblePosition();
		if (remoteFileListView.getChildAt(0)!=null) vsa.remPosTop=remoteFileListView.getChildAt(0).getTop();

	};
	
	private void restoreViewStatus(ViewSaveArea vsa) {
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (vsa.progressVisible!=LinearLayout.GONE) {
				showLocalProgressView();
				mGp.progressMsgView=mLocalProgressMsg;
				mGp.progressCancelBtn=mLocalProgressCancel;
				mGp.progressCancelBtn.setEnabled(true);
				mGp.progressCancelBtn.setOnClickListener(mProgressOnClickListener);
				mGp.progressCancelBtn.setText(vsa.progressCancelBtnText);
				mGp.progressMsgView.setText(vsa.progressMsgText);
			}
			if (vsa.dialogVisible!=LinearLayout.GONE) {
				showLocalDialogView();
				showDialogMsg(mDialogMsgCat,vsa.dialogMsgText,"");
			}
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (vsa.progressVisible!=LinearLayout.GONE) {
				showRemoteProgressView();
				mGp.progressMsgView=mRemoteProgressMsg;
				mGp.progressCancelBtn=mRemoteProgressCancel;
				mGp.progressCancelBtn.setEnabled(true);
				mGp.progressCancelBtn.setOnClickListener(mProgressOnClickListener);
				mGp.progressCancelBtn.setText(vsa.progressCancelBtnText);
				mGp.progressMsgView.setText(vsa.progressMsgText);
			}
			if (vsa.dialogVisible!=LinearLayout.GONE) {
				showRemoteDialogView();
				showDialogMsg(mDialogMsgCat,vsa.dialogMsgText,"");
			}
		}
		profileListView = (ListView) findViewById(R.id.explorer_profile_tab_listview);

		profileListView.setAdapter(profileAdapter);
		profileListView.setSelectionFromTop(vsa.profPos, vsa.profPosTop);
		
		localFileListView.setAdapter(localFileListAdapter);
		localFileListView.setSelectionFromTop(vsa.lclPos, vsa.lclPosTop);

		remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
		remoteFileListDirSpinner=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
		remoteFileListView.setAdapter(remoteFileListAdapter);
		remoteFileListView.setSelectionFromTop(vsa.remPos, vsa.remPosTop);
	};
	
	private void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT>=11)
			mActivity.invalidateOptionsMenu();
	};

	private boolean initSuCmdProcess() {
		boolean su_available=false;
		if (mGp.settingsUseRootPrivilege) {
			Process p=null;
			try {
				p=Runtime.getRuntime().exec("su");
				p.getOutputStream().write(new String("id"+"\n").getBytes());
				p.getOutputStream().flush();
				byte[] buffer = new byte[1024];
				String ret="";
				int bc=0;
				bc=p.getInputStream().read(buffer);
				if (bc>0) ret=new String(buffer,0,bc,"UTF-8");
				if (ret.length()>0) su_available=true;
//				Log.v("","ret="+ret);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (su_available) {
				mGp.mSuCmdProcess=p;
				sendDebugLogMsg(1, "I","root ptivilege granted");
			} else {
				sendDebugLogMsg(1, "I","root ptivilege not granted");
			}
		}
		return su_available;
	};
	
	private void terminateSuCmdProcess() {
		if (mGp.mSuCmdProcess!=null) {
			try {
				mGp.mSuCmdProcess.getOutputStream().write(new String("exit"+"\n").getBytes());
				mGp.mSuCmdProcess.getOutputStream().flush();
				mGp.mSuCmdProcess.waitFor();
				mGp.mSuCmdProcess.getOutputStream().close();
				mGp.mSuCmdProcess.getInputStream().close();
				mGp.mSuCmdProcess.destroy();
				mGp.mSuCmdProcess=null;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};

//	private void executeSuCmd(String cmd) {
//		try {
//			mGp.mSuCmdProcess.getOutputStream().write(new String(cmd+"\n").getBytes());
//			mGp.mSuCmdProcess.getOutputStream().flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	};

	private void createTabAndView() {
		tabHost=(TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();
		
		View childview2 = new CustomTabContentView(this, "Profile");
		TabHost.TabSpec tabSpec1=tabHost.newTabSpec("@P")
				.setIndicator(childview2)
				.setContent(R.id.explorer_profile_tab);
		tabHost.addTab(tabSpec1);
		
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
		
		localFileListDirSpinner=(Spinner)findViewById(R.id.explorer_filelist_local_tab_dir);
		remoteFileListDirSpinner=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
		
		localFileListView=(ListView)findViewById(R.id.explorer_filelist_local_tab_listview);
		remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
		localFileListEmptyView=(TextView)findViewById(R.id.explorer_filelist_local_empty_view);
		remoteFileListEmptyView=(TextView)findViewById(R.id.explorer_filelist_remote_empty_view);
		localFileListPathBtn=(Button)findViewById(R.id.explorer_filelist_local_filepath);
		localFileListUpBtn=(Button)findViewById(R.id.explorer_filelist_local_up_btn);
		localFileListTopBtn=(Button)findViewById(R.id.explorer_filelist_local_top_btn);
		localFileListPasteBtn=(Button)findViewById(R.id.explorer_filelist_local_paste_btn);
		localFileListCreateBtn=(Button)findViewById(R.id.explorer_filelist_local_create_btn);
		localFileListReloadBtn=(Button)findViewById(R.id.explorer_filelist_local_reload_btn);

		remoteFileListPathBtn=(Button)findViewById(R.id.explorer_filelist_remote_filepath);
		remoteFileListUpBtn=(Button)findViewById(R.id.explorer_filelist_remote_up_btn);
		remoteFileListTopBtn=(Button)findViewById(R.id.explorer_filelist_remote_top_btn);
		remoteFileListPasteBtn=(Button)findViewById(R.id.explorer_filelist_remote_paste_btn);
		remoteFileListCreateBtn=(Button)findViewById(R.id.explorer_filelist_remote_create_btn);
		remoteFileListReloadBtn=(Button)findViewById(R.id.explorer_filelist_remote_reload_btn);

		mLocalProgressMsg=(TextView)findViewById(R.id.explorer_filelist_local_progress_msg);
		mLocalProgressCancel=(Button)findViewById(R.id.explorer_filelist_local_progress_cancel);
		mRemoteProgressMsg=(TextView)findViewById(R.id.explorer_filelist_remote_progress_msg);
		mRemoteProgressCancel=(Button)findViewById(R.id.explorer_filelist_remote_progress_cancel);
		mGp.progressMsgView=null;
		mGp.progressCancelBtn=null; 
		mGp.dialogMsgView=null;

		mLocalProgressView=(LinearLayout)findViewById(R.id.explorer_filelist_local_progress);
		mRemoteProgressView=(LinearLayout)findViewById(R.id.explorer_filelist_remote_progress);
		mLocalDialogView=(LinearLayout)findViewById(R.id.explorer_filelist_local_dialog);
		mRemoteDialogView=(LinearLayout)findViewById(R.id.explorer_filelist_remote_dialog);

		mLocalDialogMsg=(TextView)findViewById(R.id.explorer_filelist_local_dialog_msg);
		mLocalDialogCloseBtn=(Button)findViewById(R.id.explorer_filelist_local_dialog_close);
		mRemoteDialogMsg=(TextView)findViewById(R.id.explorer_filelist_remote_dialog_msg);
		mRemoteDialogCloseBtn=(Button)findViewById(R.id.explorer_filelist_remote_dialog_close);
		
		if (Build.VERSION.SDK_INT==10) dialogBackgroundColor=Color.BLACK;
		else if (Build.VERSION.SDK_INT==19) dialogBackgroundColor=0xff181818; //4.4
		else if (Build.VERSION.SDK_INT==18) dialogBackgroundColor=0xff101010; //4.3
		else dialogBackgroundColor=0xff101010; //Other

		setPasteButtonEnabled();
	};

	class OnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			sendDebugLogMsg(1, "I","onTabchanged entered. tab="+tabId);
			currentTabName=tabId;
			setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
			setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
			setPasteButtonEnabled();
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
    	if (isUiEnabled()) {
    		menu.findItem(R.id.menu_top_export).setEnabled(true);
    		menu.findItem(R.id.menu_top_import).setEnabled(true);
    		menu.findItem(R.id.menu_top_settings).setEnabled(true);
    	} else {
    		menu.findItem(R.id.menu_top_export).setEnabled(false);
    		menu.findItem(R.id.menu_top_import).setEnabled(false);
    		menu.findItem(R.id.menu_top_settings).setEnabled(false);
    	}
        return true;
    };
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		sendDebugLogMsg(1, "I","onOptionsItemSelected entered");
		switch (item.getItemId()) {
//			case R.id.menu_top_refresh:
//				reloadFilelistView();
//				return true;
//			case R.id.menu_top_paste:
//				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
//					pasteItem(localFileListAdapter,localUrl);
//				else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
//					pasteItem(remoteFileListAdapter,remoteUrl);
//				return true;
			case R.id.menu_top_export:
				exportProfileDlg(mGp.SMBExplorerRootDir, SMBEXPLORER_PROFILE_NAME);
				return true;
			case R.id.menu_top_import:
				importProfileDlg(mGp.SMBExplorerRootDir, SMBEXPLORER_PROFILE_NAME);
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
					if (closeLastLevel()) {
						switchToHome();
//						confirmTerminateApplication();
					}
				} else {
					if (closeLastLevel()) {
						switchToHome();
//						confirmTerminateApplication();
					}
				}
			} else{
				switchToHome();
//				confirmTerminateApplication();
			}
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};

	private boolean closeLastLevel() {
		boolean result=false;
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (localDir.equals("")) {
				result=true;
			} else {
//				localFileListUpBtn.performClick();
				processLocalUpButton();
			}
		} else {
			if (remoteDir.equals("")) {
				result=true;
			} else {
//				remoteFileListUpBtn.performClick();
				processRemoteUpButton();
			}
		}
		return result;
	};

	private void confirmTerminateApplication() {
		NotifyEvent ne=new NotifyEvent(this);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				terminateApplication();
			}
	
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_terminate_confirm),"",ne);
		return;
	}

	private void switchToHome() {
		Intent in=new Intent();
		in.setAction(Intent.ACTION_MAIN);
		in.addCategory(Intent.CATEGORY_HOME);
		in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(in);
	};
	
	private void terminateApplication() {
			enableKill = true; // exit cleanly
	//		moveTaskToBack(true);
			finish();
		}

	private void applySettingParms() {
		
		mGp.loadSettingsParm();
		
		initJcifsOption();
	};
	
	private boolean mUiEnabled=true;
	private boolean isUiEnabled() {
		return mUiEnabled;
	};

	private void setUiEnabled(boolean enabled) {
//		Log.v("","enabled="+enabled);
//		Thread.dumpStack();
		if ((enabled && mUiEnabled) || (!enabled && !mUiEnabled)) return;  
		mUiEnabled=enabled;
		TabWidget tw=(TabWidget)findViewById(android.R.id.tabs);
		Spinner sp_local=(Spinner)findViewById(R.id.explorer_filelist_local_tab_dir);
		Spinner sp_remote=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);

		if (enabled) {
			tw.setEnabled(true);
			sp_local.setEnabled(true);
			sp_remote.setEnabled(true);
			remoteFileListView.setEnabled(true);
			localFileListView.setEnabled(true);
			remoteFileListView.setVisibility(ListView.VISIBLE);
			localFileListView.setVisibility(ListView.VISIBLE);
			
		} else {
			tw.setEnabled(false);
			sp_local.setEnabled(false);
			sp_remote.setEnabled(false);
			remoteFileListView.setEnabled(false);
			localFileListView.setEnabled(false);
			remoteFileListView.setVisibility(ListView.INVISIBLE);
			localFileListView.setVisibility(ListView.INVISIBLE);
		}
		localFileListUpBtn.setClickable(enabled);
		localFileListTopBtn.setClickable(enabled);
		localFileListCreateBtn.setEnabled(enabled);
		localFileListReloadBtn.setEnabled(enabled);
		remoteFileListUpBtn.setClickable(enabled);
		remoteFileListTopBtn.setClickable(enabled);
		remoteFileListCreateBtn.setEnabled(enabled);
		remoteFileListReloadBtn.setEnabled(enabled);

		setPasteButtonEnabled();
		
		refreshOptionMenu();
	};

	private void invokeSettingsActivity() {
		Intent intent = new Intent(this, SMBExpolorerSettings.class);
		startActivityForResult(intent,0);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		applySettingParms();
	};
	
	private void refreshFilelistView() {
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			if (!localBase.equals("")) {
//				refreshLocalFileList();
				int fv=0;
				int top=0;
				if (localFileListView.getChildAt(0)!=null) {
					fv=localFileListView.getFirstVisiblePosition();
					top=localFileListView.getChildAt(0).getTop();
				}
				setLocalDirBtnListener();
				String t_dir=buildFullPath(localBase,localDir);
				removeFileListCache(t_dir, localFileListCache);
				loadLocalFilelist(localBase,localDir);
				localFileListView.setSelectionFromTop(fv, top);
				setEmptyFolderView();
				updateFileListCacheByMove();
				removeDeletedFileListCache(localCurrFLI, localFileListCache);
			}
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (!remoteBase.equals("")) {
				refreshRemoteFileList(smbUser,smbPass);
			}
		} else return; //file list not selected
	};

	private void removeDeletedFileListCache(FileListCacheItem c_fci, ArrayList<FileListCacheItem>u_fcl) {
		ArrayList<FileListItem>c_fl=c_fci.file_list;
		for (int i=u_fcl.size()-1;i>=0;i--) {
			FileListCacheItem u_fci=u_fcl.get(i);
			String u_path="";
			u_path=u_fci.directory;
			if (!u_path.equals(u_fci.base)) {
//				Log.v("","c_fci_d="+c_fci.directory+", u_fci_d="+u_fci.directory);
				if (c_fci.directory.compareToIgnoreCase(u_fci.directory)<0) {
					boolean found=false;
					for (int j=0;j<c_fl.size();j++) {
						FileListItem c_fli=c_fl.get(j);
						String c_path="";
						c_path=c_fli.getPath()+"/"+c_fli.getName();
//						Log.v("","u_path="+u_path+", c_path="+c_path);
						if (c_path.equals(u_path)) {
							found=true;
							break;
						}
					}
					if (!found) {
						u_fcl.remove(i);
//						Log.v("","deleted="+u_path);
					}
				}
			}
		}
	};
	
	private void updateFileListCacheByMove() {
		if (mIsMovedListAvailable) {
			mIsMovedListAvailable=false;
			if (movedFileListForRefresh.get(0).getPath().startsWith("smb:")) {
				for (int i=0;i<remoteFileListCache.size();i++) {
					ArrayList<FileListItem> fl=remoteFileListCache.get(i).file_list;
					for (int j=fl.size()-1;j>=0;j--) {
						FileListItem fli=fl.get(j);
						if (findFileListItem(movedFileListForRefresh,fli))
							fl.remove(j);
					}
				}
			} else {
				for (int i=0;i<localFileListCache.size();i++) {
					ArrayList<FileListItem> fl=localFileListCache.get(i).file_list;
					for (int j=fl.size()-1;j>=0;j--) {
						FileListItem fli=fl.get(j);
						if (findFileListItem(movedFileListForRefresh,fli))
							fl.remove(j);
					}
				}
			}
			localFileListAdapter.notifyDataSetChanged();
			if (remoteFileListAdapter!=null) remoteFileListAdapter.notifyDataSetChanged();
		}
	}
	
	private boolean findFileListItem(ArrayList<FileListItem>list, FileListItem fli) {
		boolean result=false;
		for (int i=0;i<list.size();i++) {
			FileListItem l_fli=list.get(i);
			String l_path=l_fli.getPath()+"/"+l_fli.getName();
			String s_path=fli.getPath()+"/"+fli.getName();
			if (l_path.equals(s_path)) result=true;
		}
		return result;
	};
	
	private void reloadFilelistView() {
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			int fv=0;
			int top=0;
			if (localFileListView.getChildAt(0)!=null) {
				fv=localFileListView.getFirstVisiblePosition();
				top=localFileListView.getChildAt(0).getTop();
			}
			setLocalDirBtnListener();
			String t_dir=buildFullPath(localBase,localDir);
			removeFileListCache(t_dir, localFileListCache);
			loadLocalFilelist(localBase,localDir);
			localFileListView.setSelectionFromTop(fv, top);
			setEmptyFolderView();
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			if (!remoteBase.equals("")) {
				removeFileListCache(buildFullPath(remoteBase,remoteDir), remoteFileListCache);
				loadRemoteFilelist(remoteBase, remoteDir);
			}
		} else return; //file list not selected
	};

	private void loadLocalFilelist(String base, String dir) {
		String t_dir=buildFullPath(localBase,localDir);
		tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
		FileListCacheItem dhi=getFileListCache(t_dir, localFileListCache);
		ArrayList<FileListItem> tfl=null;
		if (dhi==null) {
			tfl = createLocalFileList(false,t_dir);
			if (tfl==null) return;
			putDirHist(localBase, localDir, localDirHist);
			dhi=new FileListCacheItem();
			dhi.profile_name=base;
			dhi.base=localBase;
			dhi.directory=t_dir;
			dhi.file_list=tfl;
			dhi.directory_history.addAll(localDirHist);
			putFileListCache(dhi,localFileListCache);
			localCurrFLI=dhi;
		} else {
			tfl=dhi.file_list;
			localCurrFLI=dhi;
		}
		
		localFileListAdapter=new FileListAdapter(this);
		localFileListAdapter.setShowLastModified(true);
		localFileListAdapter.setDataList(tfl);
		localFileListView.setAdapter(localFileListAdapter);
		
		if (dir.equals("")) {
			localFileListTopBtn.setEnabled(false);
			localFileListUpBtn.setEnabled(false);
		} else {
			localFileListTopBtn.setEnabled(true);
			localFileListUpBtn.setEnabled(true);
		}
		
		tabHost.getTabWidget().getChildTabViewAt(1).setEnabled(true);
		
		setFilelistCurrDir(localFileListDirSpinner,base,dir);
		setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
		setLocalFilelistItemClickListener();
		setLocalFilelistLongClickListener();
	};
	
	private void loadRemoteFilelist(final String url, final String dir) {
		final String t_dir=buildFullPath(remoteBase,remoteDir);
		NotifyEvent ne=new NotifyEvent(mContext);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				remoteFileListAdapter = (FileListAdapter)o[0];
				remoteFileListView.setAdapter(remoteFileListAdapter);
				setRemoteFilelistItemClickListener();
				setRemoteFilelistLongClickListener();
				setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
				setEmptyFolderView();
				putDirHist(remoteBase, remoteDir, remoteDirHist);

				FileListCacheItem dhi=new FileListCacheItem();
				dhi.profile_name=remoteFileListDirSpinner.getSelectedItem().toString();
				dhi.base=remoteBase;
				dhi.directory=t_dir;
				dhi.file_list=remoteFileListAdapter.getDataList();
				dhi.directory_history.addAll(remoteDirHist);
				putFileListCache(dhi,remoteFileListCache);
				remoteCurrFLI=dhi;
				
				if (dir.equals("")) {
					remoteFileListTopBtn.setEnabled(false);
					remoteFileListUpBtn.setEnabled(false);
				} else {
					remoteFileListTopBtn.setEnabled(true);
					remoteFileListUpBtn.setEnabled(true);
				}
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				remoteFileListAdapter = new FileListAdapter(mContext);
				remoteFileListView.setAdapter(remoteFileListAdapter);
				setRemoteFilelistItemClickListener();
				setRemoteFilelistLongClickListener();
				setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
				setEmptyFolderView();
			}
		});
		
		FileListCacheItem dhi=getFileListCache(t_dir, remoteFileListCache);
		if (dhi==null) createRemoteFileList(url+"/"+dir+"/",ne);
		else {
			remoteFileListAdapter.setDataList(dhi.file_list);
			remoteFileListAdapter.notifyDataSetChanged();
			setRemoteFilelistItemClickListener();
			setRemoteFilelistLongClickListener();
			setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
			setEmptyFolderView();
			remoteCurrFLI=dhi;
			
			if (dir.equals("")) {
				remoteFileListTopBtn.setEnabled(false);
				remoteFileListUpBtn.setEnabled(false);
			} else {
				remoteFileListTopBtn.setEnabled(true);
				remoteFileListUpBtn.setEnabled(true);
			}

		}
	};
	
	
	private boolean mIgnoreSpinnerSelection=false;
	private void setLocalDirBtnListener() {
        Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_local_tab_dir);
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(this, R.layout.custom_simple_spinner_item);
        adapter.setTextColor(Color.BLACK);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt("ローカルの選択");
        spinner.setAdapter(adapter);

        int a_no=0;
        List<ProfileListItem>pl=createLocalProfileEntry();
        for (int i=0;i<pl.size();i++) { 
			adapter.add(pl.get(i).getName());
			if (pl.get(i).getName().equals(localBase))
		        spinner.setSelection(a_no);
			a_no++;
		}
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
            	if (mIgnoreSpinnerSelection) return;
                Spinner spinner = (Spinner) parent;
				String turl=(String) spinner.getSelectedItem();
				if (turl.equals(localBase)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
				else {
					localDir="";
					localBase=turl;
					
					clearDirHist(localDirHist);
//					putDirHist(localBase, localDir, localDirHist);
					
					localCurrFLI.pos_fv=localFileListView.getFirstVisiblePosition();
					if (localFileListView.getChildAt(0)!=null)
						localCurrFLI.pos_top=localFileListView.getChildAt(0).getTop();
					
					loadLocalFilelist(localBase, localDir);
					localFileListView.setSelection(0);
					for (int j = 0; j < localFileListView.getChildCount(); j++)
						localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
					setEmptyFolderView();
				}
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        localFileListPathBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (localFileListCache.size()>0)
					showFileListCache(localFileListCache, localFileListAdapter,localFileListView,localFileListDirSpinner);
			}
        });
        
        localFileListUpBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				processLocalUpButton();
			}
        });
        
        localFileListTopBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				processLocalTopButton();
			}
        });
        
		localFileListPasteBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String to_dir="";
				if (localDir.equals("")) to_dir=localBase;
				else to_dir=localBase+"/"+localDir;
				pasteItem(localFileListAdapter, to_dir, localBase);
			}
        });
		localFileListReloadBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setLocalDirBtnListener();
				reloadFilelistView();
				setEmptyFolderView();
			}
        });
		localFileListCreateBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
					createItem(localFileListAdapter,"C", localBase+"/"+localDir);
				else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
					createItem(remoteFileListAdapter,"C", remoteBase+"/"+remoteDir);
			}
        });
        
	};

	private void showFileListCache(
			final ArrayList<FileListCacheItem>fcl, 
			final FileListAdapter fla, final ListView flv, final Spinner spinner) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.select_file_cache_dlg);
//		final Button btnOk = 
//				(Button) dialog.findViewById(R.id.select_file_cache_dlg_ok);
		final Button btnCancel = 
				(Button) dialog.findViewById(R.id.select_file_cache_dlg_cancel);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		((TextView)dialog.findViewById(R.id.select_file_cache_dlg_title))
			.setText("Select file cache");

		ListView lv=(ListView)dialog.findViewById(R.id.select_file_cache_dlg_listview);
		
		final ArrayList<String> list=new ArrayList<String>();
		for (int i=0;i<fcl.size();i++) {
			list.add(fcl.get(i).directory);
		}
		Collections.sort(list,new Comparator<String>(){
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.compareToIgnoreCase(rhs);
			}
		});
		ArrayAdapter<String> adapter=new ArrayAdapter<String>(mContext, R.layout.simple_list_item_1o, list);
		lv.setAdapter(adapter);
		
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String t_dir=list.get(position);
				FileListCacheItem dhi=getFileListCache(t_dir, fcl);
				if (dhi!=null) {
					mIgnoreSpinnerSelection=true;
					int s_no=-1;
					for (int i=0;i<spinner.getCount();i++) {
						if (spinner.getItemAtPosition(i).toString().equals(dhi.profile_name)) {
							s_no=i;
							break;
						}
					}
					fla.setDataList(dhi.file_list);
					fla.notifyDataSetChanged();
					if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
						localBase=dhi.base;
						if (dhi.base.equals(dhi.directory)) localDir="";
						else localDir=dhi.directory.replace(localBase+"/", "");
						replaceDirHist(localDirHist,dhi.directory_history);
						setFilelistCurrDir(localFileListDirSpinner,localBase, localDir);
						setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
						setEmptyFolderView();
						localCurrFLI=dhi;
						flv.setSelection(0);
						for (int j = 0; j < localFileListView.getChildCount(); j++)
							localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
						localFileListReloadBtn.performClick();
					} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
						remoteBase=dhi.base;
						if (dhi.base.equals(dhi.directory)) remoteDir="";
						else remoteDir=dhi.directory.replace(remoteBase+"/", "");
						replaceDirHist(remoteDirHist,dhi.directory_history);
						setFilelistCurrDir(remoteFileListDirSpinner,remoteBase, remoteDir);
						setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
						setEmptyFolderView();
						remoteCurrFLI=dhi;
						flv.setSelection(0);
						
//						Log.v("","base="+remoteBase+", dir="+remoteDir+", histsz="+remoteDirHist.size());
						for (int j = 0; j < remoteFileListView.getChildCount(); j++)
							remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
					}
						
					if (s_no!=-1) spinner.setSelection(s_no);
					Handler hndl=new Handler();
					hndl.postDelayed(new Runnable(){
						@Override
						public void run() {
							mIgnoreSpinnerSelection=false;
						}
					},100);
					dialog.dismiss();
				}
			}
		});
		
		btnCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	}
	
	private void processLocalUpButton() {
//		Log.v("","size1="+localDirHist.size());
//		for(int i=0;i<localDirHist.size();i++) Log.v("","hist="+localDirHist.get(i));
		if (localDirHist.size()>0) {
			localCurrFLI.pos_fv=localFileListView.getFirstVisiblePosition();
			if (localFileListView.getChildAt(0)!=null) {
				localCurrFLI.pos_top=localFileListView.getChildAt(0).getTop();
			}
			if (localDirHist.size()<=2) {
				processLocalTopButton();
				localFileListUpBtn.setEnabled(false);
			} else {
//				String c_dir=remoteDirHist.get(remoteDirHist.size()-1);
				removeDirHist(localBase,localDir,localDirHist);
//				Log.v("","size2="+localDirHist.size());
				String t_dir=getLastDirHist(localDirHist);
				FileListCacheItem dhi=getFileListCache(t_dir,localFileListCache);
				localBase=dhi.base;
				localDir=dhi.directory.replace(dhi.base+"/", "");
				localFileListAdapter.setDataList(dhi.file_list);
				localFileListAdapter.notifyDataSetChanged();
				setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
				localFileListView.setSelectionFromTop(dhi.pos_fv, dhi.pos_top);
				setEmptyFolderView();
				localCurrFLI=dhi;
				for (int j = 0; j < localFileListView.getChildCount(); j++)
					localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
			}
		}
	};

	private void processLocalTopButton() {
		if (localDirHist.size()>0) {
			localCurrFLI.pos_fv=localFileListView.getFirstVisiblePosition();
			if (localFileListView.getChildAt(0)!=null)
				localCurrFLI.pos_top=localFileListView.getChildAt(0).getTop();

			String t_dir=getTopDirHist(localDirHist);
			FileListCacheItem dhi=getFileListCache(t_dir,localFileListCache);
			localDir="";
			localBase=dhi.base;
			localFileListAdapter.setDataList(dhi.file_list);
			clearDirHist(localDirHist);
			putDirHist(t_dir,"",localDirHist);
			localFileListAdapter.notifyDataSetChanged();
			setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
			localFileListView.setSelectionFromTop(dhi.pos_fv, dhi.pos_top);
			setEmptyFolderView();
			localCurrFLI=dhi;
			for (int j = 0; j < localFileListView.getChildCount(); j++)
				localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
			localFileListTopBtn.setEnabled(false);
		}
	};

	private void setEmptyFolderView() {
		if (localFileListAdapter!=null) {
			if (localFileListAdapter.getCount()>0) {
				localFileListEmptyView.setVisibility(TextView.GONE);
				localFileListView.setVisibility(ListView.VISIBLE);
			} else {
				localFileListEmptyView.setVisibility(TextView.VISIBLE);
				localFileListView.setVisibility(ListView.GONE);
			}
		} else {
			localFileListEmptyView.setVisibility(TextView.VISIBLE);
			localFileListView.setVisibility(ListView.GONE);
		}
		if (remoteFileListAdapter!=null) {
			if (remoteFileListAdapter.getCount()>0) {
				remoteFileListEmptyView.setVisibility(TextView.GONE);
				remoteFileListView.setVisibility(ListView.VISIBLE);
			} else {
				remoteFileListEmptyView.setVisibility(TextView.VISIBLE);
				remoteFileListView.setVisibility(ListView.GONE);
			}
		} else {
			remoteFileListEmptyView.setVisibility(TextView.VISIBLE);
			remoteFileListView.setVisibility(ListView.GONE);
		}
	};
	
	private void setRemoteDirBtnListener() {
        final Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_remote_tab_dir);
        final CustomSpinnerAdapter spAdapter = new CustomSpinnerAdapter(this, R.layout.custom_simple_spinner_item);
        spAdapter.setTextColor(Color.BLACK);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt("リモートの選択");
        spinner.setAdapter(spAdapter);
//        mIgnoreRemoteSelection=true;
		if (remoteBase.equals("")) spAdapter.add("--- Not selected ---");
		int a_no=0;
		for (int i=0;i<profileAdapter.getCount();i++) 
			if (profileAdapter.getItem(i).getType().equals("R") && 
					profileAdapter.getItem(i).getActive().equals("A")) {
				spAdapter.add(profileAdapter.getItem(i).getName());
				String surl=buildRemoteBase(profileAdapter.getItem(i));
				if (surl.equals(remoteBase))
			        spinner.setSelection(a_no);
				a_no++;
			}
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
            	if (mIgnoreSpinnerSelection) {
//            		Log.v("","ignored");
            		return;
            	}
//            	mIgnoreRemoteSelection=false;
                Spinner spinner = (Spinner) parent;
                if (((String)spinner.getSelectedItem()).startsWith("---")) {
                	return;
                }
                String sel_item=(String)spinner.getSelectedItem();
                if (spAdapter.getItem(0).startsWith("---")) {
                	spAdapter.remove(spAdapter.getItem(0));
                	spinner.setSelection(position-1);
                }
                
				ProfileListItem pli=null;
				for (int i=0;i<profileAdapter.getCount();i++) {
					if (profileAdapter.getItem(i).getName()
							.equals(sel_item)) {
						pli=profileAdapter.getItem(i);
					}
				}
				String turl=buildRemoteBase(pli);
				if (turl.equals(remoteBase)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
				else {
					tabHost.getTabWidget().getChildTabViewAt(2).setEnabled(true);
					tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
					setJcifsProperties(pli.getUser(), pli.getPass());
					remoteBase = turl;
					remoteDir="";
					
					clearDirHist(remoteDirHist);
//					putDirHist(remoteBase, remoteDir, remoteDirHist);
					
					if (remoteCurrFLI!=null) {
						remoteCurrFLI.pos_fv=remoteFileListView.getFirstVisiblePosition();
						if (remoteFileListView.getChildAt(0)!=null)
							remoteCurrFLI.pos_top=remoteFileListView.getChildAt(0).getTop();
					}
					
					loadRemoteFilelist(remoteBase, remoteDir);
					remoteFileListView.setSelection(0);
					for (int j = 0; j < remoteFileListView.getChildCount(); j++)
						remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
				}
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        remoteFileListUpBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				processRemoteUpButton();
			}
        });
        remoteFileListPathBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (remoteFileListCache.size()>0)
					showFileListCache(remoteFileListCache, remoteFileListAdapter, remoteFileListView,remoteFileListDirSpinner);
			}
        });
        
        remoteFileListTopBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				processRemoteTopButton();
			}
        });
        
        remoteFileListPasteBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String to_dir="";
				if (remoteDir.equals("")) to_dir=remoteBase;
				else to_dir=remoteBase+"/"+remoteDir;
				pasteItem(remoteFileListAdapter, to_dir, remoteBase);
			}
        });
        remoteFileListReloadBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				reloadFilelistView();
				setEmptyFolderView();
			}
        });
        remoteFileListCreateBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
					createItem(localFileListAdapter,"C", localBase+"/"+localDir);
				else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
					createItem(remoteFileListAdapter,"C", remoteBase+"/"+remoteDir);
			}
        });

	};
	
	private void processRemoteUpButton() {
		if (remoteDirHist.size()>0) {
			if (remoteCurrFLI!=null) {
				remoteCurrFLI.pos_fv=remoteFileListView.getFirstVisiblePosition();
				if (remoteFileListView.getChildAt(0)!=null) remoteCurrFLI.pos_top=remoteFileListView.getChildAt(0).getTop();
			}
			if (remoteDirHist.size()<=2) {
				processRemoteTopButton();
				remoteFileListUpBtn.setEnabled(false);
			} else {
//				String c_dir=remoteDirHist.get(remoteDirHist.size()-1);
				removeDirHist(remoteBase,remoteDir,remoteDirHist);
				String t_dir=getLastDirHist(remoteDirHist);
				FileListCacheItem dhi=getFileListCache(t_dir,remoteFileListCache);
				remoteBase=dhi.base;
				remoteDir=dhi.directory.replace(dhi.base+"/", "");
				remoteFileListAdapter.setDataList(dhi.file_list);
				remoteFileListAdapter.notifyDataSetChanged();
				setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
				remoteFileListView.setSelectionFromTop(dhi.pos_fv, dhi.pos_top);
				setEmptyFolderView();
				remoteCurrFLI=dhi;
				for (int j = 0; j < remoteFileListView.getChildCount(); j++)
					remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
			}
		}
	};

	private void processRemoteTopButton() {
		if (remoteDirHist.size()>0) {
			if (remoteCurrFLI!=null) {
				remoteCurrFLI.pos_fv=remoteFileListView.getFirstVisiblePosition();
				if (remoteFileListView.getChildAt(0)!=null) remoteCurrFLI.pos_top=remoteFileListView.getChildAt(0).getTop();
			}
			String t_dir=getTopDirHist(remoteDirHist);
			FileListCacheItem dhi=getFileListCache(t_dir,remoteFileListCache);
			remoteDir="";
			remoteBase=dhi.base;
			remoteFileListAdapter.setDataList(dhi.file_list);
			clearDirHist(remoteDirHist);
			putDirHist(t_dir,"",remoteDirHist);
			remoteFileListAdapter.notifyDataSetChanged();
			setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
			remoteFileListView.setSelectionFromTop(dhi.pos_fv, dhi.pos_top);
			setEmptyFolderView();
			remoteCurrFLI=dhi;
			for (int j = 0; j < remoteFileListView.getChildCount(); j++)
				remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
			remoteFileListTopBtn.setEnabled(false);
		}
	};
	
	private String buildRemoteBase(ProfileListItem pli) {
		String url="", sep="";
		if (!pli.getPort().equals("")) sep=":";
		url = "smb://"+pli.getAddr()+sep+pli.getPort()+"/"+pli.getShare() ;
		return url;
	};

	private void setProfilelistItemClickListener() {
		profileListView
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			ProfileListItem item = profileAdapter.getItem(position);
			sendDebugLogMsg(1,"I","Profilelist item Clicked :" + item.getName());
			
			if (item.getActive().equals("A")) { // profile is active
				if (item.getType().equals("R")) {
					String turl="smb://" + item.getAddr() + "/"+ item.getShare() ;
					if (turl.equals(remoteBase)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
					else {	
						tabHost.getTabWidget().getChildTabViewAt(2).setEnabled(true);
						tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_REMOTE);
						setJcifsProperties(item.getUser(), item.getPass());
						Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_remote_tab_dir);
						for (int i=0;i<spinner.getCount();i++) {
							if (spinner.getItemAtPosition(i).toString().equals(item.getName())) {
								spinner.setSelection(i);
								break;
							}
						}
						
					}
				} else {
					String turl=item.getName();
					if (turl.equals(localBase)) tabHost.setCurrentTabByTag(SMBEXPLORER_TAB_LOCAL);
					else {
						localBase=turl;
						localDir="";
						loadLocalFilelist(localBase, localDir);
						
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
				for (int j = 0; j < parent.getChildCount(); j++)
	                parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
				if (!isUiEnabled()) return;
				setUiEnabled(false);
				FileListItem item = localFileListAdapter.getItem(position);
				sendDebugLogMsg(1,"I","Local filelist item clicked :" + item.getName());
				if (item.isDir()) {
//					if (item.getSubDirItemCount()==0) return;
					FileListCacheItem dhi_c=getFileListCache(item.getPath()+"/"+item.getName(), localFileListCache);
					ArrayList<FileListItem> tfl=null;
					if (dhi_c==null) {
						tfl=createLocalFileList(false,item.getPath()+"/"+item.getName());
					} else {
						tfl=dhi_c.file_list;
					}
					if (tfl==null) return;
					String t_dir=item.getPath()+"/"+item.getName();
					localCurrFLI.pos_fv=localFileListView.getFirstVisiblePosition();
					if (localFileListView.getChildAt(0)!=null)
						localCurrFLI.pos_top=localFileListView.getChildAt(0).getTop();
					localDir=t_dir.replace(localBase+"/", "");
					localFileListAdapter.setDataList(tfl);
					localFileListAdapter.notifyDataSetChanged();
					for (int j = 0; j < localFileListView.getChildCount(); j++)
						localFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
					setFilelistCurrDir(localFileListDirSpinner,localBase, localDir);
					setFileListPathName(localFileListPathBtn,localFileListCache,localBase,localDir);
					setEmptyFolderView();
					localFileListView.setSelection(0);
					putDirHist(localBase, localDir, localDirHist);
					
					if (dhi_c==null) {
						FileListCacheItem dhi=new FileListCacheItem();
						dhi.profile_name=localBase;
						dhi=new FileListCacheItem();
						dhi=new FileListCacheItem();
						dhi.base=localBase;
						dhi.directory=item.getPath()+"/"+item.getName();
						dhi.file_list=tfl;
						dhi.directory_history.addAll(localDirHist);
						putFileListCache(dhi,localFileListCache);
						localCurrFLI=dhi;
					} else {
						localCurrFLI=dhi_c;
					}
					localFileListTopBtn.setEnabled(true);
					localFileListUpBtn.setEnabled(true);
					setUiEnabled(true);
				} else {
					if (isFileListItemSelected(localFileListAdapter)) {
						item.setChecked(!item.isChecked());
						localFileListAdapter.notifyDataSetChanged();
						setUiEnabled(true);
					} else {
						setUiEnabled(true);
//			            view.setBackgroundColor(Color.DKGRAY);
						startLocalFileViewerIntent(item);
					}
				}
			}
		});
	};
	
	private void setFileListPathName(Button btn, ArrayList<FileListCacheItem> cfl, String base, String dir) {
		btn.setText("/"+dir);
		if (cfl!=null && cfl.size()>0) {
			btn.setEnabled(true);
		} else {
			btn.setEnabled(false);
		}
//		if (dir.equals("")) {
//			btn.setEnabled(false);
//			btn.setEnabled(false);
//		} else {
//			btn.setEnabled(true);
//			btn.setEnabled(true);
//		}
		setPasteButtonEnabled();
	};
	private void setLocalFilelistLongClickListener() {
		if (localFileListView==null) return;
		localFileListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (!isUiEnabled()) return true;
				createFilelistContextMenu(arg1, arg2,localFileListAdapter);
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
				for (int j = 0; j < parent.getChildCount(); j++)
	                parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
				if (!isUiEnabled()) return;
				setUiEnabled(false);
				final FileListItem item = remoteFileListAdapter.getItem(position);
				sendDebugLogMsg(1,"I","Remote filelist item clicked :" + item.getName());
				if (item.isDir()) {
					NotifyEvent ne=new NotifyEvent(mContext);
					ne.setListener(new NotifyEventListener() {
						@Override
						public void positiveResponse(Context c,Object[] o) {
							String t_dir=item.getPath()+"/"+item.getName();
							
							remoteCurrFLI.pos_fv=remoteFileListView.getFirstVisiblePosition();
							if (remoteFileListView.getChildAt(0)!=null)
								remoteCurrFLI.pos_top=remoteFileListView.getChildAt(0).getTop();
							remoteDir=t_dir.replace(remoteBase+"/", "");
							
							remoteFileListAdapter=(FileListAdapter)o[0];
							remoteFileListView.setAdapter(remoteFileListAdapter);
							remoteFileListAdapter.notifyDataSetChanged();
							for (int j = 0; j < remoteFileListView.getChildCount(); j++)
								remoteFileListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
							setFilelistCurrDir(remoteFileListDirSpinner,remoteBase, remoteDir);
							setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
							setEmptyFolderView();
							remoteFileListView.setSelection(0);
							putDirHist(remoteBase,remoteDir,remoteDirHist);

							FileListCacheItem dhi=new FileListCacheItem();
							dhi.profile_name=remoteFileListDirSpinner.getSelectedItem().toString();
							dhi.base=remoteBase;
							dhi.directory=t_dir;
							dhi.file_list=remoteFileListAdapter.getDataList();
							dhi.directory_history.addAll(remoteDirHist);
							putFileListCache(dhi,remoteFileListCache);
							remoteCurrFLI=dhi;
							
							setUiEnabled(true);
							
							remoteFileListTopBtn.setEnabled(true);
							remoteFileListUpBtn.setEnabled(true);

						}
						@Override
						public void negativeResponse(Context c,Object[] o) {
							setUiEnabled(true);
						}
					});
					String t_dir=item.getPath()+"/"+item.getName();
					FileListCacheItem dhi=getFileListCache(t_dir,remoteFileListCache);
					if (dhi==null) {
						createRemoteFileList(item.getPath()+"/"+item.getName(),ne);
					} else {
						remoteBase=dhi.base;
						remoteDir=dhi.directory.replace(dhi.base+"/","");
						remoteFileListAdapter.setDataList(dhi.file_list);
						remoteFileListAdapter.notifyDataSetChanged();
						setFilelistCurrDir(remoteFileListDirSpinner,remoteBase, remoteDir);
						setFileListPathName(remoteFileListPathBtn,remoteFileListCache,remoteBase,remoteDir);
						setEmptyFolderView();
						remoteFileListView.setSelection(0);
						
						putDirHist(remoteBase,remoteDir,remoteDirHist);
						setUiEnabled(true);
					}
				} else {
					setUiEnabled(true);
					if (isFileListItemSelected(remoteFileListAdapter)) {
						item.setChecked(!item.isChecked());
						remoteFileListAdapter.notifyDataSetChanged();
					} else {
//			            view.setBackgroundColor(Color.DKGRAY);
						startRemoteFileViewerIntent(remoteFileListAdapter, item);
						//commonDlg.showCommonDialog(false,false,"E","","Remote file was not viewd.",null);
					}
				}
			}
		});
	};
	
	private boolean isFileListItemSelected(FileListAdapter tfa) {
		boolean result=false;
		for (int i=0;i<tfa.getCount();i++) {
			if (tfa.getItem(i).isChecked()) {
				result=true;
				break;
			}
		}
		return result;
	}
	
	private void setRemoteFilelistLongClickListener() {
		if (remoteFileListView==null) return;
		remoteFileListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if (!isUiEnabled()) return true;
				createFilelistContextMenu(arg1, arg2, remoteFileListAdapter);
				return true;
			}
		});
	};

	private void setProfileToChecked(int pos, boolean p) {
		profileAdapter.getItem(pos).setChecked(p);
	};

	private void createProfileContextMenu(View view, int idx) {
		int prev_selected_cnt=0;
		for (int i=0;i<profileAdapter.getCount();i++) {
			if (profileAdapter.getItem(i).isChecked()) {
				prev_selected_cnt++;
			}
		}
		if (prev_selected_cnt==0) {//Not selected
			setProfileToChecked(idx, true);
			profileAdapter.notifyDataSetChanged();
			createProfileContextMenu_Single(idx);
		} else if (prev_selected_cnt==1) {//Previous selected was single
			for (int i=0;i<profileAdapter.getCount();i++) {
				if (profileAdapter.getItem(i).isChecked()) {
					if (i!=idx) {
						setProfileToChecked(i, false);
						setProfileToChecked(idx, true);
						profileAdapter.notifyDataSetChanged();
					}
				}
			}
			createProfileContextMenu_Single(idx);
		} else {
			boolean already_selected=false;
			for (int i=0;i<profileAdapter.getCount();i++) {
				if (profileAdapter.getItem(i).isChecked()) {
					if (i==idx) {
						already_selected=true;
						break;
					}
				}
			}
			if (already_selected) {
				createProfileContextMenu_Multiple(idx);
			} else {
				setAllProfileItemUnChecked();
				setProfileToChecked(idx, true);
				profileAdapter.notifyDataSetChanged();
				createProfileContextMenu_Single(idx);
			}
		}
	};
	
	private void createProfileContextMenu_Single(final int idx) {
		final ProfileListItem item = profileAdapter.getItem(idx);

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
				addRemoteProfile("", "", "username","","ip-address", "", "","");
		  	}
	  	});

		ccMenu.addMenuItem("Edit remote profile",R.drawable.menu_edit)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				editRemoteProfile(item.getActive(), item.getName(), item.getUser(),
						item.getPass(), item.getAddr(), item.getPort(), 
						item.getShare(), "",idx);
				setAllProfileItemUnChecked();
		  	}
	  	});
		ccMenu.addMenuItem("Delete remote profile",R.drawable.menu_delete)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				deleteRemoteProfile(item.getName(), idx);
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
	
	private void createProfileContextMenu_Multiple(int idx) {
		
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
					if (profileAdapter.getItem(i).isChecked()) {
						ProfileListItem item = profileAdapter.getItem(i);
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
	
	
	private void createFilelistContextMenu(View view, int idx, FileListAdapter fla) {
		fileioLinkParm.clear();

		int prev_selected_cnt=0;
		for (int i=0;i<fla.getCount();i++) {
			if (fla.getItem(i).isChecked()) {
				prev_selected_cnt++;
			}
		}
		if (prev_selected_cnt==0) {//Not selected
			setFilelistItemToChecked(fla, idx, true);
			fla.notifyDataSetChanged();
			createFilelistContextMenu_Single(fla,idx);
		} else if (prev_selected_cnt==1) {//Previous selected was single
			for (int i=0;i<fla.getCount();i++) {
				if (fla.getItem(i).isChecked()) {
					if (i!=idx) {
						setFilelistItemToChecked(fla, i, false);
						setFilelistItemToChecked(fla, idx, true);
						fla.notifyDataSetChanged();
					}
				}
			}
			createFilelistContextMenu_Single(fla,idx);
		} else {
			boolean already_selected=false;
			for (int i=0;i<fla.getCount();i++) {
				if (fla.getItem(i).isChecked()) {
					if (i==idx) {
						already_selected=true;
						break;
					}
				}
			}
			if (already_selected) {
				createFilelistContextMenu_Multiple(fla,idx);
			} else {
				setAllFilelistItemUnChecked(fla);
				setFilelistItemToChecked(fla, idx, true);
				fla.notifyDataSetChanged();
				createFilelistContextMenu_Single(fla,idx);
			}
		}

	};

	private void createFilelistContextMenu_Multiple(final FileListAdapter fla, int idx) {

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

//		String tmp;
//    	if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) tmp=localBase+"/"+localDir;
//    		else tmp=remoteBase+"/"+remoteDir;
//		if (isPasteEnabled && isValidPasteDestination(tmp+"/")) {
//			final String t_url=tmp;
//			String fl="",sep="";
//			for (int i=0;i<pasteFromList.size();i++) { 
//				fl+=sep+pasteFromList.get(i).getName();
//				sep=",";
//			}
//			ccMenu.addMenuItem("Paste to(/"+fl+")",R.drawable.blank)
//		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//			  @Override
//			  public void onClick(CharSequence menuTitle) {
//				  pasteItem(fla,t_url);
//			  }
//		  	});
//		};
		ccMenu.addMenuItem("Select all item", R.drawable.context_button_select_all)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item", R.drawable.context_button_unselect_all)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void createFilelistContextMenu_Single(final FileListAdapter fla, final int idx) {
		final FileListItem item = fla.getItem(idx);
		
		ccMenu.addMenuItem("Property",R.drawable.menu_properties)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  showProperty(fla,"C", item.getName(), item.isDir(),idx);
			  setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			ccMenu.addMenuItem("TextFileBrowser")
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
				  invokeTextFileBrowser(fla,"C", item.getName(), item.isDir(),idx);
				  setAllFilelistItemUnChecked(fla);
			  	}
		  	});
		}
//		ccMenu.addMenuItem("Create a directory",R.drawable.menu_create)
//	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//		  @Override
//		  public void onClick(CharSequence menuTitle) {
//				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
//					createItem(fla,"C", localBase+"/"+localDir);
//				else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
//					createItem(fla,"C", remoteBase+"/"+remoteDir);
//				setAllFilelistItemUnChecked(fla);
//		  	}
//	  	});

		ccMenu.addMenuItem("Rename '" + item.getName()+"'",R.drawable.menu_rename)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  renameItem(fla,"C", item.getName(), item.isDir(),idx);
			  setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		
		if (isPasteEnabled) {
			ccMenu.addMenuItem("Clear copy/cut list")
		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			  @Override
			  public void onClick(CharSequence menuTitle) {
				  clearPasteItemList();
//				  pasteFromList.clear();
			  	}
		  	});
		};
		
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
//		String pd="";
//		if (item.isDir()) pd=item.getPath()+"/"+item.getName()+"/";
//		else pd=item.getPath()+"/";
//		if (isPasteEnabled && isValidPasteDestination(pd) && item.isDir()) {
//			ccMenu.addMenuItem("Paste to /"+item.getName()+
//					" from ("+pasteItemList+")",R.drawable.blank)
//		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//			  @Override
//			  public void onClick(CharSequence menuTitle) {
//				  pasteItem(fla,item.getPath()+"/"+item.getName());
//			  	}
//		  	});
//		};
		
//		String t_url="";
//    	if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) t_url=localUrl+"/";
//    	else  t_url=remoteUrl+"/";
//    	if (isValidPasteDestination(t_url)) {
//			ccMenu.addMenuItem("Paste to /"+
//					" from ("+pasteItemList+")",R.drawable.blank)
//		  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//			  @Override
//			  public void onClick(CharSequence menuTitle) {
//		    		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) 
//		    			pasteItem(localFileListAdapter,localUrl);
//		    		else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) 
//		    			pasteItem(remoteFileListAdapter,remoteUrl);
//			  	}
//		  	});
//    	}		
//		
		ccMenu.addMenuItem("Select all item", R.drawable.context_button_select_all)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem("Unselect all item", R.drawable.context_button_unselect_all)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void setAllFilelistItemUnChecked(FileListAdapter fla) {
		FileListItem item;
		for (int i=0;i<fla.getCount();i++) {
			if (fla.getItem(i).isChecked()) { 
				item=fla.getItem(i);
				item.setChecked(false);
			}
		}
		fla.notifyDataSetChanged();
	};

	private void setAllFilelistItemChecked(FileListAdapter fla) {
		FileListItem item;
		for (int i=0;i<fla.getCount();i++) {
			item=fla.getItem(i);
			item.setChecked(true);
		}
		fla.notifyDataSetChanged();
	};

	private void setFilelistItemToChecked(FileListAdapter fla, int pos, boolean p) {
		fla.getItem(pos).setChecked(p);
	};

	private void setAllProfileItemUnChecked() {
		for (int i=0;i<profileAdapter.getCount();i++) {
			profileAdapter.getItem(i).setChecked(false);
		}
		profileAdapter.notifyDataSetChanged();
	};

	private void setAllProfileItemChecked() {
		for (int i=0;i<profileAdapter.getCount();i++) {
			profileAdapter.getItem(i).setChecked(true);
		}
		profileAdapter.notifyDataSetChanged();
	};

	private void invokeTextFileBrowser(FileListAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {
		FileListItem item=fla.getItem(item_num);
		try {
			Intent intent;
			intent = new Intent();
			intent.setClassName("com.sentaroh.android.TextFileBrowser",
					"com.sentaroh.android.TextFileBrowser.MainActivity");
			intent.setDataAndType(
					Uri.parse("file://"+item.getPath()+"/"+item.getName()), null);
				startActivity(intent);
		} catch(ActivityNotFoundException e) {
//			commonDlg.showCommonDialog(false,"E", "TextFileBrowser can not be found.",
//					"File name="+item.getName(),null);
			showDialogMsg("E", "TextFileBrowser can not be found.",
					"File name="+item.getName());
		}
	};
	
	private void startLocalFileViewerIntent(FileListItem item) {
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
			if (mt.startsWith("text")) mt="text/plain";
			try {
				Intent intent;
				intent = new Intent(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.parse("file://"+item.getPath()+"/"+item.getName()), mt);
					startActivity(intent);
			} catch(ActivityNotFoundException e) {
//				commonDlg.showCommonDialog(false,"E", "File viewer can not be found.",
//						"File name="+item.getName()+", MimeType="+mt,null);
				showDialogMsg("E", "File viewer can not be found.",
						"File name="+item.getName()+", MimeType="+mt);
			}
		} else {
//			commonDlg.showCommonDialog(false,"E", "MIME type can not be found.",
//					"File name="+item.getName(),null);
			showDialogMsg("E", "MIME type can not be found.",
					"File name="+item.getName());
		}
	};
	
	private void startRemoteFileViewerIntent(FileListAdapter fla,
			final FileListItem item) {
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
			ntfy.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					Log.v("","positive");
					try {
						Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(
							Uri.parse("file://"+
									mGp.SMBExplorerRootDir+"/SMBExplorer/download/"+
									item.getName()), mt);
						startActivity(intent);
					} catch(ActivityNotFoundException e) {
//						commonDlg.showCommonDialog(false,"E", "File viewer can not be found.",
//								"File name="+item.getName()+", MimeType="+mt,null);
						showDialogMsg("E", "File viewer can not be found.",
								"File name="+item.getName()+", MimeType="+mt);
					}
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {
					Log.v("","negative");
				}
			});
			downloadRemoteFile(fla, item, remoteBase, ntfy );
		} else {
//			commonDlg.showCommonDialog(false,"E", "MIME type can not be found.",
//					"File name="+item.getName(),null);
			showDialogMsg("E", "MIME type can not be found.",
					"File name="+item.getName());
		}

	};
	
	private void downloadRemoteFile(FileListAdapter fla, 
			FileListItem item, 
			String url, NotifyEvent p_ntfy) {
		fileioLinkParm.clear();
		buildFileioLinkParm(fileioLinkParm, item.getPath(),
				mGp.SMBExplorerRootDir+"/SMBExplorer/download/",item.getName(),"",
				smbUser,smbPass,false);
		startFileioTask(fla,FILEIO_PARM_DOWLOAD_REMOTE_FILE,fileioLinkParm,
				item.getName(),p_ntfy, mGp.SMBExplorerRootDir);
	};
	
	private ThreadCtrl mTcFileIoTask=null;
	
	private void startFileioTask(FileListAdapter fla,
			final int op_cd,final ArrayList<FileIoLinkParm> alp,String item_name,
			final NotifyEvent p_ntfy, final String lmp) {
		setAllFilelistItemUnChecked(fla);
		
		@SuppressWarnings("unused")
		String dst="";
		String dt = null;
		@SuppressWarnings("unused")
		String nitem=item_name;
		mGp.fileIoWifiLockRequired=false;
		switch (op_cd) {
			case FILEIO_PARM_REMOTE_CREATE:
				mGp.fileIoWifiLockRequired=true;
			case FILEIO_PARM_LOCAL_CREATE:
				dt="Create";
				dst=item_name+" was created.";
				nitem="";
				break;
			case FILEIO_PARM_REMOTE_RENAME:
				mGp.fileIoWifiLockRequired=true;
			case FILEIO_PARM_LOCAL_RENAME:
				dt="Rename";
				dst=item_name+" was renamed.";
				nitem="";
				break;
			case FILEIO_PARM_REMOTE_DELETE:
				mGp.fileIoWifiLockRequired=true;
			case FILEIO_PARM_LOCAL_DELETE:
				dt="Delete";
				dst="Following dirs/files were deleted.";
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
			case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
			case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
				mGp.fileIoWifiLockRequired=true;
			case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
				dt="Copy";
				dst="Following dirs/files were copied.";
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
			case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
			case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
				mGp.fileIoWifiLockRequired=true;
			case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
				dt="Move";
				dst="Following dirs/files were moved.";
				break;
			case FILEIO_PARM_DOWLOAD_REMOTE_FILE:
				mGp.fileIoWifiLockRequired=true;
				dt="Download";
				dst="";
			default:
				break;
		}
		
		mTcFileIoTask=new ThreadCtrl();
		mTcFileIoTask.setEnabled();

		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			mGp.progressMsgView=mLocalProgressMsg;
			mGp.progressCancelBtn=mLocalProgressCancel;
			showLocalProgressView();
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			mGp.progressMsgView=mRemoteProgressMsg;
			mGp.progressCancelBtn=mRemoteProgressCancel;
			showRemoteProgressView();
		}
		mGp.progressMsgView.setText(dt);
		
		mGp.progressCancelBtn.setEnabled(true);
		mGp.progressCancelBtn.setText("Cancel");
		mProgressOnClickListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				mTcFileIoTask.setDisabled();
				mGp.progressCancelBtn.setEnabled(false);
				mGp.progressCancelBtn.setText("Cancelling");
			}
		};
		mGp.progressCancelBtn.setOnClickListener(mProgressOnClickListener);
		
		NotifyEvent ne=new NotifyEvent(mContext);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				hideLocalProgressView();
				if (!mTcFileIoTask.isThreadResultSuccess()) {
					if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
					if (mTcFileIoTask.isThreadResultCancelled()) {
//						commonDlg.showCommonDialog(false,"W","File I/O task was cancelled.","",null);
						showDialogMsg("W","File I/O task was cancelled.","");
						sendLogMsg("W","File I/O task was cancelled.");
//						refreshFilelistView();
					} else {
//						commonDlg.showCommonDialog(false,"E","File I/O task was failed."+"\n"+
//										mTcFileIoTask.getThreadMessage(),"",null);
						showDialogMsg("E","File I/O task was failed."+"\n"+
								mTcFileIoTask.getThreadMessage(),"");
						sendLogMsg("E","File I/O task was failed.");
//						refreshFilelistView();
					}
				} else {
					if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
					else refreshFilelistView();
				}
				alp.clear();
				if (isTaskDataExisted()) saveTaskData();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				hideLocalProgressView();
			}
		});
		
		Thread th = new Thread(new FileIo(mGp, op_cd, alp, mTcFileIoTask, ne, this, lmp));
		mTcFileIoTask.initThreadCtrl();
		th.setPriority(Thread.MIN_PRIORITY);
		th.start(); 
	};

	private void showDialogMsg(String cat, String st, String mt) {
		setUiEnabled(false);
		createDialogCloseBtnListener();
		String msg="";
		if (mt.equals("")) msg=st;
		else msg=st+"\n"+mt;
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			showLocalDialogView();
			mGp.dialogMsgView=mLocalDialogMsg;
			mGp.dialogCloseBtn=mLocalDialogCloseBtn;
		} else if (currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
			showRemoteDialogView();
			mGp.dialogMsgView=mRemoteDialogMsg;
			mGp.dialogCloseBtn=mRemoteDialogCloseBtn;
		}
		if (cat.equals("E")) mGp.dialogMsgView.setTextColor(Color.RED);
		else if (cat.equals("W")) mGp.dialogMsgView.setTextColor(Color.YELLOW);
		else mGp.dialogMsgView.setTextColor(Color.WHITE);
		mGp.dialogMsgView.setText(msg);
		mGp.dialogCloseBtn.setOnClickListener(mDialogOnClickListener);
		mDialogMsgCat=cat;
	};
	
	private void createDialogCloseBtnListener() {
		mDialogOnClickListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				hideLocalDialogView();
				hideRemoteDialogView();
				setUiEnabled(true);
			}
			
		};
	};
	
//	private void refreshLocalFileList() {
//    	
//		final TextView tv_prog_msg=(TextView)findViewById(R.id.explorer_filelist_local_progress_msg);
//		final Button btn_prog_can=(Button)findViewById(R.id.explorer_filelist_local_progress_cancel);
//		
//		final ThreadCtrl tc = new ThreadCtrl();
//		tc.setEnabled();
//	
//		showLocalProgressView();
//		
//		tv_prog_msg.setText(R.string.msgs_progress_spin_dlg_title3);
//		
//		btn_prog_can.setEnabled(true);
//		btn_prog_can.setText("Cancel");
//		btn_prog_can.setOnClickListener(new OnClickListener(){
//			@Override
//			public void onClick(View v) {
//				sendDebugLogMsg(1,"W","Filelist is cancelled.");
//				tc.setDisabled();
//				btn_prog_can.setEnabled(false);
//				btn_prog_can.setText("Cancelling");
//			}
//		});
////		final Handler hndl=new Handler();
// 
//    	
//	};
	
	private void refreshRemoteFileList(String user, String pass) {
		final ArrayList<FileListItem> remoteFileList=new ArrayList<FileListItem>();
	
		final ThreadCtrl tc = new ThreadCtrl();
		tc.setEnabled();
	
		showRemoteProgressView();
		
		mRemoteProgressMsg.setText(R.string.msgs_progress_spin_dlg_title3);
		
		mRemoteProgressCancel.setEnabled(true);
		mRemoteProgressCancel.setText("Cancel");
		mRemoteProgressCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sendDebugLogMsg(1,"W","Filelist is cancelled.");
				tc.setDisabled();
				mRemoteProgressCancel.setEnabled(false);
				mRemoteProgressCancel.setText("Cancelling");
			}
		});
		
		NotifyEvent ne=new NotifyEvent(this);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				if (tc.isThreadResultSuccess()) {
					int fv=remoteFileListView.getFirstVisiblePosition();
					int top=0;
					if (remoteFileListView.getChildAt(0)!=null) top=remoteFileListView.getChildAt(0).getTop();
					remoteFileListAdapter.setDataList(remoteFileList);
					remoteFileListAdapter.sort();
					remoteFileListAdapter.notifyDataSetChanged();
					remoteFileListView.setSelectionFromTop(fv, top);
					removeFileListCache(buildFullPath(remoteBase,remoteDir), remoteFileListCache);
					
					FileListCacheItem dhi=new FileListCacheItem();
					dhi.profile_name=remoteFileListDirSpinner.getSelectedItem().toString();
					dhi.base=remoteBase;
					dhi.directory=buildFullPath(remoteBase,remoteDir);
					dhi.file_list=remoteFileList;
					dhi.directory_history.addAll(remoteDirHist);
					putFileListCache(dhi,remoteFileListCache);
					remoteCurrFLI=dhi;

					setEmptyFolderView();
					updateFileListCacheByMove();
					removeDeletedFileListCache(remoteCurrFLI, remoteFileListCache);
				} else {
					String err="";
					if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
					else err=tc.getThreadMessage();
//					commonDlg.showCommonDialog(false,"E",
//							getString(R.string.msgs_remote_file_list_create_error),err,null);
					showDialogMsg("E",
							getString(R.string.msgs_remote_file_list_create_error),err);
					Spinner spinner = (Spinner) findViewById(R.id.explorer_filelist_remote_tab_dir);
					spinner.setSelection(0);
					remoteBase="";
					if (remoteFileListAdapter!=null) {
						remoteFileList.clear();
					}
					setEmptyFolderView();
					mIsMovedListAvailable=false;
				}
			}
	
			@Override
			public void negativeResponse(Context c,Object[] o) {
				hideRemoteProgressView();
			}
		});
		Thread th = new Thread(new RetrieveFileList(mGp, tc,  
				RetrieveFileList.OPCD_FILE_LIST, formatRemoteSmbUrl(remoteBase+"/"+remoteDir+"/"), 
				remoteFileList,user,pass,ne));
		th.start();
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

	private void createItem(final FileListAdapter fla,
			final String item_optyp, final String base_dir) {
		sendDebugLogMsg(1,"I","createItem entered.");
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
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
				if (!checkDuplicateDir(fla,newName.getText().toString())) {
					commonDlg.showCommonDialog(false,"E","Create","Duplicate directory name specified",null);
				} else {
					int cmd=0;
					if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
						fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								base_dir,"",newName.getText().toString(),"",
								smbUser,smbPass,true); 
						cmd=FILEIO_PARM_LOCAL_CREATE;
					} else {
						cmd=FILEIO_PARM_REMOTE_CREATE;
						fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								base_dir,"",newName.getText().toString(),"",
								smbUser,smbPass,true);
					}
					sendDebugLogMsg(1,"I","createItem FILEIO task invoked.");
					startFileioTask(fla,cmd,fileioLinkParm,
							newName.getText().toString(),null,null);
				}
			}
		});
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
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

	private boolean checkDuplicateDir(FileListAdapter fla,String ndir) {
		for (int i = 0; i < fla.getCount(); i++) {
			if (ndir.equals(fla.getItem(i).getName()))
				return false; // duplicate dir
		}
		return true;
	};

	private void showProperty(FileListAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		FileListItem item = fla.getItem(item_num);
		
		String info;
		
		info = "Path="+item.getPath()+"\n";
		info = info+
				"Name="+item.getName()+"\n"+
				"Directory : "+item.isDir()+"\n"+
				"Hidden : "+item.isHidden()+"\n"+
				"canRead :"+item.canRead()+"\n"+
				"canWrite :"+item.canWrite()+"\n"+
				"Length : "+item.getLength()+"\n"+
				"Last modified : "+df.format(item.getLastModified())+"\n"+
				"Last modified(ms):"+item.getLastModified();
		commonDlg.showCommonDialog(false,"I","Property",info,null);

	}
	
	private void renameItem(final FileListAdapter fla,
			final String item_optyp, final String item_name,
			final boolean item_isdir, final int item_num) {

		sendDebugLogMsg(1,"I","renameItem entered.");
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
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
								fla.getItem(item_num).getPath() , 
								fla.getItem(item_num).getPath(),
								item_name,newName.getText().toString(),"","",true);
						cmd=FILEIO_PARM_LOCAL_RENAME;
					} else {
						cmd=FILEIO_PARM_REMOTE_RENAME;
						if (item_isdir)
							fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								fla.getItem(item_num).getPath(), 
								fla.getItem(item_num).getPath(),
								item_name,newName.getText().toString(),
								smbUser,smbPass,true);
						else
							fileioLinkParm=buildFileioLinkParm(fileioLinkParm,
								fla.getItem(item_num).getPath(), 
								fla.getItem(item_num).getPath(),
								item_name,newName.getText().toString(),smbUser,smbPass,true);
					}
					sendDebugLogMsg(1,"I","renameItem FILEIO task invoked.");
					startFileioTask(fla,cmd,fileioLinkParm,item_name,null, null);
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

	private void deleteItem(final FileListAdapter fla) {
		sendDebugLogMsg(1,"I","deleteItem entered.");
		String di ="";
		for (int i=0;i<fla.getCount();i++) {
			FileListItem item = fla.getItem(i);
			if (item.isChecked()) di=di+item.getName()+"\n";
		}
		
		final String item_name=di;
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] t) {
				for (int i=fla.getCount()-1;i>=0;i--) {
					FileListItem item = fla.getItem(i);
					if (item.isChecked()) {
						if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
							buildFileioLinkParm(fileioLinkParm,item.getPath(),
									"",item.getName(),"","","",true);
						} else {
							buildFileioLinkParm(fileioLinkParm,item.getPath(),
										"",item.getName(),"",smbUser,smbPass,true);
						}
					}
				}
				setAllFilelistItemUnChecked(fla);
				sendDebugLogMsg(1,"I","deleteItem invokw FILEIO task.");
				if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL))
					startFileioTask(fla,FILEIO_PARM_LOCAL_DELETE,fileioLinkParm,item_name,null,null);
				else startFileioTask(fla,FILEIO_PARM_REMOTE_DELETE,fileioLinkParm,item_name,null,null);
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				setAllFilelistItemUnChecked(fla);
				sendDebugLogMsg(1,"W","deleteItem canceled");
			}
		});
		commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_delete_file_dirs_confirm),item_name,ne);
	};
	
	private ArrayList<FileListItem> pasteFromList=new ArrayList<FileListItem>();
	private String pasteFromUrl, pasteItemList="";
	private boolean isPasteCopy=false,isPasteEnabled=false, isPasteFromLocal=false;
	private void setCopyFrom(FileListAdapter fla) {
		pasteItemList="";
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			pasteFromUrl=localBase+"/"+localDir;
			isPasteFromLocal=true;
		} else {
			pasteFromUrl=remoteBase+"/"+remoteDir;;
			isPasteFromLocal=false;
		}
		//Get selected item names
		isPasteCopy=true;
		isPasteEnabled=true;
		FileListItem fl_item;
		pasteFromList.clear();
		String sep="";
		for (int i = 0; i < fla.getCount(); i++) {
			fl_item = fla.getItem(i);
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
	
	private void setCutFrom(FileListAdapter fla) {
		pasteItemList="";
		if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
			pasteFromUrl=localBase+"/"+localDir;
//			pasteFromDir=localCurrDir;
			isPasteFromLocal=true;
		} else {
			pasteFromUrl=remoteBase+"/"+remoteDir;
//			pasteFromDir=remoteCurrDir;
			isPasteFromLocal=false;
		}
		//Get selected item names
		isPasteCopy=false;
		isPasteEnabled=true;
		FileListItem fl_item;
		pasteFromList.clear();
		String sep="";
		for (int i = 0; i < fla.getCount(); i++) {
			fl_item = fla.getItem(i);
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
		LinearLayout lc=(LinearLayout)findViewById(R.id.explorer_filelist_copy_paste);
		TextView pp=(TextView)findViewById(R.id.explorer_filelist_paste_list);
		TextView cc=(TextView)findViewById(R.id.explorer_filelist_paste_copycut);
		if (isPasteEnabled) {
			String op="Copy : ";
			if (!isPasteCopy) op="Cut : ";
			cc.setText(op);
			pp.setText(pasteItemList);
			lc.setVisibility(LinearLayout.VISIBLE);
		}
		setPasteButtonEnabled();
	};

	private void setPasteButtonEnabled() {
		boolean vp=false;
		if (isPasteEnabled) {
			if (currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				vp=isValidPasteDestination(localBase,localDir);
			} else {
				vp=isValidPasteDestination(remoteBase,remoteDir);
			}
		}
		localFileListPasteBtn.setEnabled(vp);
		remoteFileListPasteBtn.setEnabled(vp);
	};
	
	private void clearPasteItemList() {
		LinearLayout lc=(LinearLayout)findViewById(R.id.explorer_filelist_copy_paste);
		isPasteEnabled=false;
		pasteItemList="";
		TextView pp=(TextView)findViewById(R.id.explorer_filelist_paste_list);
//		TextView cc=(TextView)findViewById(R.id.explorer_filelist_paste_copycut);
		pp.setText(pasteItemList);
		lc.setVisibility(LinearLayout.GONE);
		setPasteButtonEnabled();
	};
	
	private boolean isValidPasteDestination(String base, String dir) {
		boolean result=false;
//		Log.v("","base="+base+", dir="+dir);
//		Thread.currentThread().dumpStack();
		if (isPasteEnabled) {
			String to_dir="";
			if (dir.equals("")) to_dir=base;
			else to_dir=base+"/"+dir;
			String from_dir=pasteFromList.get(0).getPath();
			String from_path=pasteFromList.get(0).getPath()+"/"+pasteFromList.get(0).getName();
			if (!to_dir.equals(from_dir)) {
				if (!from_path.equals(to_dir)) {
					if (!to_dir.startsWith(from_path)) {
						result=true;
					}
				}
			}
		}
		return result;
	};

	private void pasteItem(final FileListAdapter fla, final String to_dir, final String lmp) {
		//Get selected item names
		FileListItem fl_item;
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
			pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req, lmp);
		} else {
			final ArrayList<String> d_list=new ArrayList<String>();
			for (int i = 0; i < pasteFromList.size(); i++)
				d_list.add(to_dir+pasteFromList.get(i).getName());
			NotifyEvent ntfy=new NotifyEvent(this);
			// set commonDialog response 
			ntfy.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					String fl_name="",fl_exists="";
					boolean fl_conf_req=false;
					for (int i=0;i<d_list.size();i++) 
						if (!d_list.get(i).equals("")) 
							fl_exists=fl_exists+d_list.get(i)+"\n";
					if (!fl_exists.equals("")) fl_conf_req=true;
					for (int i = 0; i < pasteFromList.size(); i++) 
						fl_name=fl_name+pasteFromList.get(i).getName()+"\n";
					pasteCreateIoParm(fla,to_dir,fl_name,fl_exists,fl_conf_req, lmp);
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {	}
			});
			checkRemoteFileExists(remoteBase, smbUser,smbPass, d_list, ntfy);
		}
	};

	private void pasteCreateIoParm(FileListAdapter fla, String to_dir, 
			String fl_name, String fl_exists,boolean fl_conf_req, final String lmp) {
		
		FileListItem fi ;
		for (int i=0;i<pasteFromList.size();i++) {
			fi=pasteFromList.get(i);
			buildFileioLinkParm(fileioLinkParm, fi.getPath(),
					to_dir,fi.getName(),fi.getName(),smbUser,smbPass,true);
		}
		// copy process
		if (isPasteCopy) {
			if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				// Local to Local copy localCurrDir->curr_dir
				copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);
			} else if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Local to Remote copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_LOCAL_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);

			} else if (!isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Remote to Remote copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);

			} else {
				// Remote to Local copy localCurrDir->remoteUrl
				copyConfirm(fla,FILEIO_PARM_COPY_REMOTE_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);
			}
		} else {
			// process move
			clearPasteItemList();
			if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_LOCAL)) {
				// Local to Local 
				moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);
			} else if (isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Local to Remote 
				moveConfirm(fla,FILEIO_PARM_MOVE_LOCAL_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);

			} else if (!isPasteFromLocal && currentTabName.equals(SMBEXPLORER_TAB_REMOTE)) {
				// Remote to Remote 
				moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_REMOTE,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);

			} else {
				// Remote to Local 
				moveConfirm(fla,FILEIO_PARM_MOVE_REMOTE_TO_LOCAL,fileioLinkParm,
						fl_name,fl_conf_req, fl_exists, lmp);
			}
		}

	};
	
	private void copyConfirm(final FileListAdapter fla,
			final int cmd_cd, ArrayList<FileIoLinkParm> alp, 
			final String selected_name, boolean conf_req, String conf_msg, final String lmp) {
			
		if (conf_req) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","copyConfirm File I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null, lmp);
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Ccopy override confirmation cancelled.");
				}
			});
			commonDlg.showCommonDialog(true,"W","Copy following dirs/files are overrides?",
					conf_msg,ne);

		} else {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","copyConfirm FILE I/O task invoked.");
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null, lmp);
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Copy cancelled."+"\n"+selected_name);
				}
			});
			commonDlg.showCommonDialog(true,"I","Following dirs/files are copy?",selected_name,ne);
		}
		return;
	};
	
	private void moveConfirm(final FileListAdapter fla,
			final int cmd_cd, ArrayList<FileIoLinkParm> alp, 
			final String selected_name, boolean conf_req, String conf_msg, final String lmp) {
			
		if (conf_req) {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","moveConfirm File I/O task invoked.");
					movedFileListForRefresh=pasteFromList;
					mIsMovedListAvailable=true;
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null, lmp);
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Move override confirmation cancelled.");
					mIsMovedListAvailable=false;
				}
			});
			commonDlg.showCommonDialog(true,"W","Move following dirs/files are overrides?",
					conf_msg,ne);

		} else {
			NotifyEvent ne=new NotifyEvent(this);
			// set commonDialog response 
			ne.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					sendDebugLogMsg(1,"I","moveConfirm FILE I/O task invoked.");
					movedFileListForRefresh=pasteFromList;
					mIsMovedListAvailable=true;
					startFileioTask(fla,cmd_cd,fileioLinkParm,selected_name,null, lmp);
				}
				@Override
				public void negativeResponse(Context c,Object[] o) {
					sendLogMsg("W","Move cancelled."+"\n"+selected_name);
					mIsMovedListAvailable=false;
				}
			});
			commonDlg.showCommonDialog(true,"I","Following dirs/files are move?",selected_name,ne);
		}
		return;
	};
	
	private void setFilelistCurrDir(Spinner tv,String base, String dir) {
//		tv.setText(dir_text);
		for (int i=0;i<tv.getCount();i++) {
			String list=(String)tv.getItemAtPosition(i);
			if (list.equals(base)) {
				tv.setSelection(i);
				break;
			}
		};
	};

	private ArrayList<FileListItem> createLocalFileList(boolean dir_only, String url) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		FilelistAdapter2 filelist;
		
		ArrayList<FileListItem> dir = new ArrayList<FileListItem>();
		ArrayList<FileListItem> fls = new ArrayList<FileListItem>();

		sendDebugLogMsg(1,"I","create local file list local url=" + url);

		File sf = new File(url);

		// File List

		File[] file_list = sf.listFiles();
		if (file_list!=null) {
			try {
				for (File ff : file_list) {
					FileListItem tfi=null;
					if (ff.canRead()) {
						if (ff.isDirectory()) {
							File tlf=new File(url+"/"+ff.getName());
							String[] tfl=tlf.list();
							int sdc=0;
							if (tfl!=null) sdc=tfl.length;
							int ll=0;
							tfi=createNewFilelistItem(ff, sdc, ll);
							dir.add(tfi);
						} else {
							tfi=createNewFilelistItem(ff, 0, 0);
							fls.add(tfi);
						}
						sendDebugLogMsg(2,"I","File :" + tfi.getName()+", "+
							"length: " + tfi.getLength()+", "+
							"Lastmod: " + sdf.format(tfi.getLastModified())+", "+
							"Lastmod: " + tfi.getLastModified()+", "+
							"isdir: " + tfi.isDir()+", "+
							"parent: " + ff.getParent()+", "+
							"path: " + tfi.getPath()+", "+
							"canonicalPath: " + ff.getCanonicalPath());
					} else {
						tfi=createNewFilelistItem(ff, 0, 0);
						if (tfi.isDir()) dir.add(tfi);
						else fls.add(tfi);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				sendDebugLogMsg(0,"E",e.toString());
//				commonDlg.showCommonDialog(false,"E",getString(R.string.msgs_local_file_list_create_error),
//							e.getMessage(),null);
				showDialogMsg("E",getString(R.string.msgs_local_file_list_create_error),
						e.getMessage());
				return null;
			}
		}

		Collections.sort(dir);
		if (!dir_only) {
			Collections.sort(fls);
			dir.addAll(fls);
		}
		
//		filelist = new FilelistAdapter2(this,this,treeListImage);
//		filelist.setDataList(dir);
		return dir;
	};

	private void createRemoteFileList(final String url, final NotifyEvent parent_event) {
		sendDebugLogMsg(1,"I","Create remote filelist remote url:"+url);
		final NotifyEvent n_event=new NotifyEvent(this);
		n_event.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				String itemname = "";
				FileListAdapter filelist;
				@SuppressWarnings("unchecked")
				ArrayList<FileListItem> sf_item=(ArrayList<FileListItem>)o[0];
				
				ArrayList<FileListItem> dir = new ArrayList<FileListItem>();
				List<FileListItem> fls = new ArrayList<FileListItem>();
				
				for (int i = 0; i < sf_item.size(); i++) {
					itemname = sf_item.get(i).getName();
					if (itemname.equals("IPC$")) {
						// ignore IPC$
					} else {
						if (sf_item.get(i).canRead()) {
							if (sf_item.get(i).isDir()) {
								dir.add(createNewFilelistItem(sf_item.get(i)));
							} else {
								fls.add(createNewFilelistItem(sf_item.get(i)));
							}
						} else {
							fls.add(createNewFilelistItem(sf_item.get(i)));
						}
					}
				}
				// addFileListItem("---- End of file ----", false, 0, 0);
	
				dir.addAll(fls);
				filelist = new FileListAdapter(c);
				filelist.setShowLastModified(true);
				filelist.setDataList(dir);
				filelist.sort();
				parent_event.notifyToListener(true, new Object[]{filelist});
			}
	
			@Override
			public void negativeResponse(Context c,Object[] o) {
				parent_event.notifyToListener(false, o);
//				commonDlg.showCommonDialog(false,"E",
//						getString(R.string.msgs_remote_file_list_create_error),(String)o[0],null);
				showDialogMsg("E",
						getString(R.string.msgs_remote_file_list_create_error),(String)o[0]);
			}
		});
		FileListCacheItem dhi=getFileListCache(formatRemoteSmbUrl(url+"/"),remoteFileListCache);
		if (dhi!=null) {
			FileListAdapter fla = new FileListAdapter(mContext);
			fla.setShowLastModified(true);
			fla.setDataList(dhi.file_list);
			parent_event.notifyToListener(true, new Object[]{fla});
		} else {
			readRemoteFileList(url+"/",smbUser,smbPass,n_event);
		}
			
	};
	
	private static String buildFullPath(String base, String dir) {
		String t_dir="";
		if (dir.equals("")) t_dir=base;
		else t_dir=base+"/"+dir;
		return t_dir;
	}
	
	private void putDirHist(String base, String dir, ArrayList<String> hl) {
		String t_dir=buildFullPath(base,dir);
//		Log.v("","history added "+t_dir);
		if (getLastDirHist(hl).equals(t_dir)) sendDebugLogMsg(1,"I","putDirHist history ignored "+t_dir);
		else {
			hl.add(t_dir);
			sendDebugLogMsg(1,"I","putDirHist history added "+t_dir);
		}
//		Thread.dumpStack();
	};

	private void replaceDirHist(ArrayList<String> to, ArrayList<String> from) {
//		Log.v("","history replaced ");
		sendDebugLogMsg(1,"I","replaceDirHist history replaced");
		to.clear();
		to.addAll(from);
	};

	@SuppressWarnings("unused")
	private void removeDirHist(String base, String dir, ArrayList<String> hl) {
		String t_dir=buildFullPath(base,dir);
		boolean result=hl.remove(t_dir);
		sendDebugLogMsg(1,"I","removeDirHist history removed "+t_dir);
//		Log.v("","history removed "+t_dir+", result="+result);
	};

	private String getLastDirHist(ArrayList<String> hl) {
		String result="";
		if (hl.size()>0) result=hl.get(hl.size()-1);
		sendDebugLogMsg(1,"I","getLastDirHist get last "+result);
//		Log.v("","history get last "+result);
		return result;
	};

	private String getTopDirHist(ArrayList<String> hl) {
		String result=hl.get(0);
		sendDebugLogMsg(1,"I","getTopDirHist get top "+result);
//		Log.v("","history get top "+result);
		return result;
	};

	private void clearDirHist(ArrayList<String> hl) {
		sendDebugLogMsg(1,"I","clearDirHist history cleared");
//		Log.v("","history cleared");
		hl.clear();
	};

	private void putFileListCache(FileListCacheItem dhi, ArrayList<FileListCacheItem> fl) {
		sendDebugLogMsg(1,"I","putFileListCache put file cache base="+dhi.base+", dir="+dhi.directory);
//		Log.v("","added "+dhi.directory);
		fl.add(dhi);
	};
	
	private void removeFileListCache(String url, ArrayList<FileListCacheItem> fl) {
		FileListCacheItem dhi=getFileListCache(url, fl);
		if (dhi!=null) fl.remove(dhi);
		sendDebugLogMsg(1,"I","removeFileListCache file cache removed, key="+url);
//		if (dhi!=null) {
//			Log.v("","removed "+dhi.directory);
//		}
//		else {
//			Log.v("","not found "+url);
//			Thread.dumpStack();
//		}
	}
	
	private FileListCacheItem getFileListCache(String url, ArrayList<FileListCacheItem> fl) {
		FileListCacheItem result=null, dhi=null;
		for (int i=0;i<fl.size();i++) {
			dhi=fl.get(i);
			if (dhi.directory.equals(url)) {
				result=dhi;
				break;
			}
		}
		sendDebugLogMsg(1,"I","getFileListCache get file cache,  key="+url+", result="+result);
//		Log.v("","find url="+url+", result="+result);
//		Thread.dumpStack();
		return result;
	};
	
	private static String formatRemoteSmbUrl(String url) {
		String result="";
		String smb_url=url.replace("smb://", "");
		result="smb://"+smb_url.replaceAll("///", "/").replaceAll("//", "/");
//		Log.v("","result="+result+", t="+smb_url);
		return result;
	};

	private void checkRemoteFileExists(String url, String user, String pass,
			ArrayList<String> d_list, final NotifyEvent n_event) {
		final ArrayList<FileListItem> remoteFileList=new ArrayList<FileListItem>();
		
		final ThreadCtrl tc = new ThreadCtrl();
		remoteFileList.clear();
		tc.setEnabled();

		showRemoteProgressView();
		
		mRemoteProgressMsg.setText(R.string.msgs_progress_spin_dlg_title3);
		
		mRemoteProgressMsg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisabled();//disableAsyncTask();
				sendDebugLogMsg(1,"W","Filelist is cancelled.");
			}
		});

		NotifyEvent ne=new NotifyEvent(this);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				if (tc.isThreadResultSuccess()) {
					n_event.notifyToListener(true, o);
				} else {
					String err="";
					if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
					else err=tc.getThreadMessage();
					n_event.notifyToListener(false, new Object[]{err});
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				hideRemoteProgressView();
			}
		});
		
		Thread th = new Thread(new RetrieveFileList(mGp, tc, url, d_list,user,pass,ne));
		th.start();
	};
	
	private void showRemoteProgressView() {
		setUiEnabled(false);
		mRemoteProgressView.setVisibility(LinearLayout.VISIBLE);
		mRemoteProgressView.setBackgroundColor(dialogBackgroundColor);
		mRemoteProgressView.bringToFront();
	};

	private void hideRemoteProgressView() {
		setUiEnabled(true);
		mRemoteProgressView.setVisibility(LinearLayout.GONE);
	};

	private void showRemoteDialogView() {
		setUiEnabled(false);
		mRemoteDialogView.setVisibility(LinearLayout.VISIBLE);
		mRemoteDialogView.setBackgroundColor(dialogBackgroundColor);
		mRemoteDialogView.bringToFront();
	};

	private void hideRemoteDialogView() {
		setUiEnabled(true);
		mRemoteDialogView.setVisibility(LinearLayout.GONE);
	};

	private void showLocalDialogView() {
		setUiEnabled(false);
		mLocalDialogView.setVisibility(LinearLayout.VISIBLE);
		mLocalDialogView.setBackgroundColor(dialogBackgroundColor);
		mLocalDialogView.bringToFront();
	};

	private void hideLocalDialogView() {
		setUiEnabled(true);
		mLocalDialogView.setVisibility(LinearLayout.GONE);
	};

	private void showLocalProgressView() {
		setUiEnabled(false);
		mLocalProgressView.setVisibility(LinearLayout.VISIBLE);
		mLocalProgressView.setBackgroundColor(dialogBackgroundColor);
		mLocalProgressView.bringToFront();
	};

	private void hideLocalProgressView() {
		setUiEnabled(true);
		mLocalProgressView.setVisibility(LinearLayout.GONE);
	};

	private void readRemoteFileList(String url,String user, String pass,
				final NotifyEvent n_event) {
		final ArrayList<FileListItem> remoteFileList=new ArrayList<FileListItem>();

		final ThreadCtrl tc = new ThreadCtrl();
		remoteFileList.clear();
		tc.setEnabled();

		showRemoteProgressView();
		
		mRemoteProgressMsg.setText(R.string.msgs_progress_spin_dlg_title3);
		
		mRemoteProgressMsg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisabled();//disableAsyncTask();
				sendDebugLogMsg(1,"W","Filelist is cancelled.");
			}
		});
		
		NotifyEvent ne=new NotifyEvent(this);
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				if (tc.isThreadResultSuccess()) {
					n_event.notifyToListener(true, new Object[]{remoteFileList});
				} else {
					String err="";
					if (tc.isThreadResultCancelled()) err="Filelist was cancelled";
					else err=tc.getThreadMessage();
					n_event.notifyToListener(false, new Object[]{err});
				}
				saveTaskData();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				hideRemoteProgressView();
				saveTaskData();
			}
		});
		
		Thread th = new Thread(new RetrieveFileList(mGp, tc,  
				RetrieveFileList.OPCD_FILE_LIST, url, 
				remoteFileList,user,pass,ne));
		th.start();
	};

	private void setRemoteShare(final String prof_user, final String prof_pass,
			final String prof_addr, final NotifyEvent p_ntfy) {
		final ArrayList<String> rows = new ArrayList<String>();

		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				FileListAdapter tfl = (FileListAdapter)o[0]; 
				
				for (int i=0;i<tfl.getCount();i++){
					FileListItem item=tfl.getItem(i);
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
		        dialog.setCanceledOnTouchOutside(false);
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
						// リストアイテムを選択したときの処理
			        	p_ntfy.notifyToListener(true,  
			        			new Object[]{rows.get(idx).toString()});
		        	}
		        });	 
		        //CANCELボタンの指定
		        final Button btnCancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
		        btnCancel.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		                dialog.dismiss();
		                p_ntfy.notifyToListener(true, new Object[]{""});
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
//		        dialog.setCancelable(false);
		        dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, 
						new Object[]{"Remote file list creation error"});
			}
		});
		createRemoteFileList("smb://" + prof_addr + "/",ntfy);
		return;
	};

	private void addRemoteProfile(final String prof_act,
			final String prof_name, final String prof_user,
			final String prof_pass, final String prof_addr, final String prof_port,
			final String prof_share, final String msg_text) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.setCanceledOnTouchOutside(false);
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
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port_number);
		editport.setText(prof_port);
		
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
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editaddr.setText((String)arg1[1]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					
				});
				scanRemoteNetworkDlg(ntfy, editport.getText().toString());
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

				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						if (!((String)arg1[0]).equals("")) editshare.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
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
					int topPos =0;
					if (profileListView.getChildAt(0)!=null) profileListView.getChildAt(0).getTop();
					String prof_user=edituser.getText().toString();
					String prof_pass=editpass.getText().toString();
					String prof_addr=editaddr.getText().toString();
					String prof_share=editshare.getText().toString();
					String prof_port=editport.getText().toString();
					profileAdapter.add(new ProfileListItem(
							"R",new_name, new_act,prof_user , prof_pass,prof_addr,
									prof_port, prof_share,false));
					saveProfile(false,"","");
					profileAdapter = 
							createProfileList(false,""); // create profile list

					profileListView.setSelectionFromTop(pos,topPos);
					profileAdapter.setNotifyOnChange(true);
					
					setRemoteDirBtnListener();
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
			String prof_user, String prof_pass, String prof_addr, final String prof_port,
			String prof_share, String msg_text, final int item_num) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.setCanceledOnTouchOutside(false);
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
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port_number);
		editport.setText(prof_port);

		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.remote_profile_active);
		if (prof_act.equals("A"))
			tg.setChecked(true);
		else
			tg.setChecked(false);
		
		// addressボタンの指定
		Button btnAddr = (Button) dialog.findViewById(R.id.remote_profile_addrbtn);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editaddr.setText((String)arg1[1]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (arg1!=null) dlg_msg.setText((String)arg1[0]);
						else dlg_msg.setText("");
					}
					
				});
				scanRemoteNetworkDlg(ntfy, editport.getText().toString());
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

				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						if (!((String)arg1[0]).equals("")) editshare.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
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

				new_addr = editaddr.getText().toString();
				new_user = edituser.getText().toString();
				new_pass = editpass.getText().toString();
				new_share = editshare.getText().toString();
				new_name = editname.getText().toString();
				String new_port=editport.getText().toString();

				if (tg.isChecked()) new_act = "A";
				else new_act = "I";

				int pos = profileListView.getFirstVisiblePosition();
				int topPos =0;
				if (profileListView.getChildAt(0)!=null) profileListView.getChildAt(0).getTop();
				ProfileListItem item=profileAdapter.getItem(item_num);

				profileAdapter.remove(item);
				profileAdapter.insert(new ProfileListItem("R",
						new_name, new_act, new_user, new_pass, new_addr,new_port,
						new_share,false),
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
		ProfileListItem item ;

		for (int i=0;i<profileAdapter.getCount();i++) {
			item = profileAdapter.getItem(i);

			if (item.isChecked()) {
				item.setActive("A");
			} 
		}
		
		saveProfile(false,"","");
		//createProfileList(false);
		profileAdapter.setNotifyOnChange(true);
		
	}
	
	private void setProfileToInactive() {
		ProfileListItem item ;

		for (int i=0;i<profileAdapter.getCount();i++) {
			item = profileAdapter.getItem(i);

			if (item.isChecked()) {
				item.setActive("I");
			} 
		}
		
		saveProfile(false,"","");
		//createProfileList(false);
		profileAdapter.setNotifyOnChange(true);
	}
	
	private void deleteRemoteProfile(String item_name, final int item_num) {
		
		NotifyEvent ne=new NotifyEvent(this);
		// set commonDialog response 
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				ProfileListItem item = profileAdapter
						.getItem(item_num);

				int pos = profileListView.getFirstVisiblePosition();
				int topPos = profileListView.getChildAt(0).getTop();
				profileAdapter.remove(item);
				profileAdapter.setNotifyOnChange(true);

				saveProfile(false,"","");

				profileListView.setSelectionFromTop(pos,topPos);
				
				setRemoteDirBtnListener();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				
			}
		});
		commonDlg.showCommonDialog(true,"W",
				String.format(getString(R.string.msgs_delete_confirm),item_name),"",ne);


	};
	
	private List<ProfileListItem> createLocalProfileEntry() {
		List<ProfileListItem> lcl = new ArrayList<ProfileListItem>();
		ArrayList<String> ml=LocalMountPoint.getLocalMountPointList(mContext);
//		ArrayList<String> cpath=new ArrayList<String>();
//		try {
//			for (String ffn : ml) {
//				File lf=new File(ffn);
//				String fcp=lf.getCanonicalPath();
//				cpath.add(fcp+"\t"+ffn);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		Collections.sort(cpath);
//		
//		String c_cp="";
//		for (int i=0;i<cpath.size();i++) {
//			String[] pn=cpath.get(i).split("\t");
//			if (!c_cp.equals(pn[0])) {
//				c_cp=pn[0];
//				ProfileListItem pli=new ProfileListItem("L",pn[1], "A","", "", "", "","", false);
//				lcl.add(pli);
//			}
//		}
//		
//		Collections.sort(lcl,new Comparator<ProfileListItem>(){
//			@Override
//			public int compare(ProfileListItem arg0, ProfileListItem arg1) {
//				return arg0.getName().compareToIgnoreCase(arg1.getName());
//			}
//		});


		if (ml.size()>0) {
			try {
				String pml=LocalMountPoint.getExternalStorageDir();
				ProfileListItem pli=new ProfileListItem("L",pml, "A","", "", "", "","", false);
				File lf=new File(pml+"/"+"SMBExplorer.work.tmp");
				lf.createNewFile();
				lcl.add(pli);
				for (int i=0;i<ml.size();i++) {
//					File tlf=new File(ml.get(i));
//					Log.v("","name="+ml.get(i)+", cw="+tlf.canWrite());
					pli=new ProfileListItem("L",ml.get(i), "A","", "", "", "","", false);
					lf=new File(ml.get(i)+"/"+"SMBExplorer.work.tmp");
					if (!lf.exists()) {
						try {
							lf.createNewFile();
							lcl.add(pli);
						} catch(IOException e) {
//							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (lcl.size()>0) {
			for (int i=0;i<lcl.size();i++) {
				File lf=new File(lcl.get(i).getName()+"/"+"SMBExplorer.work.tmp");
				if (lf.exists()) lf.delete();
			}
		}
		return lcl;
	}

	private ProfileListAdapter createProfileList(boolean sdcard, String fp) {

		ProfileListAdapter pfl = null;
		BufferedReader br = null;

		sendDebugLogMsg(1,"I","Create profilelist");
		
		List<ProfileListItem> lcl=createLocalProfileEntry();
		
		List<ProfileListItem> rem = new ArrayList<ProfileListItem>();
		
		boolean error=false;

		try {
			if (sdcard) {
				File sf = new File(fp);

				if (sf.exists()) {
					br = new BufferedReader(new FileReader(fp));
				} else {
					commonDlg.showCommonDialog(false,"E",
							String.format(getString(R.string.msgs_local_file_list_create_nfound),
									fp),"",null);
					error=true;
				}
				
			} else {
				InputStream in = openFileInput(SMBEXPLORER_PROFILE_NAME);
				br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			}
			if (!error) {
				String pl;
				String[] alp;
				while ((pl = br.readLine()) != null) {
					alp = parseProfileString(pl);
					rem.add(new ProfileListItem(alp[0], alp[1], alp[2],
							alp[3], alp[4], alp[5], alp[6], alp[7],false));
				}
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			commonDlg.showCommonDialog(false,"E",
				getString(R.string.msgs_exception),e.toString(),null);
			error=true;
		} catch (IOException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			commonDlg.showCommonDialog(false,"E",
					getString(R.string.msgs_exception),e.toString(),null);
			error=true;
		}
		Collections.sort(rem);
		lcl.addAll(rem);
		if (lcl.size()==0) lcl.add(new ProfileListItem("", "No profiles", "I", "", "", "", "","",false));
		// profileListView = (ListView)findViewById(android.R.id.list);
		pfl = new ProfileListAdapter(this, R.layout.profile_list_view_item, lcl);
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
					ProfileListItem item = profileAdapter.getItem(i);
					if (item.getType().equals("R")) {
						String pl = item.getType() + "\t" + item.getName() + "\t"
								+ item.getActive() + "\t" + item.getUser() + "\t"
								+ item.getPass() + "\t" + item.getAddr() + "\t"+ item.getPort() + "\t"
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
			ProfileListItem item = profileAdapter.getItem(i);
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
	
	public static String getLocalIpAddress() {
		String result="";
		boolean exit=false;
	    try {
	        for (Enumeration<NetworkInterface> en = 
	        		NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = 
	            		intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();) {
	            	InetAddress inetAddress = enumIpAddr.nextElement();
//	                if (!inetAddress.isLoopbackAddress() && !(inetAddress.toString().indexOf(":")>=0)) {
//	                    return inetAddress.getHostAddress().toString();
//	                }
//	            	Log.v("","ip="+inetAddress.getHostAddress()+
//	            			", name="+intf.getName());
	            	if (inetAddress.isSiteLocalAddress()) {
	                    result=inetAddress.getHostAddress();
//	                    Log.v("","result="+result+", name="+intf.getName()+"-");
	                    if (intf.getName().equals("wlan0")) {
	                    	exit=true;
	                    	break;
	                    }
	            	}
	            }
	            if (exit) break;
	        }
	    } catch (SocketException ex) {
	        Log.e(DEBUG_TAG, ex.toString());
	        result="192.168.0.1";
	    }
//		Log.v("","getLocalIpAddress result="+result);
	    if (result.equals("")) result="192.168.0.1";
	    return result;
	};

	public static String getIfIpAddress() {
		String result="";
	    try {
	        for (Enumeration<NetworkInterface> en = 
	        		NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = 
	            		intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();) {
	            	InetAddress inetAddress = enumIpAddr.nextElement();
//	            	Log.v("","ip="+inetAddress.getHostAddress());
	            	if (!inetAddress.isLoopbackAddress() &&
	            			(inetAddress.getHostAddress().startsWith("0") || 
	            					inetAddress.getHostAddress().startsWith("1") || 
	            					inetAddress.getHostAddress().startsWith("2"))) {
	                    result=inetAddress.getHostAddress();
	                    break;
	            	}
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(DEBUG_TAG, ex.toString());
	        result="192.168.0.1";
	    }
//		Log.v("","getIfIpAddress result="+result);
	    if (result.equals("")) result="192.168.0.1";
	    return result;
	};

	public void scanRemoteNetworkDlg(final NotifyEvent p_ntfy, String port_number) {
		//カスタムダイアログの生成
	    final Dialog dialog=new Dialog(mContext);
	    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setCanceledOnTouchOutside(false);
	    dialog.setContentView(R.layout.scan_remote_ntwk_dlg);
	    final Button btn_scan=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
	    final Button btn_cancel=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
	    final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);
	    final TextView tv_result = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_title);
	    tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_press_scan_btn));
	    tv_result.setVisibility(TextView.GONE);
	    
		final String from=getLocalIpAddress();
		String subnet=from.substring(0,from.lastIndexOf("."));
		String subnet_o1, subnet_o2,subnet_o3;
		subnet_o1=subnet.substring(0,subnet.indexOf("."));
		subnet_o2=subnet.substring(subnet.indexOf(".")+1,subnet.lastIndexOf("."));
		subnet_o3=subnet.substring(subnet.lastIndexOf(".")+1,subnet.length());
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		baEt1.setText(subnet_o1);
		baEt2.setText(subnet_o2);
		baEt3.setText(subnet_o3);
		baEt4.setText("1");
		baEt4.setSelection(1);
		eaEt4.setText("254");
		baEt4.requestFocus();
		
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.scan_remote_ntwk_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);
		
	    CommonDialog.setDlgBoxSizeLimit(dialog, true);
	    
	    if (port_number.equals("")) {
		    et_port_number.setEnabled(false);
		    cb_use_port_number.setChecked(false);
	    } else {
		    et_port_number.setEnabled(true);
		    et_port_number.setText(port_number);
		    cb_use_port_number.setChecked(true);
	    }
	    cb_use_port_number.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				et_port_number.setEnabled(isChecked);
			}
	    });
	    
	    final NotifyEvent ntfy_lv_click=new NotifyEvent(mContext);
	    ntfy_lv_click.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
	            dialog.dismiss();
				p_ntfy.notifyToListener(true,o);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
	    });
	    
		final ArrayList<ScanAddressResultListItem> ipAddressList = new ArrayList<ScanAddressResultListItem>();
//		ScanAddressResultListItem li=new ScanAddressResultListItem();
//		li.server_name=mContext.getString(R.string.msgs_ip_address_no_address);
//		ipAddressList.add(li);
	    final ListView lv = (ListView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_list);
	    final AdapterScanAddressResultList adap=new AdapterScanAddressResultList
		    	(mContext, R.layout.scan_address_result_list_item, ipAddressList, ntfy_lv_click);
	    lv.setAdapter(adap);
	    lv.setScrollingCacheEnabled(false);
	    lv.setScrollbarFadingEnabled(false);
	    
	    //SCANボタンの指定
	    btn_scan.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            ipAddressList.clear();
	            NotifyEvent ntfy=new NotifyEvent(mContext);
	    		ntfy.setListener(new NotifyEventListener() {
	    			@Override
	    			public void positiveResponse(Context c,Object[] o) {
	    				if (ipAddressList.size()<1) {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_not_detected));
	    					tv_result.setVisibility(TextView.GONE);
	    				} else {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_select_detected_host));
	    					tv_result.setVisibility(TextView.VISIBLE);
	    				}
//	    				adap.clear();
//	    				for (int i=0;i<ipAddressList.size();i++) 
//	    					adap.add(ipAddressList.get(i));
	    			}
	    			@Override
	    			public void negativeResponse(Context c,Object[] o) {}

	    		});
				if (auditScanAddressRangeValue(dialog)) {
					tv_result.setVisibility(TextView.GONE);
					String ba1=baEt1.getText().toString();
					String ba2=baEt2.getText().toString();
					String ba3=baEt3.getText().toString();
					String ba4=baEt4.getText().toString();
					String ea4=eaEt4.getText().toString();
					String subnet=ba1+"."+ba2+"."+ba3;
					int begin_addr = Integer.parseInt(ba4);
					int end_addr = Integer.parseInt(ea4);
					scanRemoteNetwork(dialog,lv,adap,ipAddressList,
							subnet, begin_addr, end_addr, ntfy);
				} else {
					//error
				}
	        }
	    });

	    //CANCELボタンの指定
	    btn_cancel.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            dialog.dismiss();
	            p_ntfy.notifyToListener(false, null);
	        }
	    });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
	    dialog.show();

	};

	private int mScanCompleteCount=0, mScanAddrCount=0;
	private String mLockScanCompleteCount="";
	private void scanRemoteNetwork(
			final Dialog dialog,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String subnet, final int begin_addr, final int end_addr,
			final NotifyEvent p_ntfy) {
		final Handler handler=new Handler();
		final ThreadCtrl tc=new ThreadCtrl();
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final TextView tvmsg=(TextView) dialog.findViewById(R.id.scan_remote_ntwk_progress_msg);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		final Button scan_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_progress_cancel);
		
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.scan_remote_ntwk_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);

		tvmsg.setText("");
		scan_cancel.setText(R.string.msgs_progress_spin_dlg_addr_cancel);
		ll_addr.setVisibility(LinearLayout.GONE);
		ll_prog.setVisibility(LinearLayout.VISIBLE);
		btn_scan.setEnabled(false);
		btn_cancel.setEnabled(false);
		adap.setButtonEnabled(false);
		scan_cancel.setEnabled(true);
	    dialog.setOnKeyListener(new DialogBackKeyListener(mContext));
	    dialog.setCancelable(false);
		// CANCELボタンの指定
		scan_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				scan_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				scan_cancel.setEnabled(false);
				sendDebugLogMsg(1,"W","IP Address list creation was cancelled");
				tc.setDisabled();
			}
		});
		dialog.show();
		
		sendDebugLogMsg(1,"I","Scan IP address ransge is "+subnet+ "."+begin_addr+" - "+end_addr);
		
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		String p_txt=String.format(scan_prog,0);
		tvmsg.setText(p_txt);

       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				mScanCompleteCount=0;
				mScanAddrCount=end_addr-begin_addr+1;
				int scan_thread=50;
				String scan_port="";
				if (cb_use_port_number.isChecked()) scan_port=et_port_number.getText().toString();
				for (int i=begin_addr; i<=end_addr;i+=scan_thread) {
					if (!tc.isEnabled()) break;
					boolean scan_end=false;
					for (int j=i;j<(i+scan_thread);j++) {
						if (j<=end_addr) {
							startRemoteNetworkScanThread(handler, tc, dialog, p_ntfy,
									lv_ipaddr, adap, tvmsg, subnet+"."+j,ipAddressList, scan_port);
						} else {
							scan_end=true;
						}
					}
					if (!scan_end) {
						for (int wc=0;wc<210;wc++) {
							if (!tc.isEnabled()) break;
							SystemClock.sleep(30);
						}
					}
				}
				if (!tc.isEnabled()) {
					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
						}
					});
				} else {
					handler.postDelayed(new Runnable() {// UI thread
						@Override
						public void run() {
							closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
						}
					},10000);
				}
			}
		})
       	.start();
	};

	private void closeScanRemoteNetworkProgressDlg(
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg) {
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		ll_addr.setVisibility(LinearLayout.VISIBLE);
		ll_prog.setVisibility(LinearLayout.GONE);
		btn_scan.setEnabled(true);
		btn_cancel.setEnabled(true);
		adap.setButtonEnabled(true);
	    dialog.setOnKeyListener(null);
	    dialog.setCancelable(true);
		if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
		
	};
	
	private void startRemoteNetworkScanThread(final Handler handler,
			final ThreadCtrl tc,
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg,
			final String addr,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String scan_port) {
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		Thread th=new Thread(new Runnable() {
			@Override
			public void run() {
				if (isIpAddrSmbHost(addr,scan_port)) {
					synchronized(mLockScanCompleteCount) {
						mScanCompleteCount++;
						String srv_name=getSmbHostName(addr);
						ScanAddressResultListItem li=new ScanAddressResultListItem();
						li.server_address=addr;
						li.server_name=srv_name;
						ipAddressList.add(li);
						Collections.sort(ipAddressList, new Comparator<ScanAddressResultListItem>(){
							@Override
							public int compare(ScanAddressResultListItem lhs,
									ScanAddressResultListItem rhs) {
								return lhs.server_address.compareTo(rhs.server_address);
							}
						});
					}
				} else {
					synchronized(mLockScanCompleteCount) {
						mScanCompleteCount++;
					}
				}
				handler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						synchronized(mLockScanCompleteCount) {
							lv_ipaddr.setSelection(lv_ipaddr.getCount());
							adap.notifyDataSetChanged();
							String p_txt=String.format(scan_prog, 
									(mScanCompleteCount*100)/mScanAddrCount);
							tvmsg.setText(p_txt);
							
							if (mScanCompleteCount>=mScanAddrCount) {
								closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
							}
						}
					}
				});
			}
       	});
       	th.start();
	};

	private boolean isIpAddrSmbHost(String address, String scan_port) {
		boolean smbhost=false;
		boolean reachable=NetworkUtil.ping(address);
		if (reachable) {
			if (scan_port.equals("")) {
				if (!NetworkUtil.isIpAddressAndPortConnected(address,139,3000)) {
					smbhost=NetworkUtil.isIpAddressAndPortConnected(address,445,3000);
				} else smbhost=true;
			} else {
				smbhost=NetworkUtil.isIpAddressAndPortConnected(address,
						Integer.parseInt(scan_port),3000);
			}
		}
		sendDebugLogMsg(2,"I","isIpAddrSmbHost Address="+address+
				", port="+scan_port+", reachable="+reachable+", smbhost="+smbhost);
		return smbhost;
	};

//	@SuppressWarnings("unused")
//	private boolean isNbtAddressActive(String address) {
//		boolean result=NetworkUtil.isNbtAddressActive(address);
//    	sendDebugLogMsg(1,"I","isSmbHost Address="+address+", result="+result);
//		return result;
//	};
	
	private String getSmbHostName(String address) {
		String srv_name=NetworkUtil.getSmbHostNameFromAddress(address);
       	sendDebugLogMsg(1,"I","getSmbHostName Address="+address+", name="+srv_name);
    	return srv_name;
 	};

	private boolean auditScanAddressRangeValue(Dialog dialog) {
		boolean result=false;
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);

		String ba1=baEt1.getText().toString();
		String ba2=baEt2.getText().toString();
		String ba3=baEt3.getText().toString();
		String ba4=baEt4.getText().toString();
		String ea4=eaEt4.getText().toString();
		
    	tvmsg.setText("");
		if (ba1.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt1.requestFocus();
			return false;
		} else if (ba2.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt2.requestFocus();
			return false;
		} else if (ba3.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt3.requestFocus();
			return false;
		} else if (ba4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt4.requestFocus();
			return false;
		} else if (ea4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt4.requestFocus();
			return false;
		}
		int iba1 = Integer.parseInt(ba1);
		if (iba1>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt1.requestFocus();
			return false;
		}
		int iba2 = Integer.parseInt(ba2);
		if (iba2>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt2.requestFocus();
			return false;
		}
		int iba3 = Integer.parseInt(ba3);
		if (iba3>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
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
					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_addr_gt_end_addr));
				}
			} else {
				eaEt4.requestFocus();
				tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_range_error));
			}
		} else {
			baEt4.requestFocus();
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
		}

//		if (iba1==192&&iba2==168) {
//			//class c private
//		} else {
//			if (iba1==10) {
//				//class a private
//			} else {
//				if (iba1==172 && (iba2>=16&&iba2<=31)) {
//					//class b private
//				} else {
//					//not private
//					result=false;
//					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_not_private));
//				}
//			}
//		}
		
		return result;
	};

	public void importProfileDlg(final String curr_dir, String file_name) {

		sendDebugLogMsg(1,"I","Import profile dlg.");

		NotifyEvent ne=new NotifyEvent(mContext);
		// set commonDialog response 
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			String fpath=(String)o[0];
    			
				ProfileListAdapter tfl = createProfileList(true, fpath);
				if (tfl!=null) {
					profileAdapter = tfl;
					saveProfile(false,"","");
					commonDlg.showCommonDialog(false,"I",
							String.format(getString(R.string.msgs_select_import_dlg_success),
									fpath),"",null);
    			}

			}

			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(curr_dir,
				"/SMBExplorer",file_name,"Select import file.",ne);
	};

	public void exportProfileDlg(final String curr_dir, final String ifn) {
		sendDebugLogMsg(1,"I","Export profile.");

		NotifyEvent ne=new NotifyEvent(mContext);
		// set commonDialog response 
		ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			String fpath=(String)o[0];
    			String fd=fpath.substring(0,fpath.lastIndexOf("/"));
    			String fn=fpath.replace(fd+"/","");
				exportProfileToFile(fd,fn);
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {}
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
			ne.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
					saveProfile(true,profile_dir,profile_filename);
					commonDlg.showCommonDialog(false,"I",
							String.format(getString(R.string.msgs_select_export_dlg_success),
									profile_dir+"/"+profile_filename),"",null);
				}
				
				@Override
				public void negativeResponse(Context c,Object[] o) {}
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
		smbUser=user;
		smbPass=pass;
	};
	
	private void initJcifsOption() {
		System.setProperty("jcifs.util.loglevel", mGp.jcifs_option_log_level);
		System.setProperty("jcifs.smb.lmCompatibility", "0");
		System.setProperty("jcifs.smb.client.useExtendedSecurity", "false");

		System.setProperty("jcifs.smb.client.tcpNoDelay",mGp.jcifs_option_tcp_nodelay);
        
		System.setProperty("jcifs.smb.client.rcv_buf_size", mGp.jcifs_option_rcv_buf_size);//60416 120832
		System.setProperty("jcifs.smb.client.snd_buf_size", mGp.jcifs_option_snd_buf_size);//16644 120832
    
		System.setProperty("jcifs.smb.client.listSize",mGp.jcifs_option_listSize); //65536 1300
		System.setProperty("jcifs.smb.maxBuffers",mGp.jcifs_option_maxBuffers);//16 100
	};
	

	private void sendLogMsg(String cat, String logmsg) {
		String m_txt=cat+" "+"MAIN    "+" "+logmsg;
		Log.v(DEBUG_TAG,m_txt);
	};
	
	private void sendDebugLogMsg(int lvl, String cat, String logmsg) {

		if (mGp.debugLevel>=lvl) {
			Log.v(DEBUG_TAG,cat+" "+"MAIN    "+" "+logmsg);
		}
	};

	private void saveTaskData() {
		ActivityDataHolder data = new ActivityDataHolder();

		data.remote_file_list_cache=remoteFileListCache;
		data.remote_curr_file_list=remoteCurrFLI;
		data.local_file_list_cache=localFileListCache;
		data.local_curr_file_list=localCurrFLI;
		
		data.paste_list=pasteFromList;
		data.paste_from_url=pasteFromUrl;
//		data.paste_to_url=pasteToUrl;
		data.paste_item_list=pasteItemList;
		data.is_paste_copy=isPasteCopy;
		data.is_paste_enabled=isPasteEnabled;
		data.is_paste_from_local=isPasteFromLocal;
		
		data.local_dir_hist=localDirHist;
		data.remote_dir_hist=remoteDirHist;
		
	    data.vsa=new ViewSaveArea();
	    saveViewStatus(data.vsa);
	    
	    data.dialog_msg_cat=mDialogMsgCat;
		try {
		    File lf =
			    	new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
//		    FileOutputStream fos = openFileOutput(SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
		    FileOutputStream fos = new FileOutputStream(lf);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(data);
		    oos.close();
		    sendDebugLogMsg(1,"I", "Task data was saved");
		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", "saveTaskData error, "+e.toString());
		}
	};
	
	private static final String SERIALIZABLE_FILE_NAME="Serial.dat";
	private void restoreTaskData() {
		try {
		    File lf =
		    	new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
//		    FileInputStream fis = openFileInput(SMBSYNC_SERIALIZABLE_FILE_NAME);
		    FileInputStream fis = new FileInputStream(lf); 
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    ActivityDataHolder data = (ActivityDataHolder) ois.readObject();
		    ois.close();
		    lf.delete();

		    remoteFileListCache=data.remote_file_list_cache;
		    remoteCurrFLI=data.remote_curr_file_list;
		    localFileListCache=data.local_file_list_cache;
		    localCurrFLI=data.local_curr_file_list;
		    
			localFileListAdapter =new FileListAdapter(mContext);
			localFileListAdapter.setShowLastModified(true);
			localFileListAdapter.setDataList(localCurrFLI.file_list);

			if (remoteCurrFLI!=null && remoteCurrFLI.file_list!=null) {
				remoteFileListAdapter =new FileListAdapter(this);
				remoteFileListAdapter.setShowLastModified(true);
				remoteFileListAdapter.setDataList(remoteCurrFLI.file_list);
			}
//			
			localFileListView.setAdapter(localFileListAdapter);
			remoteFileListView=(ListView)findViewById(R.id.explorer_filelist_remote_tab_listview);
			remoteFileListDirSpinner=(Spinner)findViewById(R.id.explorer_filelist_remote_tab_dir);
			remoteFileListView.setAdapter(remoteFileListAdapter);
			
			pasteFromList=data.paste_list;
			pasteFromUrl=data.paste_from_url;
//			pasteToUrl=data.paste_to_url;
			pasteItemList=data.paste_item_list;
			isPasteCopy=data.is_paste_copy;
			isPasteEnabled=data.is_paste_enabled;
			isPasteFromLocal=data.is_paste_from_local;
			
			if (data.local_dir_hist!=null) localDirHist=data.local_dir_hist;
			if (data.remote_dir_hist!=null) remoteDirHist=data.remote_dir_hist;

			mDialogMsgCat=data.dialog_msg_cat;
			
			restoreViewStatus(data.vsa);
			sendDebugLogMsg(1,"I", "Task data was restored");
		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", "restoreTaskData error, "+e.toString());
		}
	};
	
	private boolean isTaskDataExisted() {
    	File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
	    return lf.exists();
	};

	private void deleteTaskData() {
		File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
	    if (lf.exists()) {
	    	lf.delete();
	    	sendDebugLogMsg(1,"I", "Task data was deleted");
	    }
	};

	@SuppressLint("SimpleDateFormat")
	static public FileListItem createNewFilelistItem(FileListItem tfli) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		FileListItem fi=null;
		if (tfli.canRead()) {
			if (tfli.isDir()) {
				fi= new FileListItem(tfli.getName(), 
						sdf.format(tfli.getLastModified())+", ",
						true, 
						0,
						0,
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),tfli.getPath(),
						tfli.getListLevel());
				fi.setSubDirItemCount(tfli.getSubDirItemCount());
				fi.setHasExtendedAttr(tfli.hasExtendedAttr());
			} else {
			    String tfs = MiscUtil.convertFileSize(tfli.getLength());

				fi=new FileListItem(tfli.getName(), 
					sdf.format(tfli.getLastModified())+","+tfs, 
					false, 
					tfli.getLength(),
					tfli.getLastModified(),
					false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getPath(),
					tfli.getListLevel());
				fi.setHasExtendedAttr(tfli.hasExtendedAttr());
			}
		} else {
			fi=new FileListItem(tfli.getName(), 
					sdf.format(tfli.getLastModified())+","+0, false,
					tfli.getLength(),tfli.getLastModified(),false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getPath(),
					tfli.getListLevel());
			fi.setEnableItem(false);
			fi.setHasExtendedAttr(tfli.hasExtendedAttr());
		}
		return fi;
	};
	
	@SuppressLint("SimpleDateFormat")
	static public FileListItem createNewFilelistItem(File tfli, int sdc, int ll) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		FileListItem fi=null;
		if (tfli.canRead()) {
			if (tfli.isDirectory()) {
				fi= new FileListItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+", ",
						true, 
						0,
						0,
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),tfli.getParent(),
						ll);
				fi.setSubDirItemCount(sdc);
			} else {
			    String tfs = MiscUtil.convertFileSize(tfli.length());

				fi=new FileListItem(tfli.getName(), 
					sdf.format(tfli.lastModified())+","+tfs, 
					false, 
					tfli.length(),
					tfli.lastModified(),
					false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getParent(),
					ll);
			}
		} else {
			if (tfli.isDirectory()) {
				fi= new FileListItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+", ",
						true, 
						0,
						0,
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),tfli.getParent(),
						ll);
				fi.setSubDirItemCount(0);
			} else {
			    String tfs = MiscUtil.convertFileSize(tfli.length());

				fi=new FileListItem(tfli.getName(), 
					sdf.format(tfli.lastModified())+","+tfs, 
					false, 
					tfli.length(),
					tfli.lastModified(),
					false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getParent(),
					ll);
			}
		}
		return fi;
	};

	@SuppressLint("SimpleDateFormat")
	static public FileListItem createNewFilelistItem(SmbFile tfli, int sdc, int ll) throws SmbException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		FileListItem fi=null;
		boolean has_ea=tfli.getAttributes()<16384?false:true;
		try {
			String fp=tfli.getParent();
			if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
			if (tfli.canRead()) {
				if (tfli.isDirectory()) {
					fi= new FileListItem(tfli.getName(), 
							sdf.format(tfli.lastModified())+", ",
							true, 
							0,
							0,
							false,
							tfli.canRead(),tfli.canWrite(),
							tfli.isHidden(),fp,
							ll);
					fi.setSubDirItemCount(sdc);
					fi.setHasExtendedAttr(has_ea);
				} else {
				    String tfs = MiscUtil.convertFileSize(tfli.length());

					fi=new FileListItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+","+tfs, 
						false, 
						tfli.length(),
						tfli.lastModified(),
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),fp,
						ll);
					fi.setHasExtendedAttr(has_ea);
				}
			} else {
				fi=new FileListItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+","+0, false,
						tfli.length(),tfli.lastModified(),false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),fp,
						ll);
				fi.setEnableItem(false);
				fi.setHasExtendedAttr(has_ea);
			}
		} catch(SmbException e) {
			
		}
		return fi;
	};

	@SuppressLint("InflateParams")
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
	
	private MyUncaughtExceptionHandler myUncaughtExceptionHandler = new MyUncaughtExceptionHandler();
	class MyUncaughtExceptionHandler implements UncaughtExceptionHandler{
		private boolean mCrashing=false;
	    private UncaughtExceptionHandler defaultUEH;
		public void init() {
			defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
	        Thread.currentThread().setUncaughtExceptionHandler(myUncaughtExceptionHandler);
		}
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
        	try {
                if (!mCrashing) {
                    mCrashing = true;
                    
                	StackTraceElement[] st=ex.getStackTrace();
                	String st_msg="";
                	for (int i=0;i<st.length;i++) {
                		st_msg+="\n at "+st[i].getClassName()+"."+
                				st[i].getMethodName()+"("+st[i].getFileName()+
                				":"+st[i].getLineNumber()+")";
                	}
        			String end_msg=ex.toString()+st_msg;
        			
        			String end_msg2="";
        			st_msg="";
        			Throwable cause=ex.getCause();
        			if (cause!=null) {
            			st=cause.getStackTrace();
            			if (st!=null) {
                        	for (int i=0;i<st.length;i++) {
                        		st_msg+="\n at "+st[i].getClassName()+"."+
                        				st[i].getMethodName()+"("+st[i].getFileName()+
                        				":"+st[i].getLineNumber()+")";
                        	}
                			end_msg2="Caused by:"+cause.toString()+st_msg;
            			}
        			}

        			sendLogMsg("E", end_msg);
        			if (!end_msg2.equals("")) sendLogMsg("E", end_msg2);
        			
//        			File ldir=new File(mGp.settingsLogFileDir);
//        			if (!ldir.exists()) ldir.mkdirs();
//        			
//            		File lf=new File(mGp.settingsLogFileDir+"exception.txt");
//            		try {
//            			FileWriter fw=new FileWriter(lf,true);
//    					PrintWriter pw=new PrintWriter(fw);
//    					pw.println(end_msg);
//    					pw.println(end_msg2);
//    					pw.flush();
//    					pw.close();
//    				} catch (FileNotFoundException e) {
//    					e.printStackTrace();
//    				} catch (IOException e) {
//    					e.printStackTrace();
//    				}

                }
            } finally {
                defaultUEH.uncaughtException(thread, ex);
            }
        }
};

}
class ActivityDataHolder implements Externalizable  {

	private static final long serialVersionUID = 1L;

	ArrayList<String> local_dir_hist=null, remote_dir_hist=null;

	ArrayList<FileListCacheItem> remote_file_list_cache=null;
	FileListCacheItem remote_curr_file_list=null;
	ArrayList<FileListCacheItem> local_file_list_cache=null;
	FileListCacheItem local_curr_file_list=null;
	
	ArrayList<FileListItem> paste_list=null;
	String paste_from_url=null, paste_to_url=null, paste_item_list=null;
	boolean is_paste_copy=false,is_paste_enabled=false, is_paste_from_local=false;
	
	public ViewSaveArea vsa=null;
	
	public String dialog_msg_cat="";

	public ActivityDataHolder() {};
	
	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
//		Log.v("","rd");
		long sid=objin.readLong();
		if (serialVersionUID!=sid) {
			throw new IOException("serialVersionUID was not matched by saved UID");
		}

		paste_list=(ArrayList<FileListItem>) SerializeUtil.readArrayList(objin);
		paste_from_url=SerializeUtil.readUtf(objin);
		paste_to_url=SerializeUtil.readUtf(objin);
		paste_item_list=SerializeUtil.readUtf(objin);
		
		is_paste_copy=objin.readBoolean();
		is_paste_enabled=objin.readBoolean();
		is_paste_from_local=objin.readBoolean();
		
		local_dir_hist=(ArrayList<String>) SerializeUtil.readArrayList(objin);
		remote_dir_hist=(ArrayList<String>) SerializeUtil.readArrayList(objin);
		
		remote_file_list_cache=(ArrayList<FileListCacheItem>) SerializeUtil.readArrayList(objin);
		remote_curr_file_list=(FileListCacheItem) objin.readObject();
		local_file_list_cache=(ArrayList<FileListCacheItem>) SerializeUtil.readArrayList(objin);
		local_curr_file_list=(FileListCacheItem) objin.readObject();
		vsa=new ViewSaveArea();
		vsa.readExternal(objin);
		dialog_msg_cat=objin.readUTF();
	}

	@Override
	public void writeExternal(ObjectOutput objout) throws IOException {
//		Log.v("","wr");
		objout.writeLong(serialVersionUID);
		SerializeUtil.writeArrayList(objout, paste_list);
		SerializeUtil.writeUtf(objout, paste_from_url);
		SerializeUtil.writeUtf(objout, paste_to_url);
		SerializeUtil.writeUtf(objout, paste_item_list);
		
		objout.writeBoolean(is_paste_copy);
		objout.writeBoolean(is_paste_enabled);
		objout.writeBoolean(is_paste_from_local);
		
		SerializeUtil.writeArrayList(objout, local_dir_hist);
		SerializeUtil.writeArrayList(objout, remote_dir_hist);
		
		SerializeUtil.writeArrayList(objout, remote_file_list_cache);
		objout.writeObject(remote_curr_file_list);
		SerializeUtil.writeArrayList(objout, local_file_list_cache);
		objout.writeObject(local_curr_file_list);
		vsa.writeExternal(objout);
		objout.writeUTF(dialog_msg_cat);
	}
};

class ViewSaveArea implements Externalizable{
	private static final long serialVersionUID = 1L;
	public int progressVisible=LinearLayout.GONE;
	public String progressCancelBtnText="";
	public String progressMsgText="";
	public int profPos,profPosTop=0,lclPos,lclPosTop=0,remPos=0,remPosTop=0;
	
	public int dialogVisible=LinearLayout.GONE;
	public String dialogMsgText="";
	
	ViewSaveArea() {}
	
	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		@SuppressWarnings("unused")
		long sid=input.readLong();
		progressVisible=input.readInt();
		progressCancelBtnText=input.readUTF();
		progressMsgText=input.readUTF();
		lclPos=input.readInt();
		lclPosTop=input.readInt();
		profPos=input.readInt();
		profPosTop=input.readInt();
		remPos=input.readInt();
		remPosTop=input.readInt();
		dialogVisible=input.readInt();
		dialogMsgText=input.readUTF();
	}
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeLong(serialVersionUID);
		output.writeInt(progressVisible);
		output.writeUTF(progressCancelBtnText);
		output.writeUTF(progressMsgText);
		output.writeInt(lclPos);
		output.writeInt(lclPosTop);
		output.writeInt(profPos);
		output.writeInt(profPosTop);
		output.writeInt(remPos);
		output.writeInt(remPosTop);
		output.writeInt(dialogVisible);
		output.writeUTF(dialogMsgText);

	}
}

class FileListCacheItem implements Serializable{
	private static final long serialVersionUID = 1L;
	public String profile_name="";
	public String base="";
	public String directory="";
	public int pos_fv=0, pos_top=0;
	public ArrayList<String> directory_history=new ArrayList<String>();
	public ArrayList<FileListItem> file_list=null;
}
