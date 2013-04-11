package com.sentaroh.android.SMBExplorer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sentaroh.android.Utilities.TreeFilelistItem;

public class SMBExplorerTaskDataHolder implements Serializable  {

	private static final long serialVersionUID = 1L;

	List<MsglistItem> msglist;
	
	List<TreeFilelistItem> local_tfl;

	List<TreeFilelistItem> remote_tfl;
	
	ArrayList<TreeFilelistItem> paste_list=null;
	String paste_from_url=null, paste_to_url=null, paste_item_list=null;
	boolean is_paste_copy=false,is_paste_enabled=false, is_paste_from_local=false;
	
};
