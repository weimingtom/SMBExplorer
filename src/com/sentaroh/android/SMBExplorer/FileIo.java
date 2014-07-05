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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import com.sentaroh.android.Utilities.*;

import static com.sentaroh.android.SMBExplorer.Constants.*;

public class FileIo implements Runnable {
//	private final static boolean DEBUG = true;
//	private final static boolean DEBUG = false;
	private final static String DEBUG_TAG = "SMBExplorer";
	
	private static final boolean setLastModified=true;

	private static PrintWriter mLogWriter=null;
	
	private static int debugLevel = 0;
	
	private static int SMB_BUFF_SIZE =65536*4;
	
	private static NotifyEvent notifyEvent ;

	private static boolean fileioTaskResultOk = true;
	
	private static ThreadCtrl fileioThreadCtrl;
	
	private static ArrayList<FileIoLinkParm> fileioLinkParm;
	
	private static int file_op_cd;
	private static String file_tgt_url1, file_tgt_url2, file_tgt_name;
	private static String file_tgt_newname;
	private static String file_userid, file_password;
	private static boolean allcopy=true;
	
	private static Context currContext=null;
	
	private static String jcifs_option_iobuff="";

	private static boolean settingsMslScan = false;
	
	private static MediaScannerConnection mediaScanner ;
	
	private static Handler uiHandler = new Handler() ;

    private static byte[] fileIoArea = null;

	private static NtlmPasswordAuthentication ntlmPaswordAuth;
	
	private static TextView mProgressDlgMsg=null;
	
	// @Override
	public FileIo(TextView tv_msg, int op_cd,
			ArrayList<FileIoLinkParm> alp, ThreadCtrl tc, int dl,NotifyEvent ne, 
			Context cc, PrintWriter lw) {
		
		mProgressDlgMsg=tv_msg;
		fileioThreadCtrl=tc;
		file_op_cd=op_cd;
		fileioLinkParm=alp;
		debugLevel=dl;
		notifyEvent=ne;
		
		currContext=cc;
		
		mLogWriter=lw;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cc);
		settingsMslScan=prefs.getBoolean("settings_msl_scan", false);
		
		mediaScanner = new MediaScannerConnection(currContext,
				new MediaScannerConnectionClient() {
			@Override
			public void onMediaScannerConnected() {
				sendDebugLogMsg(1,"I","MediaScanner connected.");
			};

			@Override
			public void onScanCompleted(String path, Uri uri) {
				sendDebugLogMsg(2,"I","MediaScanner scan completed. fn="+
						path+", Uri="+uri);
			};
		});
		mediaScanner.connect();

