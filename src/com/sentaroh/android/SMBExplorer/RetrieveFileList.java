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

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ListView;

import com.sentaroh.android.Utilities.*;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;

@SuppressLint("SimpleDateFormat")
public class RetrieveFileList implements Runnable  {
	private final static String DEBUG_TAG = "SMBExplorerGetFilelist";
	
	private int debugLevel = 0;

	private ListView msgListView;
	private MsgListAdapter msglistAdapter;

	private ThreadCtrl getFLCtrl=null;
	
	private ArrayList<TreeFilelistItem> remoteFileList;
	private String remoteUrl;//, currDir;
	private List<String> dir_list;
	
	private String opCode="FL";
	
	private NotifyEvent notifyEvent ;
	
	final static private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	final static private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");

	private Handler uiHandler=null;
	@SuppressWarnings("unused")
	private String jcifs_option_rcv_buf_size="",jcifs_option_snd_buf_size="",
			jcifs_option_listSize="",jcifs_option_maxBuffers="",jcifs_option_iobuff="",
					jcifs_option_tcp_nodelay="", jcifs_option_log_level="";
	
	private Context currContext;

	public RetrieveFileList(Context c, MsgListAdapter ma, ListView ml,
			ThreadCtrl ac, int dl, String ru, List<String> d_list,
			String user, String pass, NotifyEvent ne) {
		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		debugLevel=dl;
		
		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		uiHandler=new Handler();
		
		dir_list=d_list;
		
		opCode="EC"; //check item is exists
		
		sendDebugLogMsg(1,"I","getFileList constructed. user="+user+", url="+ru);
		sendDebugLogMsg(9,"I","getFileList constructed. pass="+pass);
		
		setJcifsOption();
		setJcifsProperties(user,pass);
	}

	
	public RetrieveFileList(Context c, MsgListAdapter ma, ListView ml,
			ThreadCtrl ac, int dl, String ru, 
			ArrayList<TreeFilelistItem> fl,String user, String pass, NotifyEvent ne) {
		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		debugLevel=dl;
		remoteFileList=fl;

		uiHandler=new Handler();

		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		sendDebugLogMsg(1,"I","getFileList constructed. user="+user+", url="+ru);
		sendDebugLogMsg(9,"I","getFileList constructed. pass="+pass);
		
		setJcifsOption();
		setJcifsProperties(user,pass);
	}
	
	
	@Override
	public void run() {
		getFLCtrl.setThreadResultSuccess();
		
		sendDebugLogMsg(1,"I","getFileList started");
		
		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);

		
		if (opCode.equals("FL")) {
			remoteFileList.clear();
			readFileList(remoteUrl);
		} else if (opCode.equals("EC")) {
			checkItemExists(remoteUrl);
		}
		sendDebugLogMsg(1,"I","getFileList terminated.");
		uiHandler.post(new Runnable(){
			@Override
			public void run() {
				notifyEvent.notifyToListener(true, null);
			}
		});
		getFLCtrl.setDisable();
		
	};
	
// Default uncaught exception handler variable
    private UncaughtExceptionHandler defaultUEH;
    
