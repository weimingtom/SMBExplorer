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
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

import com.sentaroh.android.Utilities.*;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;

@SuppressLint("SimpleDateFormat")
public class RetrieveFileList implements Runnable  {
	private final static String DEBUG_TAG = "SMBExplorer";
	
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
	
	private NtlmPasswordAuthentication ntlmPaswordAuth;

	public RetrieveFileList(Context c, MsgListAdapter ma, ListView ml,
			ThreadCtrl ac, int dl, String ru, List<String> d_list,
			String user, String pass, NotifyEvent ne) {
//		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		debugLevel=dl;
		
		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		uiHandler=new Handler();
		
		dir_list=d_list;
		
		opCode="EC"; //check item is exists

		String tuser=null,tpass=null;
		if (!user.equals("")) tuser=user;
		if (!pass.equals("")) tpass=pass;
		ntlmPaswordAuth = new NtlmPasswordAuthentication( null,tuser,tpass);
	}

	
	public RetrieveFileList(Context c, MsgListAdapter ma, ListView ml,
			ThreadCtrl ac, int dl, String ru, 
			ArrayList<TreeFilelistItem> fl,String user, String pass, NotifyEvent ne) {
//		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		debugLevel=dl;
		remoteFileList=fl;
		
		uiHandler=new Handler();

		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		opCode="FL";
		
		String tuser=null,tpass=null;
		if (!user.equals("")) tuser=user;
		if (!pass.equals("")) tpass=pass;
		ntlmPaswordAuth = new NtlmPasswordAuthentication( null,tuser,tpass);

	}
	
	
	@Override
	public void run() {
		getFLCtrl.setThreadResultSuccess();
		
		sendDebugLogMsg(1,"I","getFileList started");
		
		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);

//        Log.v("","url="+remoteUrl);
		String host_t1=remoteUrl.replace("smb://","").replaceAll("//", "/");
		String host_t2=host_t1.substring(0,host_t1.indexOf("/"));
		String host_t3=host_t2;
//		Log.v("","1="+host_t1+", 2="+host_t2+", 3="+host_t3);
		if (host_t2.indexOf(":")>=0) host_t3=host_t2.substring(0,host_t2.indexOf(":"));
		boolean error=false;
		String err_msg="";
		if (NetworkUtil.isValidIpAddress(host_t3)) {
			if (!NetworkUtil.isNbtAddressActive(host_t3)) error=true;
			err_msg="Can not be connected to specified IP address, IP address="+host_t3;
		} else {
			if (NetworkUtil.getSmbHostIpAddressFromName(host_t3)==null) error=true;
			err_msg="Specified hostname is not found, Name="+host_t3;
		}
		if (error) {
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisable();
			getFLCtrl.setThreadMessage(err_msg);
		} else {
			if (opCode.equals("FL")) {
				remoteFileList.clear();
				readFileList(remoteUrl);
			} else if (opCode.equals("EC")) {
				checkItemExists(remoteUrl);
			}
			
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
					if (fl[i].canRead() && 
//							fl[i].isDirectory() && 
							!fn.equals("IPC$") &&
							!fn.equals("System Volume Information")) {
						String fp=fl[i].getParent();
						if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
						int dirct=0;
						try {
							if (fl[i].isDirectory()) {
								SmbFile tdf=new SmbFile(fl[i].getPath(),ntlmPaswordAuth);
								SmbFile[] tfl=tdf.listFiles();
								dirct=tfl.length;
							}
						} catch (SmbException e) {
							sendDebugLogMsg(0,"I","File ignored by exception: "+e.toString()+", "+
									"Name="+fn+", "+
									"isDirectory="+fl[i].isDirectory()+", "+
									"Length="+fl[i].length()+", "+
									"LastModified="+fl[i].lastModified()+", "+
									"CanRead="+fl[i].canRead()+", "+
									"CanWrite="+fl[i].canWrite()+", "+
									"isHidden="+fl[i].isHidden()+", "+
									"Parent="+fp+", " +
									"Path="+fl[i].getPath()+", "+
									"CanonicalPath="+fl[i].getCanonicalPath());
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
						sendDebugLogMsg(2,"I","Filelist added: "+
							"Name="+fn+", "+
							"isDirectory="+fl[i].isDirectory()+", "+
							"Length="+fl[i].length()+", "+
							"LastModified="+fl[i].lastModified()+", "+
							"CanRead="+fl[i].canRead()+", "+
							"CanWrite="+fl[i].canWrite()+", "+
							"isHidden="+fl[i].isHidden()+", "+
							"Parent="+fp+", " +
							"Path="+fl[i].getPath()+", "+
							"CanonicalPath="+fl[i].getCanonicalPath());
					}
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
		if (debugLevel>=lvl) {
			Log.v(DEBUG_TAG,"FILELIST"+" "+msg);
			uiHandler.post(new Runnable(){
				@Override
				public void run() {
					msglistAdapter.add(
							new MsgListItem(cat,
									sdfDate.format(System.currentTimeMillis()),
									sdfTime.format(System.currentTimeMillis()),
									"FILELIST",msg));
					msgListView.setSelection(msgListView.getCount());
				}
			});
		}
	};
	
}