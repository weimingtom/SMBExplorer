package com.sentaroh.android.SMBExplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.TextView;
import com.sentaroh.android.Utilities.*;
import static com.sentaroh.android.SMBExplorer.Constants.*;

public class FileIo implements Runnable {
//	private final static boolean DEBUG = true;
//	private final static boolean DEBUG = false;
	private final static String DEBUG_TAG = "FileIO";
	
	private static final boolean setLastModified=true;
	
	private int debugLevel = 0;
	
	private MsglistAdapter msglistAdapter;
	private ListView msgListView;
	
	private int SMB_BUFF_SIZE =65536*4;
	
	private NotifyEvent notifyEvent ;

	private boolean fileioTaskResultOk = true;
	
	private Dialog threadDlg;
	private TextView dlgMsg;
	
	private ThreadCtrl fileioThreadCtrl;
	
	private ArrayList<FileIoLinkParm> fileioLinkParm;
	
	private Calendar calInstance;
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	private int file_op_cd;
	private String file_tgt_url1, file_tgt_url2, file_tgt_name;
	private String file_tgt_newname;
	private String file_userid, file_password;
	private boolean allcopy=true;
	
	private Context currContext=null;
	
	private String jcifs_option_rcv_buf_size="",jcifs_option_snd_buf_size="",
			jcifs_option_listSize="",jcifs_option_maxBuffers="",jcifs_option_iobuff="",
					jcifs_option_tcp_nodelay="", jcifs_option_log_level="";

	private boolean settingsMslScan = false;
	
	private MediaScannerConnection mediaScanner ;
	
	private Handler uiHandler = new Handler() ;
	