// handler listener
    private Thread.UncaughtExceptionHandler unCaughtExceptionHandler =
        new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
            	Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
            	ex.printStackTrace();
            	StackTraceElement[] st=ex.getStackTrace();
            	String st_msg="";
            	for (int i=0;i<st.length;i++) {
            		st_msg+="\n at "+st[i].getClassName()+"."+
            				st[i].getMethodName()+"("+st[i].getFileName()+
            				":"+st[i].getLineNumber()+")";
            	}
            	getFLCtrl.setThreadResultError();
    			String end_msg=ex.toString()+st_msg;
    			getFLCtrl.setThreadMessage(end_msg);
    			getFLCtrl.setDisable();
    			notifyEvent.notifyToListener(true, null);
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
    };

	
	private NtlmPasswordAuthentication ntlmPaswordAuth;
	private void setJcifsOption() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(currContext);
		String cp=
				prefs.getString(currContext.getString(R.string.settings_smb_perform_class), "");
		
		if (cp.equals("0")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="16644";
			jcifs_option_snd_buf_size="16644";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="";
			jcifs_option_iobuff="4";
			jcifs_option_tcp_nodelay="false";
		} else if (cp.equals("1")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="33288";
			jcifs_option_snd_buf_size="33288";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="100";
			jcifs_option_iobuff="4";
			jcifs_option_tcp_nodelay="false";
		} else if (cp.equals("2")) {
			jcifs_option_log_level="0";
			jcifs_option_rcv_buf_size="66576";
			jcifs_option_snd_buf_size="66576";
			jcifs_option_listSize="";
			jcifs_option_maxBuffers="100";
			jcifs_option_iobuff="8";
			jcifs_option_tcp_nodelay="true";
		} else {
			jcifs_option_log_level=
					prefs.getString(currContext.getString(R.string.settings_smb_log_level), "0");
			if (jcifs_option_log_level.length()==0) jcifs_option_log_level="0";
			
			jcifs_option_rcv_buf_size=
					prefs.getString(currContext.getString(R.string.settings_smb_rcv_buf_size),"");
			jcifs_option_snd_buf_size=
					prefs.getString(currContext.getString(R.string.settings_smb_snd_buf_size),"");
			jcifs_option_listSize=
					prefs.getString(currContext.getString(R.string.settings_smb_listSize), "");
			jcifs_option_maxBuffers=
					prefs.getString(currContext.getString(R.string.settings_smb_maxBuffers), "");
			jcifs_option_iobuff=
					prefs.getString(currContext.getString(R.string.settings_io_buffers), "");
			jcifs_option_tcp_nodelay=
					prefs.getString(currContext.getString(R.string.settings_smb_tcp_nodelay),"false");
		}
			
	};
	
	private void setJcifsProperties(String user, String pass) {
		System.setProperty("jcifs.util.loglevel", jcifs_option_log_level);
		System.setProperty("jcifs.smb.lmCompatibility", "0");
		System.setProperty("jcifs.smb.client.useExtendedSecurity", "false");

		String tuser=null,tpass=null;
		if (!user.equals("")) tuser=user;
		if (!pass.equals("")) tpass=pass;
		ntlmPaswordAuth = new NtlmPasswordAuthentication( null,tuser,tpass);
	};

	private void readFileList(String url) {
		sendDebugLogMsg(2,"I","Filelist directory: "+url);
		try {		
			SmbFile remoteFile = new SmbFile(url,ntlmPaswordAuth);
			SmbFile[] fl = remoteFile.listFiles();

			for (int i=0;i<fl.length;i++){
				if (getFLCtrl.isEnable()) {
//					String fn,String cp,boolean d,
//					long fl,long lm, boolean ic, boolean cr,boolean cw,boolean hd);
					String fn=fl[i].getName();
					if (fn.endsWith("/")) fn=fn.substring(0,fn.length()-1);
					String fp=fl[i].getParent();
					if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
					int dirct=0;
					if (fl[i].canRead() && fl[i].isDirectory() && !fn.equals("IPC$") &&
							!fn.equals(".android_secure") &&
							!fn.equals("System Volume Information")) {
						SmbFile tdf=new SmbFile(fl[i].getPath(),ntlmPaswordAuth);
						SmbFile[] tfl=tdf.listFiles();
						dirct=tfl.length;
					}
					TreeFilelistItem fi=new TreeFilelistItem(
							fn,
							"",
							fl[i].isDirectory(),
							fl[i].length(),
							fl[i].lastModified(),
							false,
							fl[i].canRead(),
							fl[i].canWrite(),
							fl[i].isHidden(),
							fp,0);
					fi.setSubDirItemCount(dirct);
					remoteFileList.add(fi);
					sendDebugLogMsg(2,"I","Filelist detail: "+
						"Name="+fn+","+
						"isDirectory="+fl[i].isDirectory()+","+
						"Length="+fl[i].length()+","+
						"LastModified="+fl[i].lastModified()+","+
						"CanRead="+fl[i].canRead()+","+
						"CanWrite="+fl[i].canWrite()+","+
						"isHidden="+fl[i].isHidden()+","+
						"Parent="+fp+"," +
						"Path="+fl[i].getPath()+","+
						"CanonicalPath="+fl[i].getCanonicalPath());
				} else {
					getFLCtrl.setThreadResultCancelled();
					sendDebugLogMsg(-1,"W","Cancelled by main task.");
					break;
				}
			}
		} catch (SmbException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisable();
			getFLCtrl.setThreadMessage(e.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisable();
			getFLCtrl.setThreadMessage(e.toString());
		}

	};

	private void checkItemExists(String url) {
		for (int i=0;i<dir_list.size();i++) {
			try {		
				SmbFile remoteFile = 
						new SmbFile(url+"/"+dir_list.get(i),ntlmPaswordAuth);
				if (!remoteFile.exists()) dir_list.set(i,"");
			} catch (SmbException e) {
				e.printStackTrace();
				sendDebugLogMsg(0,"E",e.toString());
				getFLCtrl.setThreadResultError();
				getFLCtrl.setDisable();
				getFLCtrl.setThreadMessage(e.toString());
			} catch (MalformedURLException e) {
				e.printStackTrace();
				sendDebugLogMsg(0,"E",e.toString());
				getFLCtrl.setThreadResultError();
				getFLCtrl.setDisable();
				getFLCtrl.setThreadMessage(e.toString());
			}
		}
	};
	
	
	private void sendDebugLogMsg(int lvl, final String cat, final String msg) {
		if (debugLevel>0) Log.v(DEBUG_TAG,msg);
		if (debugLevel>=lvl) {
			uiHandler.post(new Runnable(){
				@Override
				public void run() {
					msglistAdapter.add(
							new MsgListItem(cat,
									sdfDate.format(System.currentTimeMillis()),
									sdfTime.format(System.currentTimeMillis()),
									"DEBUG-I",msg));
					msgListView.setSelection(msgListView.getCount());
				}
			});
		}
	};
	
}