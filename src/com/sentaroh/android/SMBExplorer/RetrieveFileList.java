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
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.os.Handler;
import android.util.Log;
import com.sentaroh.android.Utilities.*;

public class RetrieveFileList implements Runnable  {
	private final static String DEBUG_TAG = "SMBExplorer";
	
	private ThreadCtrl getFLCtrl=null;
	
	private ArrayList<FileListItem> remoteFileList=null;
	private String remoteUrl;//, currDir;
	private List<String> dir_list;
	
	private String opCode="FL";
	
	private NotifyEvent notifyEvent ;
	
	private Handler uiHandler=null;
	
	private NtlmPasswordAuthentication ntlmPaswordAuth;
	
	private GlobalParameters mGp=null;

	public RetrieveFileList(GlobalParameters gp,  
			ThreadCtrl ac, String ru, List<String> d_list,
			String user, String pass, NotifyEvent ne) {
//		currContext=c;
		mGp=gp;
		
		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		uiHandler=new Handler();
		
		dir_list=d_list;
		
		opCode=OPCD_EXISTS_CHECK; //check item is exists

		String tuser=null,tpass=null;
		if (!user.equals("")) tuser=user;
		if (!pass.equals("")) tpass=pass;
		ntlmPaswordAuth = new NtlmPasswordAuthentication( null,tuser,tpass);
	}

	public final static String OPCD_FILE_LIST="FL"; 
	public final static String OPCD_EXISTS_CHECK="EC";
	
	public RetrieveFileList(GlobalParameters gp, 
			ThreadCtrl ac, String opcd, String ru, 
			ArrayList<FileListItem> fl,
			String user, String pass, NotifyEvent ne) {
		mGp=gp;
		remoteFileList=fl;
		
		uiHandler=new Handler();

		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		opCode=opcd;
		
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
		if (host_t2.indexOf(":")>=0) host_t3=host_t2.substring(0,host_t2.indexOf(":"));
		boolean error=false;
		String err_msg="";
		if (NetworkUtil.isValidIpAddress(host_t3)) {
			if (!isSmbHostAddressConnected(host_t3)) {
				error=true;
				err_msg="Can not be connected to specified IP address, IP address="+host_t3;
			}
		} else {
			if (NetworkUtil.getSmbHostIpAddressFromName(host_t3)==null) {
				error=true;
				err_msg="Specified hostname is not found, Name="+host_t3;
			}
		}
		
        Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);

		if (error) {
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisabled();
			getFLCtrl.setThreadMessage(err_msg);
		} else {
			if (opCode.equals(OPCD_FILE_LIST)) {
				remoteFileList.clear();
				ArrayList<FileListItem> tl=readFileList(remoteUrl);
				for (int i=0;i<tl.size();i++) remoteFileList.add(tl.get(i));
			} else if (opCode.equals(OPCD_EXISTS_CHECK)) {
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
		getFLCtrl.setDisabled();
		
	};
	
	public static boolean isSmbHostAddressConnected(String addr) {
		boolean result=false;
		if (NetworkUtil.isIpAddressAndPortConnected(addr,139,3500) || 
				NetworkUtil.isIpAddressAndPortConnected(addr,445,3500)) result=true;
		return result;
	}

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
    			getFLCtrl.setDisabled();
    			notifyEvent.notifyToListener(true, null);
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
    };
    
	private ArrayList<FileListItem> readFileList(String url) {
		ArrayList<FileListItem> rem_list=new ArrayList<FileListItem>();
		sendDebugLogMsg(2,"I","Filelist directory: "+url);
		try {		
			SmbFile remoteFile = new SmbFile(url,ntlmPaswordAuth);
			SmbFile[] fl = remoteFile.listFiles();

			for (int i=0;i<fl.length;i++){
				if (getFLCtrl.isEnabled()) {
//					String fn,String cp,boolean d,
//					long fl,long lm, boolean ic, boolean cr,boolean cw,boolean hd);
					String fn=fl[i].getName();
					if (fn.endsWith("/")) fn=fn.substring(0,fn.length()-1);
					if (fl[i].canRead() && 
//							fl[i].isDirectory() && 
							!fn.equals("IPC$") &&
							!fn.equals("System Volume Information")) {
						boolean has_ea=fl[i].getAttributes()<16384?false:true;
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
									"hasExtendedAttr="+has_ea+", "+
									"Parent="+fp+", " +
									"Path="+fl[i].getPath()+", "+
									"CanonicalPath="+fl[i].getCanonicalPath());
						}					
						FileListItem fi=new FileListItem(
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
						fi.setHasExtendedAttr(has_ea);
						rem_list.add(fi);
						sendDebugLogMsg(2,"I","Filelist added: "+
							"Name="+fn+", "+
							"isDirectory="+fl[i].isDirectory()+", "+
							"Length="+fl[i].length()+", "+
							"LastModified="+fl[i].lastModified()+", "+
							"CanRead="+fl[i].canRead()+", "+
							"CanWrite="+fl[i].canWrite()+", "+
							"isHidden="+fl[i].isHidden()+", "+
							"hasExtendedAttr="+has_ea+", "+
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
			getFLCtrl.setDisabled();
			getFLCtrl.setThreadMessage(e.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisabled();
			getFLCtrl.setThreadMessage(e.toString());
		}
		return rem_list;
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
				getFLCtrl.setDisabled();
				getFLCtrl.setThreadMessage(e.toString());
			} catch (MalformedURLException e) {
				e.printStackTrace();
				sendDebugLogMsg(0,"E",e.toString());
				getFLCtrl.setThreadResultError();
				getFLCtrl.setDisabled();
				getFLCtrl.setThreadMessage(e.toString());
			}
		}
	};
	
	
	private void sendDebugLogMsg(int lvl, final String cat, final String msg) {
		if (mGp.debugLevel>=lvl) {
			Log.v(DEBUG_TAG,"FILELIST"+" "+msg);
		}
	};
	
}