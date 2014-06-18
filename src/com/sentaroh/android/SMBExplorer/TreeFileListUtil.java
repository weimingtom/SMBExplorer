package com.sentaroh.android.SMBExplorer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import android.annotation.SuppressLint;
import android.util.Log;

import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;

public class TreeFileListUtil {

    static public ArrayList<String> createRefDirList(ArrayList<TreeFilelistItem> file_list) {
    	ArrayList<String>w_ref_list=new ArrayList<String>();
    	String c_path="";
    	for (int i=0;i<file_list.size();i++) {
    		TreeFilelistItem tfli=file_list.get(i);
    		if (!tfli.getPath().equals(c_path)) {
    			w_ref_list.add(tfli.getPath());
    			c_path=tfli.getPath();
    		}
    	}
    	
    	Collections.sort(w_ref_list);
    	ArrayList<String>ref_list=new ArrayList<String>();
    	c_path="";
    	for (int i=0;i<w_ref_list.size();i++) {
    		if (!w_ref_list.get(i).equals(c_path)) {
    			c_path=w_ref_list.get(i);
    			ref_list.add(c_path);
    		}
    	}
    	
    	return ref_list;
    };

    static public void updateTreeFileListArray(
			String url,
			ArrayList<TreeFilelistItem> ref_list,
			ArrayList<TreeFilelistItem> file_list) {
		Log.v("","url="+url+", ref_list="+ref_list.size()+", flsize="+file_list.size());
		for (int i=0;i<ref_list.size();i++) {
			TreeFilelistItem si=ref_list.get(i);
			int pos=findTreeListItem(si, file_list);
			if (pos!=-1) {
				//Update
				updateTreeFileListItem(si,file_list,pos);
			} else {
				//Add
				insertTreeFileListItem(si, ref_list.size(), file_list);
			}
		}
		//Delete if ref_list not exists
		ArrayList<TreeFilelistItem> del_list=new ArrayList<TreeFilelistItem>();
		for(int i=0;i<file_list.size();i++) {
			TreeFilelistItem rl=file_list.get(i);
			if (rl.getPath().equals(url)) {
//				rl.dump();
				if (ref_list.size()>0) {
					int pos=findTreeListItem(rl, ref_list);
					if (pos==-1) {
						del_list.add(rl);
					}
				} else {
					del_list.add(rl);
				}
			}
		}
		for(int i=0;i<del_list.size();i++) file_list.remove(del_list.get(i));
		
		//Update dir count
		for (int i=file_list.size()-1;i>=0;i--) {
			TreeFilelistItem tfli=file_list.get(i);
			String t_key=tfli.getPath()+"/"+tfli.getName();
			if (url.equals(t_key)) {
				tfli.setSubDirItemCount(ref_list.size());
				if (file_list.size()==0) {
					tfli.setChildListExpanded(false);
					tfli.setSubDirLoaded(false);
					tfli.setHideListItem(false);
				}
//				tfli.dump();
				break;
			}
		}
	};
	
	static public void updateTreeFileListItem(TreeFilelistItem ufli, 
			ArrayList<TreeFilelistItem> file_list, int pos) {
		TreeFilelistItem tfli=file_list.get(pos);
		tfli.setLastModified(ufli.getLastModified());
		tfli.setSubDirItemCount(ufli.getSubDirItemCount());
	};

	static public void insertTreeFileListItem(TreeFilelistItem ufli, int sub_dir_cnt, 
			ArrayList<TreeFilelistItem> file_list) {
		String u_key=ufli.getPath();//+"/"+ufli.getName();
//		ufli.dump("0");
		for (int i=file_list.size()-1;i>=0;i--) {
			TreeFilelistItem tfli=file_list.get(i);
			String t_key=tfli.getPath();//+"/"+tfli.getName();
//			Log.v("","u_key="+u_key+", t_key="+t_key);
			if (u_key.equals(t_key)) {
				tfli.setSubDirItemCount(sub_dir_cnt);
				tfli.setChildListExpanded(false);
				
				String u_path=ufli.getPath()+"/"+ufli.getName();
				for(int j=i;j>=0;j--) {
					String t_path=file_list.get(j).getPath()+"/"+file_list.get(j).getName();
//					Log.v("","u_path="+u_path+", t_path="+t_path+", dir="+file_list.get(j).isDir());
					if (ufli.isDir()) {
						if (file_list.get(j).isDir() &&
								u_path.compareToIgnoreCase(t_path)>0) {
//							file_list.get(j).dump("1");
							TreeFilelistItem nfli=createNewTreeFilelistItem(ufli);
							nfli.setListLevel(tfli.getListLevel());//.file_list.get(j).getListLevel()+1);
							file_list.add(j+1, nfli);
							break;
						}
					} else {
						if (!file_list.get(j).isDir()) {
							if (u_path.compareToIgnoreCase(t_path)>0) {
//								file_list.get(j).dump("2");
								TreeFilelistItem nfli=createNewTreeFilelistItem(ufli);
								nfli.setListLevel(file_list.get(j).getListLevel());
								nfli.setChildListExpanded(file_list.get(j).isChildListExpanded());
								file_list.add(j, nfli);
							break;
							}
						} else {
//							file_list.get(j-1).dump("3");
							TreeFilelistItem nfli=createNewTreeFilelistItem(ufli);
							nfli.setListLevel(file_list.get(j).getListLevel());
							nfli.setChildListExpanded(file_list.get(j).isChildListExpanded());
							nfli.setHideListItem(file_list.get(j).isHideListItem());
							file_list.add(j+1, nfli);
//							nfli.dump();
							break;
						}
					}
				}
				break;
			}
		}
	};
	