		String cp=prefs.getString(currContext.getString(R.string.settings_smb_perform_class), "");
		if (cp.equals("0") || cp.equals("")) {
			jcifs_option_iobuff="4";
		} else if (cp.equals("1")) {
			jcifs_option_iobuff="4";
		} else if (cp.equals("2")) {
			jcifs_option_iobuff="8";
		} else {
			jcifs_option_iobuff=
					prefs.getString(currContext.getString(R.string.settings_io_buffers), "8");
		}
		SMB_BUFF_SIZE=Integer.parseInt(jcifs_option_iobuff)*65536;			
	};
	
    private void waitMediaScanner(boolean ds) {
    	boolean time_out=false;
    	int timeout_val=0;
    	try {
    		while(true) {
    			if (mediaScanner.isConnected()==ds) break;
    			Thread.sleep(10);
    			timeout_val++;
    			if (timeout_val>=501) {
    				time_out=true;
    				break;
    			}
    		}
		} catch (InterruptedException e) {
			e.printStackTrace();
			sendLogMsg("E","MediaScannerConnection wait error:"+e.toString());
		}
    	if (time_out) 
    		sendLogMsg("E","MediaScannerConnection timeout occured.");
    };
	
    private long taskBeginTime=0;
    
	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		final WakeLock wake_lock=((PowerManager)currContext.getSystemService(Context.POWER_SERVICE))
	    			.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBExplorer-ScreenOn");
		try {
			if (fileIoArea==null) {
//				Log.v("","allocated");
			    fileIoArea = new byte[4096*32];
			}

			wake_lock.acquire();
			sendLogMsg("I","Task has started.");
			
			taskBeginTime=System.currentTimeMillis();
			
			waitMediaScanner(true);
			
			for (int i=0;i<fileioLinkParm.size();i++) {
				file_tgt_url1 = fileioLinkParm.get(i).getUrl1();
				file_tgt_url2 = fileioLinkParm.get(i).getUrl2();
				file_tgt_name = fileioLinkParm.get(i).getName();
				file_tgt_newname = fileioLinkParm.get(i).getNew();
				file_userid = fileioLinkParm.get(i).getUser();
				file_password = fileioLinkParm.get(i).getPass();
				allcopy=fileioLinkParm.get(i).isAllCopyEnabled();

				String tuser=null,tpass=null;
				if (file_userid!=null && !file_userid.equals("")) tuser=file_userid;
				if (file_password!=null && !file_password.equals("")) tpass=file_password;
		    	ntlmPaswordAuth = new NtlmPasswordAuthentication( null,tuser,tpass);

				sendDebugLogMsg(9,"I","FILEIO task invoked."+
						" url1="+file_tgt_url1+
						", url2="+file_tgt_url2+
						", name="+file_tgt_name+", new="+file_tgt_newname+
						", uid="+file_userid+
						", password="+file_password);
				fileOperation();
				if (!fileioTaskResultOk) 
					break;
			}
			sendLogMsg("I","Task was ended. fileioTaskResultOk="+fileioTaskResultOk+
					", fileioThreadCtrl:"+fileioThreadCtrl.toString());
			sendLogMsg("I","Task elapsed time="+(System.currentTimeMillis()-taskBeginTime));
			if (fileioTaskResultOk) {
				fileioThreadCtrl.setThreadResultSuccess();
				sendDebugLogMsg(1,"I","Task was endeded without error.");			
			} else if (fileioThreadCtrl.isEnabled()) {
				fileioThreadCtrl.setThreadResultError();
				sendLogMsg("W","Task was ended with error.");
			} else {
				fileioThreadCtrl.setThreadResultCancelled();
				sendLogMsg("W","Task was cancelled.");
			}
			fileioThreadCtrl.setDisabled();
			mediaScanner.disconnect();
			waitMediaScanner(false);

			uiHandler.post(new Runnable() {// UI thread
				@Override
				public void run() {
					notifyEvent.notifyToListener(true, null);
				}
			});		
		} finally {
			wake_lock.release();
		}
	};
	
	private void fileOperation() {
		WifiManager wm = 
				(WifiManager)currContext.getSystemService(Context.WIFI_SERVICE);
		WifiLock wl=wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBSync-wifi");
		try {
			switch (file_op_cd) {
				case FILEIO_PARM_LOCAL_CREATE:
					fileioTaskResultOk=createLocalDir(
							file_tgt_url1+"/"+file_tgt_name);
					break;
				case FILEIO_PARM_REMOTE_CREATE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=createRemoteDir(
							file_tgt_url1+"/"+file_tgt_name+"/");
					break;
				case FILEIO_PARM_LOCAL_RENAME:
					fileioTaskResultOk=renameLocalItem(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname);
					break;
				case FILEIO_PARM_REMOTE_RENAME:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=renameRemoteItem(
							file_tgt_url1+"/"+file_tgt_name+"/", 
							file_tgt_url2+"/"+file_tgt_newname+"/");
					break;
				case FILEIO_PARM_LOCAL_DELETE:
					fileioTaskResultOk=deleteLocalItem(
							file_tgt_url1+"/"+file_tgt_name);
					break;
				case FILEIO_PARM_REMOTE_DELETE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=deleteRemoteItem(
							file_tgt_url1+"/"+file_tgt_name+"/");
					break;
				case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=copyRemoteToLocal(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname);
					break;
				case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=copyRemoteToRemote(
							file_tgt_url1+"/"+file_tgt_name+"/", 
							file_tgt_url2+"/"+file_tgt_newname+"/");
					break;
				case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
					fileioTaskResultOk=copyLocalToLocal(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname);
					break;
				case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=copyLocalToRemote(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname+"/");
					break;
				case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=copyRemoteToLocal(
							file_tgt_url1+"/"+file_tgt_name+"/", 
							file_tgt_url2+"/"+file_tgt_newname);
					if (fileioTaskResultOk)
						fileioTaskResultOk=deleteRemoteItem(
								file_tgt_url1+"/"+file_tgt_name+"/");
					break;
				case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
					if (!wl.isHeld()) wl.acquire();
					if (file_tgt_url1.equals(file_tgt_url2)) {
						fileioTaskResultOk=moveRemoteToRemote(
								file_tgt_url1+"/"+file_tgt_name+"/", 
								file_tgt_url2+"/"+file_tgt_newname+"/");
					} else {
						fileioTaskResultOk=copyRemoteToRemote(
								file_tgt_url1+"/"+file_tgt_name+"/", 
								file_tgt_url2+"/"+file_tgt_newname);
						if (fileioTaskResultOk)
							fileioTaskResultOk=deleteRemoteItem(
									file_tgt_url1+"/"+file_tgt_name+"/");
					}
					break;
				case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
					fileioTaskResultOk=moveLocalToLocal(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname);
					break;
				case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=copyLocalToRemote(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_newname+"/");
					if (fileioTaskResultOk)
						fileioTaskResultOk=deleteLocalItem(
								file_tgt_url1+"/"+file_tgt_name);
					break;
				case FILEIO_PARM_DOWLOAD_REMOTE_FILE:
					if (!wl.isHeld()) wl.acquire();
					fileioTaskResultOk=downloadRemoteFile(
							file_tgt_url1+"/"+file_tgt_name, 
							file_tgt_url2+"/"+file_tgt_name);
					break;
	
				default:
					break;
			};
		} finally {
			if (wl.isHeld()) wl.release();
		}
	};
	
	private static String mPrevProgMsg="";
	private static void sendMsgToProgDlg(final String log_msg) {
//		if (debugLevel>0) Log.v(DEBUG_TAG,"P "+msg);
		if (!mPrevProgMsg.equals(log_msg)) {
			mPrevProgMsg=log_msg;
			uiHandler.post(new Runnable() {// UI thread
				@Override
				public void run() {
					mProgressDlgMsg.setText(log_msg);
//					Log.v("","pop="+log_msg);
				}
			});		
		}
	};

	static private void sendLogMsg(final String log_cat, final String log_msg) {
		String m_txt=log_cat+" "+"FileIO  "+" "+log_msg;
		Log.v(DEBUG_TAG,m_txt);
		synchronized(mLogWriter) {
			mLogWriter.println(DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis())+" "+m_txt);
		}
	};

	static private void sendDebugLogMsg(int lvl, final String log_cat, final String log_msg) {

		if (debugLevel>0) Log.v(DEBUG_TAG,log_msg);
	};
	
	private static boolean createLocalDir(String newUrl) {
    	File sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	sendDebugLogMsg(1,"I","Create local dir item="+newUrl);
    	
    	try {
    		result=true;
    		sf = new File( newUrl );
    		
    		if (sf.exists()) return false;
    		
    		sf.mkdir();
			sendLogMsg("I",newUrl+" was created");
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
	
    private static boolean createRemoteDir(String newUrl) {
    	SmbFile sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	sendDebugLogMsg(1,"I","Create remote dir item="+newUrl);
    	
    	try {
    		result=true;
    		sf = new SmbFile( newUrl,ntlmPaswordAuth);
    		
    		if (sf.exists()) return false;
    		
    		sf.mkdir();
    		sendLogMsg("I",newUrl+" was created");
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
	
    private static boolean renameRemoteItem(String oldUrl, String newUrl) {
    	SmbFile sf,sfd;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	sendDebugLogMsg(1,"I","Rename remote item="+oldUrl);
    	
    	try {
    		result=true;
    		sf = new SmbFile( oldUrl,ntlmPaswordAuth );
    		sfd = new SmbFile( newUrl,ntlmPaswordAuth );
    		
    		sf.renameTo(sfd);
    		sendLogMsg("I",oldUrl+" was renamed to "+newUrl);
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Rename error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Rename error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
    
    private static boolean renameLocalItem(String oldUrl, String newUrl) {
    	File sf,sfd;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	sendDebugLogMsg(1,"I","Rename local item="+oldUrl);
    	
    	try {
    		sf = new File( oldUrl );
    		if (sf.isDirectory()) sfd = new File( newUrl+"/" );
    		else sfd = new File( newUrl );
			if (sf.renameTo(sfd)) {
				result=true;
				sendLogMsg("I",oldUrl+" was renamed to "+newUrl);
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Rename error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Rename error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };

    private static boolean deleteLocalItem(String url) {
    	File sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	//url="/sdcard/NEW";
    	sendDebugLogMsg(1,"I","Delete local file entered, File="+url);
    	
    	try {
    		sf = new File( url );
			result=deleteLocalFile(sf);
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Local file delete error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Local file delete error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };

    private static boolean deleteLocalFile(File lf) {
        //ファイルやフォルダを削除  
        //フォルダの場合、中にあるすべてのファイルやサブフォルダも削除されます  
        if (lf.isDirectory()) {//ディレクトリの場合  
            String[] children = lf.list();//ディレクトリにあるすべてのファイルを処理する  
            for (int i=0; i<children.length; i++) {  
            	if (!fileioThreadCtrl.isEnabled()) return false;
            	boolean success = deleteLocalFile(new File(lf, children[i]));
                if (!success) {  
                    return false;  
                }  
            }
        }  
	    // 削除  
        if (!fileioThreadCtrl.isEnabled()) return false;
	    boolean result=lf.delete();
	    if (settingsMslScan) deleteMediaStoreItem(lf.getPath());
	    sendMsgToProgDlg(lf.getName()+" was deleted");
	    sendLogMsg("I","File was Deleted. File="+lf.getPath());
	    return result;
        
    };
    
    private static boolean deleteRemoteItem(String url) {
    	SmbFile sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnabled()) return false;
    	
    	sendDebugLogMsg(1,"I","Delete remote file entered, File="+url);
    	
    	try {
    		result=true;
			sf = new SmbFile( url+"/",ntlmPaswordAuth );
			result=deleteRemoteFile(sf);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
 
    private static boolean deleteRemoteFile(SmbFile rf) {
        //ファイルやフォルダを削除  
        //フォルダの場合、中にあるすべてのファイルやサブフォルダも削除されます  
    	try {
			if (rf.isDirectory()) {//ディレクトリの場合  
	            String[] children = rf.list();//ディレクトリにあるすべてのファイルを処理する  
	            for (int i=0; i<children.length; i++) {  
	            	if (!fileioThreadCtrl.isEnabled()) return false;
	            	
	            	boolean success = deleteRemoteFile(new SmbFile(rf, children[i]+"/"));
	                if (!success) {  
	                    return false;  
	                }  
	            }
	        }  
		    // 削除  
	        if (!fileioThreadCtrl.isEnabled()) return false;
		    rf.delete();
		    sendMsgToProgDlg(rf.getName().replace("/", "")+" was deleted");
		    sendLogMsg("I","File was Deleted. File="+rf.getPath());
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Remote file delete error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Remote file delete error:"+e.toString());
			return false;
		}
	    return true;
        
    };
    
    private static boolean copyLocalToLocal(String fromUrl, String toUrl)  {
        File iLf ;
        boolean result = true;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Copy Local to Local from="+fromUrl+", to="+toUrl);
                
		try {
			iLf = new File(fromUrl );
			if (iLf.isDirectory()) { // Directory copy
				iLf = new File(fromUrl+"/");
				
				String[] children = iLf.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	if (!copyLocalToLocal(fromUrl+"/"+element, toUrl+"/"+element )) 
	            		return false;
	            }
				makeLocalDirs(toUrl+"/");
			} else { // file copy
				makeLocalDirs(toUrl);
				result=copyFileLocalToLocal(iLf,fromUrl,toUrl,"Copying");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };
    
	@SuppressWarnings("unused")
	private static String makeRemoteTempFilePath(String  targetUrl) {
		String tmp_wu="";
		String last_sep="";
		if (targetUrl.endsWith("/")) {
			tmp_wu=targetUrl.substring(0,(targetUrl.length()-1));
			last_sep="/";
		} else tmp_wu=targetUrl;
		String target_dir1=tmp_wu.substring(0,tmp_wu.lastIndexOf("/"));
		String target_fn=tmp_wu.replace(target_dir1, "").substring(1);
		String tmp_target=target_dir1+"/SMBExplorer.work.tmp"+last_sep;
//		Log.v("","tmp="+tmp_target+", to="+targetUrl+", wu="+tmp_wu+", tdir1="+target_dir1+
//				", tfn="+target_fn);
		return tmp_target;
	}


    private static boolean copyRemoteToRemote(String fromUrl, String toUrl)  {
        SmbFile ihf, ohf = null;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","copy Remote to Remote from item="+fromUrl+", to item="+toUrl);
                
		String tmp_toUrl="";
		
		try {
			ihf = new SmbFile(fromUrl,ntlmPaswordAuth );
			if (ihf.isDirectory()) { // Directory copy
				result=true;
				ihf = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = ihf.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	boolean success=copyRemoteToRemote(
	            			fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!success) return false;
	            }
				makeRemoteDirs(toUrl+"/");
			} else { // file copy
				makeRemoteDirs(toUrl);
				tmp_toUrl=makeRemoteTempFilePath(toUrl);
				
				ohf = new SmbFile(tmp_toUrl,ntlmPaswordAuth);
				if (ohf.exists()) ohf.delete();
				result=true;
				if (!fileioThreadCtrl.isEnabled()) return false;
				if (ihf.getAttributes()<16384) { //no EA, copy was done
					result=copyFileRemoteToRemote(ihf,ohf,fromUrl,toUrl,"Copying");
					if (result) {
						SmbFile hfd=new SmbFile(toUrl,ntlmPaswordAuth);
						if (hfd.exists()) hfd.delete();
						ohf.renameTo(hfd);
					}

				} else {
					result=false;
					sendLogMsg("E","EA founded, copy canceled. path="+fromUrl);
					fileioThreadCtrl.setThreadMessage("Copy error:"+"EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			if (!tmp_toUrl.equals("")) {
				try {
					if (ohf.exists()) ohf.delete();
				} catch (SmbException e1) {
				}
			}
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    private static boolean copyRemoteToLocal(String fromUrl, String toUrl)  {
        SmbFile hf,hfd;
        File lf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Copy Remote to Local from item="+fromUrl+", to item="+toUrl);
                
		try {
			hf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (hf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	result=copyRemoteToLocal(fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!result) return false;
	            }
					
			} else { // file copy
				if (hf.getAttributes()<16384) { //no EA, copy was done
					makeLocalDirs(toUrl);
					lf=new File(toUrl);
					result=copyFileRemoteToLocal(hf, lf,toUrl, fromUrl,"Copying");
				} else {
					result=false;
					sendLogMsg("E","EA founded, copy canceled. path="+fromUrl);
					fileioThreadCtrl.setThreadMessage("Copy error:"+"EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };
    
    private static boolean copyLocalToRemote(String fromUrl, String toUrl)  {
        SmbFile ohf=null ;
        File ilf,lfd ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Copy Local to Remote from item="+fromUrl+", to item="+toUrl);

		String tmp_toUrl="";

		try {
			ilf = new File(fromUrl );
			if (ilf.isDirectory()) { // Directory copy
				result=true;
				lfd = new File(fromUrl+"/");
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = lfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	result=copyLocalToRemote(fromUrl+"/"+element, toUrl+element+"/" );
	            	if (!result) return false;
	            }
					
			} else { // file copy
				makeRemoteDirs(toUrl);
				tmp_toUrl=makeRemoteTempFilePath(toUrl);
				ohf = new SmbFile(tmp_toUrl,ntlmPaswordAuth);
				if (ohf.exists()) ohf.delete();
				result=copyFileLocalToRemote(ohf,ilf,fromUrl,toUrl,"Copying");
				if (result) {
					SmbFile hfd=new SmbFile(toUrl,ntlmPaswordAuth);
					if (hfd.exists()) hfd.delete();
					ohf.renameTo(hfd);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			if (!tmp_toUrl.equals("")) {
				try {
					if (ohf.exists()) ohf.delete();
				} catch (SmbException e1) {
				}
			}
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendDebugLogMsg(1,"E","Copy error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    private static boolean moveLocalToLocal(String fromUrl, String toUrl)  {
        File iLf,iLfd, oLf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Move Local to Local from item="+fromUrl+",to item="+toUrl);
                
		iLf = new File(fromUrl );
		if (iLf.isDirectory()) { // Directory copy
			result=true;
			iLfd = new File(fromUrl+"/");
			oLf = new File(toUrl);
			
			String[] children = iLfd.list();
			for (String element : children) {
				if (!fileioThreadCtrl.isEnabled()) return false;
		    	result=moveLocalToLocal(fromUrl+"/"+element, toUrl+"/"+element );
		    	if (!result) return false;
		    }
			makeLocalDirs(toUrl+"/");
			iLf.delete();
			sendLogMsg("I",fromUrl+" was deleted.");
		} else { // file rename
			if (!fileioThreadCtrl.isEnabled()) return false;
			makeLocalDirs(toUrl);
			
			if (isSameMountPoint(fromUrl,toUrl)) { // renameでMoveする
				oLf=new File(toUrl);
				oLf.delete();
				result=iLf.renameTo(oLf);
			} else {
				try {
					result=copyFileLocalToLocal(iLf,fromUrl,toUrl,"Copying");
//					Log.v("","result="+result+", n="+iLf.getPath());
					if (result) iLf.delete();
				} catch (IOException e) {
					e.printStackTrace();
					sendLogMsg("E","Copy error:"+e.toString());
					fileioThreadCtrl.setThreadMessage("Copy error:"+e.toString());
					result=false;
					return false;
				}
			}
			if (settingsMslScan) scanMediaStoreLibraryFile(toUrl);
			if (result) sendLogMsg("I",fromUrl+" was moved to "+toUrl);
			else sendLogMsg("I","Move was failed. fromUrl="+ fromUrl+", toUrl="+toUrl);
		}
		return result;
    };

    private static boolean isSameMountPoint(String f_fp, String t_fp) {
    	boolean result=false;
    	ArrayList<String> ml=LocalMountPoint.buildLocalMountPointList();
//    	for (int i=0;i<ml.size();i++) Log.v("","ml="+ml.get(i));
    	if (LocalMountPoint.isExternal2MountPioint(f_fp) || 
    			LocalMountPoint.isExternal2MountPioint(t_fp)) {
        	if (LocalMountPoint.isExternal2MountPioint(f_fp)==
        			LocalMountPoint.isExternal2MountPioint(t_fp))  
        		result=true;
    	} else {
    		int i,j;
    		for (i=ml.size()-1;i>=0;i--) {
    			if (f_fp.startsWith(ml.get(i)+"/")) break;
    		}
    		for (j=ml.size()-1;j>=0;j--) {
    			if (t_fp.startsWith(ml.get(j)+"/")) break;
    		}
    		if (i==j) result=true;
    	}
    	sendDebugLogMsg(1,"I","isSameMountPoint result="+result+", f_fp="+f_fp+", t_fp="+t_fp);
    	return result;
    };
    
    private static boolean moveRemoteToRemote(String fromUrl, String toUrl)  {
        SmbFile ihf,hfd, ohf = null;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Move Remote to Remote from item="+fromUrl+", to item="+toUrl);
                
		try {
			ihf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (ihf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	boolean success=moveRemoteToRemote(
	            			fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!success) return false;
	            }
				makeRemoteDirs(toUrl+"/");
				ihf=new SmbFile(fromUrl+"/" ,ntlmPaswordAuth);
				ihf.delete();
				sendLogMsg("I",fromUrl+" was deleted.");
			} else { // file move
				if (!fileioThreadCtrl.isEnabled()) return false;
				makeRemoteDirs(toUrl);
				
				String f_f1=fromUrl.replace("smb://","");
				String f_f2=f_f1.substring(0,f_f1.indexOf("/"));
				f_f1.replace(f_f2+"/", "");
				String f_f3=f_f1.substring(0,f_f1.indexOf("/"));
				String t_f1=toUrl.replace("smb://","");
				String t_f2=t_f1.substring(0,f_f1.indexOf("/"));
				f_f1.replace(t_f2+"/", "");
				String t_f3=t_f1.substring(0,f_f1.indexOf("/"));
				//smb://1.1.1.1/share/dir
				if (t_f3.equals(f_f3)) { // renameでMoveする
					ohf = new SmbFile(toUrl,ntlmPaswordAuth);
					ohf=new SmbFile(toUrl+"/",ntlmPaswordAuth);
					if (ohf.exists()) ohf.delete();
					ihf.renameTo(ohf);
					result=ohf.exists();
				} else {
					result=copyFileRemoteToRemote(ihf,ohf,fromUrl,toUrl,"Copying");
					if (result) ihf.delete();
				}
				if (result) sendLogMsg("I",fromUrl+" was moved to "+toUrl);
				else sendLogMsg("I","Move was failed. fromUrl="+ fromUrl+", toUrl="+toUrl);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Move error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Move error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Move error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };
    
    private static boolean downloadRemoteFile(String fromUrl, String toUrl)  {
        SmbFile hf,hfd;
        File lf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnabled()) return false;
        
        sendDebugLogMsg(1,"I","Download Remote file, from item="+fromUrl+", to item="+toUrl);
                
		try {
			hf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (hf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnabled()) return false;
	            	result=copyRemoteToLocal(fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!result) return false;
	            }
					
			} else { // file copy
				if (hf.getAttributes()<16384) { //no EA, copy was done
					makeLocalDirs(toUrl);
					result=true;
					lf=new File(toUrl);
					if (!allcopy && 
							!isFileDifferent(hf.lastModified(),hf.length(),
									lf.lastModified(),lf.length())) {
						sendDebugLogMsg(1,"I","Download was cancelled because file does not changed.");
					} else {
						result=copyFileRemoteToLocal(hf, lf,toUrl, fromUrl,"Downloading");
					}
				} else {
					result=false;
					sendLogMsg("E","EA founded, copy canceled. path="+fromUrl);
					fileioThreadCtrl.setThreadMessage("Download error:"+"EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Download error:"+e.toString());
			fileioThreadCtrl.setThreadMessage("Download error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

//	private boolean copyFileLocalToLocalByChannel(File iLf, String fromUrl, String toUrl,
//    		String title_header) 
//			throws IOException {
//    	
//        File oLf;
//		long t0 = System.currentTimeMillis();
//	    FileInputStream fin = new FileInputStream( iLf );
//	    FileChannel inCh = fin.getChannel();
//	    FileOutputStream fout = new FileOutputStream(toUrl);
//	    FileChannel outCh = fout.getChannel();
//	    int n=0;
//	    long tot = 0;
//	    long fileBytes=iLf.length();
//	    String fn=iLf.getName();
//	
//    	sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.",fn,0));
//
//	    mFileChBuffer.clear();
//	    while (( n = inCh.read( mFileChBuffer )) > 0) {
//            n=mFileChBuffer.position();
//            mFileChBuffer.flip();
//            outCh.write(mFileChBuffer);
//            tot += n;
//	        if (n<fileBytes) 
//	        	sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.",fn,
//	        					(tot*100)/fileBytes));
//	        mFileChBuffer.clear();
//        }
//	    
//	    inCh.close();
//	    outCh.close();
//	    
//	    oLf = new File(toUrl);
//	    boolean slm=false;
//	    if (setLastModified) slm=oLf.setLastModified(iLf.lastModified());
//	    long t = System.currentTimeMillis() - t0;
//	    if (settingsMslScan) scanMediaStoreLibraryFile(toUrl);
//	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+
//	    		tot + " bytes transfered in " + 
//	    		t  + " mili seconds at " + calTransferRate(tot,t));
//	    return true;
//	};

    private static boolean copyFileLocalToLocal(File iLf, String fromUrl, String toUrl,
    		String title_header) 
			throws IOException {
//    	return copyFileLocalToLocalByChannel(iLf, fromUrl, toUrl, title_header);
    	return copyFileLocalToLocalByStream(iLf, fromUrl, toUrl, title_header);
    }
    
	static private boolean copyFileLocalToLocalByStream(File iLf, String fromUrl, String toUrl,
    		String title_header) 
			throws IOException {
    	
		File oLf;
		long t0 = System.currentTimeMillis();
	    FileInputStream fin = new FileInputStream( iLf );
	    FileOutputStream fout = new FileOutputStream(toUrl);
	    BufferedInputStream bis=new BufferedInputStream(fin,SMB_BUFF_SIZE);
	    BufferedOutputStream bos=new BufferedOutputStream(fout,SMB_BUFF_SIZE);
	    int n=0;
	    long tot = 0;
	    long fileBytes=iLf.length();
	    String fn=iLf.getName();
	
    	sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.",fn,0));

	    while(( n = bis.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnabled()) {
				bis.close();
			    bos.close();
			    File t_lf=new File(toUrl);
			    t_lf.delete();
	    		return false;
	    	} 
	        bos.write(fileIoArea, 0, n );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
	    bis.close();
	    bos.flush();
	    bos.close();
	    
	    oLf = new File(toUrl);
	    if (setLastModified) oLf.setLastModified(iLf.lastModified());
	    long t = System.currentTimeMillis() - t0;
	    if (settingsMslScan) scanMediaStoreLibraryFile(toUrl);
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+
	    		tot + " bytes transfered in " + 
	    		t  + " mili seconds at " + calTransferRate(tot,t));
//	    Log.v("","copy success");
	    return true;
	}

	private static boolean copyFileRemoteToRemote(SmbFile ihf, SmbFile ohf,String fromUrl,
			String toUrl, String title_header) throws IOException {
		long t0 = System.currentTimeMillis();
	    SmbFileInputStream in = new SmbFileInputStream( ihf );
	    SmbFileOutputStream out = new SmbFileOutputStream( ohf ); 
	    BufferedInputStream bis=new BufferedInputStream(in,SMB_BUFF_SIZE);
	    BufferedOutputStream bos=new BufferedOutputStream(out,SMB_BUFF_SIZE);
	    int n=0;
	    long tot = 0;
	    long fileBytes=ihf.length();
	    String fn=ihf.getName().replaceAll("/", "");
    	sendMsgToProgDlg(
    			String.format(title_header+" %s %s%% completed.",fn,0));
	
	    while(( n = bis.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnabled()) {
				bis.close();
			    bos.close();
			    if (ohf.exists()) ohf.delete();
	    		return false;
	    	} 
	        bos.write( fileIoArea, 0, n );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(
	        			String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		bis.close();
		bos.flush();
	    bos.close();
	
	    long t = System.currentTimeMillis() - t0;
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+
	    		", "+tot + " bytes transfered in " + 
	    		t+" mili seconds at "+ calTransferRate(tot,t));
	
	    try {
		    if (setLastModified) ohf.setLastModified(ihf.lastModified());
	    } catch(SmbException e) {
	    	sendLogMsg("I","SmbFile#setLastModified() was failed, reason="+e.getMessage());
	    }

		return true;
	}

	private static boolean copyFileLocalToRemote(SmbFile hf, File lf,
			String fromUrl, String toUrl, String title_header) throws IOException {
		
		long t0 = System.currentTimeMillis();
	    SmbFileOutputStream out = new SmbFileOutputStream( hf );
	    FileInputStream in = new FileInputStream(fromUrl);
	    BufferedInputStream bis=new BufferedInputStream(in,SMB_BUFF_SIZE);
	    BufferedOutputStream bos=new BufferedOutputStream(out,SMB_BUFF_SIZE);
	    int n=0;
	    long tot = 0;
	    long fileBytes=lf.length();
	    String fn=lf.getName();
	    
    	sendMsgToProgDlg(
    			String.format(title_header+" %s %s%% completed.",fn,0));
	    
	    while(( n = bis.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnabled()) {
				bis.close();
			    bos.close();
			    if (hf.exists()) hf.delete();
	    		return false;
	    	}
	        bos.write( fileIoArea, 0, n );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(
	        			String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		bis.close();
		bos.flush();
	    bos.close();

	    long t = System.currentTimeMillis() - t0;
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+
	    		tot + " bytes transfered in " + 
	    		t  + " mili seconds at " + calTransferRate(tot,t));

	    try {
		    if (setLastModified) hf.setLastModified(lf.lastModified());
	    } catch(SmbException e) {
	    	sendLogMsg("I","SmbFile#setLastModified() was failed, reason="+e.getMessage());
	    }
		
		return true;
	}

	private static boolean copyFileRemoteToLocal(SmbFile hf, File lf, 
    		String toUrl, String fromUrl, String title_header) 
    		throws IOException {
		long t0 = System.currentTimeMillis();
	    SmbFileInputStream in = new SmbFileInputStream( hf );
	    FileOutputStream out = new FileOutputStream(toUrl);
	    BufferedInputStream bis=new BufferedInputStream(in,SMB_BUFF_SIZE);
	    BufferedOutputStream bos=new BufferedOutputStream(out,SMB_BUFF_SIZE);
	    int n;
		long tot = 0;
	    long fileBytes=hf.length();
	    String fn=hf.getName().replaceAll("/", "");
	    
    	sendMsgToProgDlg(
    			String.format(title_header+" %s,  %s%% completed.",fn,0));
	    
	    while(( n = bis.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnabled()) {
				bis.close();
			    bos.close();
			    File t_file=new File(toUrl);
			    t_file.delete();
	    		return false;
	    	}
	        bos.write( fileIoArea, 0, n );
	        tot += n;
	        if (tot<fileBytes) 
	        	sendMsgToProgDlg(
	        			String.format(title_header+" %s,  %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		bis.close();
		bos.flush();
	    bos.close();
	    
	    if (setLastModified) lf.setLastModified(hf.lastModified());
	    long t = System.currentTimeMillis() - t0;
	    
	    if (settingsMslScan) scanMediaStoreLibraryFile(toUrl);
	    
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+tot +
	    		" bytes transfered in " + 
	    		t+" mili seconds at " + calTransferRate(tot,t));
	    return true;
    }
    
	private static void scanMediaStoreLibraryFile(String fp) {
		String mt=isMediaFile(fp);
		if (!isNoMediaPath(fp) 
			&& (mt.startsWith("audio") || mt.startsWith("video") || mt.startsWith("image") )) { 
			File lf=new File(fp);
			sendDebugLogMsg(1,"I",
					"scanMediaStoreLibrary MediaScanner was invoked. fn="+fp+
					", lastModified="+lf.lastModified());
			mediaScanner.scanFile(fp, null);
		} else {
			if (mt==null) sendDebugLogMsg(1,"I",
				"scanMediaStoreLibrary hidden directory or .nomedia found, " +
				"MediaScanner does not invoked.");
			else sendDebugLogMsg(1,"I",
					"scanMediaStoreLibrary Mime type was not media, " +
					"MediaScanner does not invoked. mime type="+mt);
		}
	};

	private static String isMediaFile(String fp) {
		String mt=null;
		String fid="";
		if (fp.lastIndexOf(".")>0) {
			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
			fid=fid.toLowerCase();
		}
		mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		if (mt==null) return "";
		else return mt;
	};
	
    private static boolean isNoMediaPath(String path) {
        if (path == null) return false;

        if (path.indexOf("/.") >= 0) return true;

        int offset = 1;
        while (offset >= 0 && offset<path.lastIndexOf("/")) {
            int slashIndex = path.indexOf('/', offset);
            if (slashIndex > offset) {
                slashIndex++; // move past slash
                File file = new File(path.substring(0, slashIndex) + ".nomedia");
//                Log.v("","off="+offset+", si="+slashIndex+", p="+file.getPath());
                if (file.exists()) {
                    return true;
                }
            }
            offset = slashIndex;
        }
        return false;
    }
	@SuppressLint("InlinedApi")
	private static int deleteMediaStoreItem(String fp) {
		int dc_image=0, dc_audio=0, dc_video=0, dc_files=0;
		String mt=isMediaFile(fp);
		if (mt!=null && 
				(mt.startsWith("audio") ||
				 mt.startsWith("video") ||
				 mt.startsWith("image") )) {
	    	ContentResolver cri = currContext.getContentResolver();
	    	ContentResolver cra = currContext.getContentResolver();
	    	ContentResolver crv = currContext.getContentResolver();
	    	ContentResolver crf = currContext.getContentResolver();
	    	dc_image=cri.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
	       	dc_audio=cra.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Audio.Media.DATA + "=?", new String[]{fp} );
	       	dc_video=crv.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Video.Media.DATA + "=?", new String[]{fp} );
	        if(Build.VERSION.SDK_INT >= 11) {
	        	dc_files=crf.delete(MediaStore.Files.getContentUri("external"), 
	          		MediaStore.Files.FileColumns.DATA + "=?", new String[]{fp} );
	        }
       		sendLogMsg("I","deleMediaStoreItem fn="+fp+
	       				", delete count image="+dc_image+
	       				", audio="+dc_audio+", video="+dc_video+", files="+dc_files);
		} else {
       		sendDebugLogMsg(1,"I","deleMediaStoreItem not MediaStore library. fn="+
	       				fp+"");
		}
		
		return dc_image+dc_audio+dc_video+dc_files;
	};
    
    
    private static boolean isFileDifferent(long f1_lm, long f1_fl,long f2_lm, long f2_fl) {
    	boolean result=false;
    	if (f1_fl==f2_fl) {
    		long td=Math.abs(f1_lm-f2_lm)/1000;
    		if (td>=3) result=true;
    	} else result=true;
    	return result;
    };
    
	private static boolean makeRemoteDirs(String targetPath) 
					throws MalformedURLException, SmbException {
		boolean result=false;
		String target_dir1="";
		String target_dir2="";
		if (targetPath.lastIndexOf("/")<=0) return false;
		else {
			if (targetPath.endsWith("/")) {//path is dir
				target_dir1=targetPath.substring(0,targetPath.lastIndexOf("/"));
			} else {
				target_dir1=targetPath;
			}
			target_dir2=target_dir1.substring(0,target_dir1.lastIndexOf("/"));
		}

		SmbFile hf = new SmbFile(target_dir2 + "/",ntlmPaswordAuth);
//		Log.v("","tdir="+target_dir2);
		if (!hf.exists()) {
			hf.mkdirs();
		}
		return result;
	};
	
	private static boolean makeLocalDirs(String targetPath) {
		boolean result=false;
		String target_dir="";
		if (targetPath.lastIndexOf("/")<=0) return false;
		else target_dir=targetPath.substring(0,targetPath.lastIndexOf("/"));
		File lf = new File(target_dir);
		if (!lf.exists()) {
			lf.mkdirs();
			result=true;
		}
		return result;
	};

	private static String calTransferRate(long tb, long tt) {
	    String tfs = null;
	    BigDecimal bd_tr;
	    if (tb>(1024)) {//KB
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"KBytes/sec";
		} else {
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"Bytes/sec";
		}
		
		return tfs;
	};
}
