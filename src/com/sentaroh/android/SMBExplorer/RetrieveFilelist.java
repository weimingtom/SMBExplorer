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

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.sentaroh.android.Utilities.*;

public class RetrieveFilelist implements Runnable  {
	private final static String DEBUG_TAG = "SMBExplorerGetFilelist";
	
	private int debugLevel = 0;

	private Dialog threadDlg;
	private TextView dlgMsg;
	
	private ListView msgListView;
	private MsglistAdapter msglistAdapter;

	private ThreadCtrl getFLCtrl=null;
	
	private ArrayList<TreeFilelistItem> remoteFileList;
	private String remoteUrl;//, currDir;
	private List<String> dir_list;
	
	private String opCode="FL";
	
	private NotifyEvent notifyEvent ;
	
	private Calendar calInstance;
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	@SuppressWarnings("unused")
	private String jcifs_option_rcv_buf_size="",jcifs_option_snd_buf_size="",
			jcifs_option_listSize="",jcifs_option_maxBuffers="",jcifs_option_iobuff="",
					jcifs_option_tcp_nodelay="", jcifs_option_log_level="";
	
	private Context currContext;

	public RetrieveFilelist(Context c, MsglistAdapter ma, ListView ml, Dialog pd,
			ThreadCtrl ac, int dl, String ru, List<String> d_list,
			String user, String pass, NotifyEvent ne) {
		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		threadDlg=pd;
		debugLevel=dl;
		
		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		dir_list=d_list;
		
		opCode="EC"; //check item is exists
		
		dlgMsg = (TextView) threadDlg.findViewById(R.id.progress_spin_dlg_msg);
		
		sendDebugLogMsg(1,"I","getFileList constructed. user="+user+", url="+ru);
		sendDebugLogMsg(9,"I","getFileList constructed. pass="+pass);
		
		setJcifsOption();
		setJcifsProperties(user,pass);
	}

	
	public RetrieveFilelist(Context c, MsglistAdapter ma, ListView ml, Dialog pd,
			ThreadCtrl ac, int dl, String ru, 
			ArrayList<TreeFilelistItem> fl,String user, String pass, NotifyEvent ne) {
		currContext=c;
		msglistAdapter=ma;
		msgListView=ml;
		threadDlg=pd;
		debugLevel=dl;
		remoteFileList=fl;
		
		getFLCtrl=ac; //new SMBExplorerThreadCtrl();
		notifyEvent=ne;
		remoteUrl=ru;
		
		dlgMsg = (TextView) threadDlg.findViewById(R.id.progress_spin_dlg_msg);
		
		sendDebugLogMsg(1,"I","getFileList constructed. user="+user+", url="+ru);
		sendDebugLogMsg(9,"I","getFileList constructed. pass="+pass);
		
		setJcifsOption();
		setJcifsProperties(user,pass);
	}
	
	
	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
		
		getFLCtrl.setThreadResultSuccess();
		
		sendDebugLogMsg(1,"I","getFileList started");
		
		if (opCode.equals("FL")) {
			remoteFileList.clear();
			readFileList(remoteUrl);
		} else if (opCode.equals("EC")) {
			checkItemExists(remoteUrl);
		}
		sendDebugLogMsg(1,"I","getFileList terminated.");
		sendEmptyLogMsg();
		getFLCtrl.setDisable();
		
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
		ntlmPaswordAuth = new NtlmPasswordAuthentication( null,user,pass);
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
						"Parent="+fp);
				} else {
					getFLCtrl.setThreadResultCancelled();
					sendDebugLogMsg(-1,"W","Cancelled by main task.");
					break;
				}
			}
		} catch (SmbException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			sendDebugLogMsg(0,"E",e.toString());
			getFLCtrl.setThreadResultError();
			getFLCtrl.setDisable();
			getFLCtrl.setThreadMessage(e.toString());
		} catch (MalformedURLException e) {
			// TODO 自動生成された catch ブロック
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
	
	
	private void sendDebugLogMsg(int lvl, String cat, String msg) {

		if (debugLevel>0)  
			Log.v(DEBUG_TAG,msg);
		if (debugLevel>=lvl) {
			Message msgInstance=new Message();
			msgInstance.arg1=0; // arg1=0 not close, arg1=1 close
			msgInstance.arg2=2;	// arg2=1 both, arg2=0 dialog only, arg2=2 log only
			msgInstance.obj=new String[]{cat,"DEBUG-I",msg};
			handler.sendMessage(msgInstance);
		}
	};
	
	private void sendEmptyLogMsg() {

		if (debugLevel>0)  
			Log.v(DEBUG_TAG,"Send empty log msg");
		Message msgInstance=new Message();
		msgInstance.arg1=1;
		msgInstance.obj=null;
		handler.sendMessage(msgInstance);
	};
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// sendMessage()で渡されたobjectを取得してUIに表示
			if (msg.arg1==0) { // arg1=0 not close, arg1=1 close
				String[] sm = (String[])msg.obj;
				if (msg.arg2==0) {// arg2=1 both, arg2=0 dialog only, arg2=2 log only
					dlgMsg.setText(sm[2]);
				} else { //
					if (msg.arg2==1) dlgMsg.setText(sm[2]);
					calInstance = Calendar.getInstance();
					msglistAdapter.add(
								new MsglistItem(sm[0],
										sdfDate.format(calInstance.getTime()),
										sdfTime.format(calInstance.getTime()),
										sm[1],sm[2]));}
					msgListView.setSelection(msgListView.getCount());
			} else { // last message received, cancel progress dialog
				threadDlg.dismiss();
				notifyEvent.notifyTolistener(true, null);
			}
		}
	};
}