	@SuppressLint("SimpleDateFormat")
	static public TreeFilelistItem createNewTreeFilelistItem(TreeFilelistItem tfli) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		TreeFilelistItem fi=null;
		if (tfli.canRead()) {
			if (tfli.isDir()) {
				fi= new TreeFilelistItem(tfli.getName(), 
						sdf.format(tfli.getLastModified())+", ",
						true, 
						0,
						0,
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),tfli.getPath(),
						tfli.getListLevel());
				fi.setSubDirItemCount(tfli.getSubDirItemCount());
			} else {
			    String tfs = MiscUtil.convertFileSize(tfli.getLength());

				fi=new TreeFilelistItem(tfli.getName(), 
					sdf.format(tfli.getLastModified())+","+tfs, 
					false, 
					tfli.getLength(),
					tfli.getLastModified(),
					false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getPath(),
					tfli.getListLevel());
			}
		} else {
			fi=new TreeFilelistItem(tfli.getName(), 
					sdf.format(tfli.getLastModified())+","+0, false,
					tfli.getLength(),tfli.getLastModified(),false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getPath(),
					tfli.getListLevel());
			fi.setEnableItem(false);
		}
		return fi;
	};
	
	@SuppressLint("SimpleDateFormat")
	static public TreeFilelistItem createNewTreeFilelistItem(File tfli, int sdc, int ll) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		TreeFilelistItem fi=null;
		if (tfli.canRead()) {
			if (tfli.isDirectory()) {
				fi= new TreeFilelistItem(tfli.getName(), 
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

				fi=new TreeFilelistItem(tfli.getName(), 
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
			fi=new TreeFilelistItem(tfli.getName(), 
					sdf.format(tfli.lastModified())+","+0, false,
					tfli.length(),tfli.lastModified(),false,
					tfli.canRead(),tfli.canWrite(),
					tfli.isHidden(),tfli.getParent(),
					ll);
			fi.setEnableItem(false);
		}
		return fi;
	};

	@SuppressLint("SimpleDateFormat")
	static public TreeFilelistItem createNewTreeFilelistItem(SmbFile tfli, int sdc, int ll) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		TreeFilelistItem fi=null;
		try {
			String fp=tfli.getParent();
			if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
			if (tfli.canRead()) {
				if (tfli.isDirectory()) {
					fi= new TreeFilelistItem(tfli.getName(), 
							sdf.format(tfli.lastModified())+", ",
							true, 
							0,
							0,
							false,
							tfli.canRead(),tfli.canWrite(),
							tfli.isHidden(),fp,
							ll);
					fi.setSubDirItemCount(sdc);
				} else {
				    String tfs = MiscUtil.convertFileSize(tfli.length());

					fi=new TreeFilelistItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+","+tfs, 
						false, 
						tfli.length(),
						tfli.lastModified(),
						false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),fp,
						ll);
				}
			} else {
				fi=new TreeFilelistItem(tfli.getName(), 
						sdf.format(tfli.lastModified())+","+0, false,
						tfli.length(),tfli.lastModified(),false,
						tfli.canRead(),tfli.canWrite(),
						tfli.isHidden(),fp,
						ll);
				fi.setEnableItem(false);
			}
		} catch(SmbException e) {
			
		}
		return fi;
	};

	
	static public int findTreeListItem(TreeFilelistItem s, ArrayList<TreeFilelistItem> tl) {
		int result=-1;
		for(int i=0;i<tl.size();i++) {
			if (s.getPath().equals(tl.get(i).getPath()) &&
					s.getName().equals(tl.get(i).getName())) {
				result=i;
				break;
			}
		}
//		Log.v("","find path="+s.getPath()+", name="+s.getName()+", result="+result);
		return result;
	};

}