	// @Override
	public FileIo(ListView lv,
			MsglistAdapter ma, Dialog pd, int op_cd,
			ArrayList<FileIoLinkParm> alp, ThreadCtrl tc, int dl,NotifyEvent ne, 
			Context cc) {
		
		msgListView = lv;
		msglistAdapter = ma;
		
		threadDlg=pd;
		fileioThreadCtrl=tc;
		file_op_cd=op_cd;
		fileioLinkParm=alp;
		debugLevel=dl;
		notifyEvent=ne;
		
		currContext=cc;
		
		dlgMsg = (TextView) threadDlg.findViewById(R.id.progress_spin_dlg_msg);
		
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

		setJcifsOption();
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
	
	@Override
	public void run() {
		
		sendDebugLogMsg(1,"I","Task has started.");
		
		waitMediaScanner(true);
		
		for (int i=0;i<fileioLinkParm.size();i++) {
			file_tgt_url1 = fileioLinkParm.get(i).getUrl1();
			file_tgt_url2 = fileioLinkParm.get(i).getUrl2();
			file_tgt_name = fileioLinkParm.get(i).getName();
			file_tgt_newname = fileioLinkParm.get(i).getNew();
			file_userid = fileioLinkParm.get(i).getUser();
			file_password = fileioLinkParm.get(i).getPass();
			allcopy=fileioLinkParm.get(i).isAllCopyEnabled();
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
		sendDebugLogMsg(1,"I","Task was ended. fileioTaskResultOk="+fileioTaskResultOk+
				", fileioThreadCtrl:"+fileioThreadCtrl.toString());
		if (fileioTaskResultOk) {
			fileioThreadCtrl.setThreadResultSuccess();
			sendDebugLogMsg(1,"I","Task was endeded without error.");			
		} else if (fileioThreadCtrl.isEnable()) {
			fileioThreadCtrl.setThreadResultError();
			sendLogMsg("W","Task was ended with error.");
		}
		else {
			fileioThreadCtrl.setThreadResultCancelled();
			sendLogMsg("W","Task was cancelled.");
		}
		fileioThreadCtrl.setDisable();
		mediaScanner.disconnect();
		waitMediaScanner(false);
		notifyThreadTerminate(); //dismiss progress dialog
	};
	
	private void fileOperation() {
		
		switch (file_op_cd) {
			case FILEIO_PARM_LOCAL_CREATE:
				sendMsgToProgDlg(false,"","Creating local directory "+file_tgt_name);
				fileioTaskResultOk=createLocalDir(
						file_tgt_url1+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_REMOTE_CREATE:
				sendMsgToProgDlg(false,"","Creating remote directory "+file_tgt_name);
				setJcifsParm() ;
				fileioTaskResultOk=createRemoteDir(
						file_tgt_url1+"/"+file_tgt_name+"/");
				break;
			case FILEIO_PARM_LOCAL_RENAME:
				sendMsgToProgDlg(false,"","Renaming local "+file_tgt_name);
				fileioTaskResultOk=renameLocalItem(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_newname);
				break;
			case FILEIO_PARM_REMOTE_RENAME:
				sendMsgToProgDlg(false,"","Renaming remote "+file_tgt_name);
				setJcifsParm() ;
				fileioTaskResultOk=renameRemoteItem(
						file_tgt_url1+"/"+file_tgt_name+"/", 
						file_tgt_url2+"/"+file_tgt_newname+"/");
				break;
			case FILEIO_PARM_LOCAL_DELETE:
				sendMsgToProgDlg(false,"","Deleteing local "+file_tgt_name);
				fileioTaskResultOk=deleteLocalItem(
						file_tgt_url1+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_REMOTE_DELETE:
				sendMsgToProgDlg(false,"","Deleteing remote "+file_tgt_name);
				setJcifsParm() ;
				fileioTaskResultOk=deleteRemoteItem(
						file_tgt_url1+"/"+file_tgt_name+"/");
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_LOCAL:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Copying remote to local '"+file_tgt_name+"'");
				fileioTaskResultOk=copyRemoteToLocal(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_COPY_REMOTE_TO_REMOTE:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Copying remote to remote '"+file_tgt_name+"'");
				fileioTaskResultOk=copyRemoteToRemote(
						file_tgt_url1+"/"+file_tgt_name+"/", 
						file_tgt_url2+"/"+file_tgt_name+"/");
				break;
			case FILEIO_PARM_COPY_LOCAL_TO_LOCAL:
				sendMsgToProgDlg(false,"","Copying local to local '"+file_tgt_name+"'");
				fileioTaskResultOk=copyLocalToLocal(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_COPY_LOCAL_TO_REMOTE:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Copying local to remote '"+file_tgt_name+"'");
				fileioTaskResultOk=copyLocalToRemote(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name+"/");
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_LOCAL:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Moving remote to local '"+file_tgt_name+"'");
				fileioTaskResultOk=copyRemoteToLocal(
						file_tgt_url1+"/"+file_tgt_name+"/", 
						file_tgt_url2+"/"+file_tgt_name);
				if (fileioTaskResultOk)
					fileioTaskResultOk=deleteRemoteItem(
							file_tgt_url1+"/"+file_tgt_name+"/");
				break;
			case FILEIO_PARM_MOVE_REMOTE_TO_REMOTE:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Moving remote to remote '"+file_tgt_name+"'");
				if (file_tgt_url1.equals(file_tgt_url2)) {
					fileioTaskResultOk=moveRemoteToRemote(
							file_tgt_url1+"/"+file_tgt_name+"/", 
							file_tgt_url2+"/"+file_tgt_name+"/");
				} else {
					fileioTaskResultOk=copyRemoteToRemote(
							file_tgt_url1+"/"+file_tgt_name+"/", 
							file_tgt_url2+"/"+file_tgt_name);
					if (fileioTaskResultOk)
						fileioTaskResultOk=deleteRemoteItem(
								file_tgt_url1+"/"+file_tgt_name+"/");
				}
				break;
			case FILEIO_PARM_MOVE_LOCAL_TO_LOCAL:
				sendMsgToProgDlg(false,"","Moving local to local '"+file_tgt_name+"'");
				fileioTaskResultOk=moveLocalToLocal(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_MOVE_LOCAL_TO_REMOTE:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Moving local to remote '"+file_tgt_name+"'");
				fileioTaskResultOk=copyLocalToRemote(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name+"/");
				if (fileioTaskResultOk)
					fileioTaskResultOk=deleteLocalItem(
							file_tgt_url1+"/"+file_tgt_name);
				break;
			case FILEIO_PARM_DOWLOAD_RMOTE_FILE:
				setJcifsParm() ;
				sendMsgToProgDlg(false,"","Downloading remote file '"+file_tgt_name+"'");
				fileioTaskResultOk=downloadRemoteFile(
						file_tgt_url1+"/"+file_tgt_name, 
						file_tgt_url2+"/"+file_tgt_name);
				break;

			default:
				break;
		};
	};
	
	private void sendMsgToProgDlg(final boolean log, final String log_cat, final String log_msg) {
//		if (debugLevel>0) Log.v(DEBUG_TAG,"P "+msg);
		uiHandler.post(new Runnable() {// UI thread
			@Override
			public void run() {
				if (!log) {// arg2=1 both, arg2=0 dialog only, arg2=2 log only
					dlgMsg.setText(log_msg);
				} else { //
					Log.v(DEBUG_TAG,log_msg);
					dlgMsg.setText(log_msg);
					calInstance = Calendar.getInstance();
					msglistAdapter.add(
								new MsglistItem(log_cat,
										sdfDate.format(calInstance.getTime()),
										sdfTime.format(calInstance.getTime()),
										"MirrorIO",log_msg));
				}
				msgListView.setSelection(msgListView.getCount());
			}
		});		

		
	};

	private void sendLogMsg(final String log_cat, final String log_msg) {
		Log.v(DEBUG_TAG,log_msg);
		uiHandler.post(new Runnable() {// UI thread
			@Override
			public void run() {
				calInstance = Calendar.getInstance();
				msglistAdapter.add(
						new MsglistItem(log_cat,
								sdfDate.format(calInstance.getTime()),
								sdfTime.format(calInstance.getTime()),
								"MirrorIO",log_msg));
				msgListView.setSelection(msgListView.getCount());
			}
		});		
	};

	private void sendDebugLogMsg(int lvl, final String log_cat, final String log_msg) {

		if (debugLevel>0) Log.v(DEBUG_TAG,log_msg);
		if (debugLevel>=lvl) {
			uiHandler.post(new Runnable() {// UI thread
				@Override
				public void run() {
					calInstance = Calendar.getInstance();
					msglistAdapter.add(
							new MsglistItem(log_cat,
									sdfDate.format(calInstance.getTime()),
									sdfTime.format(calInstance.getTime()),
									"DEBUG-I",log_msg));
					msgListView.setSelection(msgListView.getCount());
				}
			});		
		}
	};
	
	private void notifyThreadTerminate() {

		if (debugLevel>0)  
			Log.v(DEBUG_TAG,"Send empty log msg");
		uiHandler.post(new Runnable() {// UI thread
			@Override
			public void run() {
				threadDlg.dismiss();
				notifyEvent.notifyTolistener(true, null);
			}
		});		

	};
	
	private boolean createLocalDir(String newUrl) {
    	File sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
    	sendDebugLogMsg(1,"I","Create local dir item="+newUrl);
    	
    	try {
    		result=true;
    		sf = new File( newUrl );
    		
    		if (sf.exists()) return false;
    		
    		sf.mkdir();
			sendDebugLogMsg(1,"I",newUrl+" was created");
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
	
    private boolean createRemoteDir(String newUrl) {
    	SmbFile sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
    	sendDebugLogMsg(1,"I","Create remote dir item="+newUrl);
    	
    	try {
    		result=true;
    		sf = new SmbFile( newUrl,ntlmPaswordAuth);
    		
    		if (sf.exists()) return false;
    		
    		sf.mkdir();
    		sendDebugLogMsg(1,"I",newUrl+" was created");
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Create error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
	
    private boolean renameRemoteItem(String oldUrl, String newUrl) {
    	SmbFile sf,sfd;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
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
			result=false;
			return false;
		}
    	return result;
    };
    
    private boolean renameLocalItem(String oldUrl, String newUrl) {
    	File sf,sfd;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
    	sendDebugLogMsg(1,"I","Rename local item="+oldUrl);
    	
    	try {
    		sf = new File( oldUrl );
    		if (sf.isDirectory()) sfd = new File( newUrl+"/" );
    		else sfd = new File( newUrl );
			if (sf.renameTo(sfd)) {
				result=true;
				sendDebugLogMsg(1,"I",oldUrl+" was renamed to "+newUrl);
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Rename error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };

    private boolean deleteLocalItem(String url) {
    	File sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
    	//url="/sdcard/NEW";
    	sendDebugLogMsg(1,"I","Delete local item="+url);
    	
    	try {
    		sf = new File( url );
			result=deleteLocalFile(sf);
		} catch (Exception e) {
			e.printStackTrace();
			sendLogMsg("E","Delete error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };

    private boolean deleteLocalFile(File lf) {
        //ファイルやフォルダを削除  
        //フォルダの場合、中にあるすべてのファイルやサブフォルダも削除されます  
    	sendDebugLogMsg(1,"I","Deleted:"+lf.getName()+", isdir="+lf.isDirectory());
        if (lf.isDirectory()) {//ディレクトリの場合  
            String[] children = lf.list();//ディレクトリにあるすべてのファイルを処理する  
            for (int i=0; i<children.length; i++) {  
            	if (!fileioThreadCtrl.isEnable()) return false;
            	boolean success = deleteLocalFile(new File(lf, children[i]));
                if (!success) {  
                    return false;  
                }  
            }
        }  
	    // 削除  
        if (!fileioThreadCtrl.isEnable()) return false;
	    boolean result=lf.delete();
	    if (settingsMslScan) deleteMediaStoreItem(lf.getPath());
	    sendMsgToProgDlg(false,"",lf.getName()+" was deleted");
	    sendDebugLogMsg(1,"I","Deleted:"+lf.getName()+", result="+result);
	    return result;
        
    };
    
    private boolean deleteRemoteItem(String url) {
    	SmbFile sf;
    	boolean result = false;
    	
    	if (!fileioThreadCtrl.isEnable()) return false;
    	
    	sendDebugLogMsg(1,"I","Delete remote item="+url);
    	
    	try {
    		result=true;
			sf = new SmbFile( url+"/",ntlmPaswordAuth );
			result=deleteRemoteFile(sf);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Delete error:"+e.toString());
			result=false;
			return false;
		}
    	return result;
    };
 
    private boolean deleteRemoteFile(SmbFile rf) {
        //ファイルやフォルダを削除  
        //フォルダの場合、中にあるすべてのファイルやサブフォルダも削除されます  
    	try {
				sendDebugLogMsg(1,"I","Deleted:"+rf.getName()+", isdir="+rf.isDirectory());
	        if (rf.isDirectory()) {//ディレクトリの場合  
	            String[] children = rf.list();//ディレクトリにあるすべてのファイルを処理する  
	            for (int i=0; i<children.length; i++) {  
	            	if (!fileioThreadCtrl.isEnable()) return false;
	            	
	            	boolean success = deleteRemoteFile(new SmbFile(rf, children[i]+"/"));
	                if (!success) {  
	                    return false;  
	                }  
	            }
	        }  
		    // 削除  
	        if (!fileioThreadCtrl.isEnable()) return false;
		    rf.delete();
		    sendMsgToProgDlg(true,"",rf.getName().replace("/", "")+" was deleted");
		    sendDebugLogMsg(1,"I","Deleted:"+rf.getName());
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Delete error:"+e.toString());
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Delete error:"+e.toString());
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Delete error:"+e.toString());
			return false;
		}
	    return true;
        
    };
    
    private boolean copyLocalToLocal(String fromUrl, String toUrl)  {
        File iLf ;
        boolean result = true;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Copy Local to Local from item="+fromUrl+",to item="+toUrl);
                
		try {
			iLf = new File(fromUrl );
			if (iLf.isDirectory()) { // Directory copy
				iLf = new File(fromUrl+"/");
				
				String[] children = iLf.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
	            	if (!copyLocalToLocal(fromUrl+"/"+element, toUrl+"/"+element )) 
	            		return false;
	            }
			} else { // file copy
				makeLocalDirs(toUrl);
				result=copyFileLocalToLocal(iLf,fromUrl,toUrl,"Copying");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    private boolean copyRemoteToRemote(String fromUrl, String toUrl)  {
        SmbFile ihf, ohf = null;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","copy Remote to Remote from item="+fromUrl+", to item="+toUrl);
                
		try {
			ihf = new SmbFile(fromUrl,ntlmPaswordAuth );
			if (ihf.isDirectory()) { // Directory copy
				result=true;
				ihf = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = ihf.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
	            	boolean success=copyRemoteToRemote(
	            			fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!success) return false;
	            }
			} else { // file copy
				makeRemoteDirs(toUrl);
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				result=true;
				if (!fileioThreadCtrl.isEnable()) return false;
				if (ihf.getAttributes()<16384) { //no EA, copy was done
					result=copyFileRemoteToRemote(ihf,ohf,fromUrl,toUrl,"Copying");
				} else {
					result=false;
					sendLogMsg("E","EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    private boolean copyRemoteToLocal(String fromUrl, String toUrl)  {
        SmbFile hf,hfd;
        File lf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Copy Remote to Local from item="+fromUrl+", to item="+toUrl);
                
		try {
			hf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (hf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
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
					sendLogMsg("E","EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };
    
    private boolean copyLocalToRemote(String fromUrl, String toUrl)  {
        SmbFile hf ;
        File lf,lfd ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Copy Local to Remote from item="+fromUrl+", to item="+toUrl);
                
		try {
			lf = new File(fromUrl );
			if (lf.isDirectory()) { // Directory copy
				result=true;
				lfd = new File(fromUrl+"/");
				hf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = lfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
	            	result=copyLocalToRemote(fromUrl+"/"+element, toUrl+element+"/" );
	            	if (!result) return false;
	            }
					
			} else { // file copy
				makeRemoteDirs(toUrl);
				hf = new SmbFile(toUrl,ntlmPaswordAuth);
				result=copyFileLocalToRemote(hf,lf,fromUrl,toUrl,"Copying");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendDebugLogMsg(1,"E","Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    private boolean moveLocalToLocal(String fromUrl, String toUrl)  {
        File iLf,iLfd, oLf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Move Local to Local from item="+fromUrl+",to item="+toUrl);
                
		iLf = new File(fromUrl );
		if (iLf.isDirectory()) { // Directory copy
			result=true;
			iLfd = new File(fromUrl+"/");
			oLf = new File(toUrl);
			
			String[] children = iLfd.list();
			for (String element : children) {
				if (!fileioThreadCtrl.isEnable()) return false;
		    	result=moveLocalToLocal(fromUrl+"/"+element, toUrl+"/"+element );
		    	if (!result) return false;
		    }
			iLf.delete();
			sendLogMsg("I",fromUrl+" was deleted.");
		} else { // file rename
			if (!fileioThreadCtrl.isEnable()) return false;
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

    private boolean isSameMountPoint(String f_fp, String t_fp) {
    	boolean result=false;
    	ArrayList<String> ml=LocalMountPoint.buildLocalMountPointList();
    	for (int i=0;i<ml.size();i++) Log.v("","ml="+ml.get(i));
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
    	return result;
    };

    private boolean moveRemoteToRemote(String fromUrl, String toUrl)  {
        SmbFile hf,hfd, ohf = null;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Move Remote to Remote from item="+fromUrl+", to item="+toUrl);
                
		try {
			hf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (hf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				ohf = new SmbFile(toUrl,ntlmPaswordAuth);
				
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
	            	boolean success=moveRemoteToRemote(
	            			fromUrl+"/"+element, toUrl+"/"+element );
	            	if (!success) return false;
	            }
				hf=new SmbFile(fromUrl+"/" ,ntlmPaswordAuth);
				hf.delete();
				sendLogMsg("I",fromUrl+" was deleted.");
			} else { // file move
				if (!fileioThreadCtrl.isEnable()) return false;
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
					hf.renameTo(ohf);
					result=ohf.exists();
				} else {
					result=copyFileRemoteToRemote(hf,ohf,fromUrl,toUrl,"Copying");
					if (!result) hf.delete();
				}
				if (result) sendLogMsg("I",fromUrl+" was moved to "+toUrl);
				else sendLogMsg("I","Move was failed. fromUrl="+ fromUrl+", toUrl="+toUrl);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Move error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };
    
    private boolean downloadRemoteFile(String fromUrl, String toUrl)  {
        SmbFile hf,hfd;
        File lf ;
        boolean result = false;
        
        if (!fileioThreadCtrl.isEnable()) return false;
        
        sendDebugLogMsg(1,"I","Download Remote file, from item="+fromUrl+", to item="+toUrl);
                
		try {
			hf = new SmbFile(fromUrl ,ntlmPaswordAuth);
			if (hf.isDirectory()) { // Directory copy
				result=true;
				hfd = new SmbFile(fromUrl+"/",ntlmPaswordAuth);
				String[] children = hfd.list();
				for (String element : children) {
					if (!fileioThreadCtrl.isEnable()) return false;
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
					sendLogMsg("E","EA founded, copy canceled");
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (SmbException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			sendLogMsg("E","Copy error:"+e.toString());
			result=false;
			return false;
		}
		return result;
    };

    byte[] fileIoArea = new byte[SMB_BUFF_SIZE];
//    ByteBuffer fileIoAreaX = ByteBuffer.allocateDirect(SMB_BUFF_SIZE*2);
    private boolean copyFileLocalToLocal(File iLf, String fromUrl, String toUrl,
    		String title_header) 
			throws IOException {
    	
		File oLf;
		long t0 = System.currentTimeMillis();
	    FileInputStream fin = new FileInputStream( iLf );
//	    FileChannel in = fin.getChannel();
	    FileOutputStream fout = new FileOutputStream(toUrl);
//	    FileChannel out = fout.getChannel();
	    long n=0;
	    long tot = 0;
	    long fileBytes=iLf.length();
	    String fn=iLf.getName();
	
	    sendMsgToProgDlg(false,"",title_header+" : "+iLf.getName());

//	    fileIoAreaX.clear();
//	    while (in.read(fileIoArea)>=0) {
//            n=fileIoAreaX.position();
//            fileIoAreaX.flip();
//            out.write(fileIoAreaX);
//            tot += n;
//	        if (n<fileBytes) 
//	        	sendMsgToProgDlg(false,"",String.format(title_header+" %s %s%% completed.",fn,
//	        					(tot*100)/fileBytes));
//	        fileIoAreaX.clear();
//        }
	    
	    while(( n = fin.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnable()) {
				fin.close();
			    fout.close();
	    		return false;
	    	} 
	        fout.write( fileIoArea );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(false,"",String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
//		in.close();
//	    out.close();
	    fin.close();
	    fout.close();
	    
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

	private boolean copyFileRemoteToRemote(SmbFile ihf, SmbFile ohf,String fromUrl,
			String toUrl, String title_header) throws IOException {
		long t0 = System.currentTimeMillis();
	    SmbFileInputStream in = new SmbFileInputStream( ihf );
	    SmbFileOutputStream out = new SmbFileOutputStream( ohf ); 
	    int n=0;
	    long tot = 0;
	    long fileBytes=ihf.length();
	    String fn=ihf.getName();
	
	    sendMsgToProgDlg(false,"",title_header+" :"+fn);
	    
	    while(( n = in.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnable()) {
				in.close();
			    out.close();
	    		return false;
	    	} 
	        out.write( fileIoArea, 0, n );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(false,"",
	        			String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		in.close();
	    out.close();
	
	    long t = System.currentTimeMillis() - t0;
	    
	    if (setLastModified) ohf.setLastModified(ihf.lastModified());
	    
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+
	    		", "+tot + " bytes transfered in " + 
	    		t+" mili seconds at "+ calTransferRate(tot,t));
	
		return true;
	}

	private boolean copyFileLocalToRemote(SmbFile hf, File lf,
			String fromUrl, String toUrl,String title_header) throws IOException {
		
		long t0 = System.currentTimeMillis();
	    SmbFileOutputStream out = new SmbFileOutputStream( hf );
	    FileInputStream in = new FileInputStream(fromUrl);
	    int n=0;
	    long tot = 0;
	    long fileBytes=lf.length();
	    String fn=lf.getName();
	    
	    sendMsgToProgDlg(false,"",title_header+" :"+fn);
	    
	    while(( n = in.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnable()) {
				in.close();
			    out.close();
	    		return false;
	    	}
	        out.write( fileIoArea, 0, n );
	        tot += n;
	        if (n<fileBytes) 
	        	sendMsgToProgDlg(false,"",
	        			String.format(title_header+" %s %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		in.close();
	    out.close();
	    
	    hf = new SmbFile(toUrl,ntlmPaswordAuth);
	    if (setLastModified) hf.setLastModified(lf.lastModified());
	    
	    long t = System.currentTimeMillis() - t0;
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+
	    		tot + " bytes transfered in " + 
	    		t  + " mili seconds at " + calTransferRate(tot,t));
	
		
		return true;
	}

	private boolean copyFileRemoteToLocal(SmbFile hf, File lf, 
    		String toUrl, String fromUrl, String title_header) 
    		throws IOException {
		long t0 = System.currentTimeMillis();
	    SmbFileInputStream in = new SmbFileInputStream( hf );
	    FileOutputStream out = new FileOutputStream(toUrl);
	    int n;
		long tot = 0;
	    long fileBytes=hf.length();
	    String fn=hf.getName();
	    
	    sendMsgToProgDlg(false,"",title_header+" : "+fn);
	    
	    while(( n = in.read( fileIoArea )) > 0 ) {
	    	if (!fileioThreadCtrl.isEnable()) {
				in.close();
			    out.close();
	    		return false;
	    	}
	        out.write( fileIoArea, 0, n );
	        tot += n;
	        if (tot<fileBytes) 
	        	sendMsgToProgDlg(false,"",
	        			String.format(title_header+" %s,  %s%% completed.",fn,
	        					(tot*100)/fileBytes));
	    }
		in.close();
	    out.close();
	    
	    if (setLastModified) lf.setLastModified(hf.lastModified());
	    long t = System.currentTimeMillis() - t0;
	    
	    if (settingsMslScan) scanMediaStoreLibraryFile(toUrl);
	    
	    sendLogMsg("I",fromUrl+" was copied to "+toUrl+", "+tot +
	    		" bytes transfered in " + 
	    		t+" mili seconds at " + calTransferRate(tot,t));
	    return true;
    }
    
	private void scanMediaStoreLibraryFile(String fp) {
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

	private String isMediaFile(String fp) {
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
	
    private boolean isNoMediaPath(String path) {
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
	private int deleteMediaStoreItem(String fp) {
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
       		sendDebugLogMsg(1,"I","deleMediaStoreItem fn="+fp+
	       				", delete count image="+dc_image+
	       				", audio="+dc_audio+", video="+dc_video+", files="+dc_files);
		} else {
       		sendDebugLogMsg(1,"I","deleMediaStoreItem not MediaStore library. fn="+
	       				fp+"");
		}
		
		return dc_image+dc_audio+dc_video+dc_files;
	};
    
    
    private boolean isFileDifferent(long f1_lm, long f1_fl,long f2_lm, long f2_fl) {
    	boolean result=false;
    	if (f1_fl==f2_fl) {
    		long td=Math.abs(f1_lm-f2_lm)/1000;
    		if (td>=3) result=true;
    	} else result=true;
    	return result;
    };
    
	private boolean makeRemoteDirs(String targetPath) 
					throws MalformedURLException, SmbException {
		boolean result=false;
		String target_dir="";
		if (targetPath.lastIndexOf("/")<=0) return false;
		else {
			target_dir=targetPath.substring(0,targetPath.lastIndexOf("/"));
			target_dir=target_dir.substring(0,target_dir.lastIndexOf("/"));
		}

		SmbFile hf = new SmbFile(target_dir + "/",ntlmPaswordAuth);
		if (!hf.exists()) {
			hf.mkdirs();
		}
		return result;
	};
	
	private boolean makeLocalDirs(String targetPath) {
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

	private String calTransferRate(long tb, long tt) {
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

	private NtlmPasswordAuthentication ntlmPaswordAuth;
	private void setJcifsOption() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(currContext);
		String cp=
				prefs.getString(currContext.getString(R.string.settings_smb_perform_class), "");
		
		if (cp.equals("0") || cp.equals("")) {
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
		SMB_BUFF_SIZE=Integer.parseInt(jcifs_option_iobuff)*65536;			
	};
	
    private void setJcifsParm() {
    	ntlmPaswordAuth = new NtlmPasswordAuthentication( null,file_userid,file_password);
		System.setProperty("jcifs.util.loglevel", jcifs_option_log_level);
		System.setProperty("jcifs.smb.lmCompatibility", "0");
		System.setProperty("jcifs.smb.client.useExtendedSecurity", "false");

		jcifs.Config.setProperty("jcifs.smb.client.tcpNoDelay",jcifs_option_tcp_nodelay);
        
		if (!jcifs_option_rcv_buf_size.equals(""))
			jcifs.Config.setProperty("jcifs.smb.client.rcv_buf_size", jcifs_option_rcv_buf_size);//60416 120832
		if (!jcifs_option_snd_buf_size.equals(""))
			jcifs.Config.setProperty("jcifs.smb.client.snd_buf_size", jcifs_option_snd_buf_size);//16644 120832
        
		if (!jcifs_option_listSize.equals(""))
			jcifs.Config.setProperty("jcifs.smb.client.listSize",jcifs_option_listSize); //65536 1300
		if (!jcifs_option_maxBuffers.equals(""))
			jcifs.Config.setProperty("jcifs.smb.maxBuffers",jcifs_option_maxBuffers);//16 100
		jcifs.Config.registerSmbURLHandler();
	};


}
