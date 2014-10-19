package com.sentaroh.android.SMBExplorer;

import java.io.Serializable;

import android.util.Log;

public class FileListItem implements Serializable, Comparable<FileListItem>{
	private static final long serialVersionUID = 1L;
	
	private String fileName;
	private String fileCap;
	private boolean isDir=false;
	private long fileLength;
	private long lastModdate;
	private boolean isChecked=false;
	private boolean canRead=false;
	private boolean isHidden=false;
	private boolean canWrite=false;
	private String filePath;
	private boolean childListExpanded=false;
	private int listLevel=0;
	private boolean hideListItem=false;
	private boolean subDirLoaded=false;
	private int subDirItemCount=0;
	private boolean triState=false;
	private boolean enableItem=true;
	private boolean hasExtendedAttr=false;
	
	public void dump(String id) {
		String did=(id+"            ").substring(0,12);
		Log.v("FileListItem",did+"FileName="+fileName+", Caption="+fileCap+", filePath="+filePath);
		Log.v("FileListItem",did+"isDir="+isDir+", Length="+fileLength+
				", lastModdate="+lastModdate+", isChecked="+isChecked+
				", canRead="+canRead+",canWrite="+canWrite+", isHidden="+isHidden+", hasExtendedAttr="+hasExtendedAttr);
		Log.v("FileListItem",did+"childListExpanded="+childListExpanded+
				", listLevel=="+listLevel+", hideListItem="+hideListItem+
				", subDirLoaded="+subDirLoaded+", subDirItemCount="+subDirItemCount+
				", triState="+triState+", enableItem="+enableItem);
	};
	
	public FileListItem(String fn){
		fileName = fn;
	}
	
	public FileListItem(String fn,String cp,
		boolean d, long fl,long lm, boolean ic, 
		boolean cr,boolean cw,boolean hd, String fp, int lvl){
		fileName = fn;
		fileLength = fl;
		fileCap = cp;
		isDir=d;
		lastModdate=lm;
		isChecked =ic;
		canRead=cr;
		canWrite=cw;
		isHidden=hd;
		filePath=fp;
		listLevel=lvl;
	}
	public String getName(){return fileName;}
	public long getLength(){return fileLength;}
	public String getCap(){return fileCap;}
	public boolean isDir(){return isDir;}
	public long getLastModified(){return lastModdate;}
	public void setLastModified(long p){lastModdate=p;}
	public boolean isChecked(){return isChecked;}
	public void setChecked(boolean p){
		isChecked=p;
		if (p) triState=false;
	};
	public boolean canRead(){return canRead;}
	public boolean canWrite(){return canWrite;}
	public boolean isHidden(){return isHidden;}
	public String getPath(){return filePath;}
	public void setChildListExpanded(boolean p){childListExpanded=p;}
	public boolean isChildListExpanded(){return childListExpanded;}
	public void setListLevel(int p){listLevel=p;}
	public int getListLevel(){return listLevel;}
	public boolean isHideListItem(){return hideListItem;}
	public void setHideListItem(boolean p){hideListItem=p;}
	public void setSubDirItemCount(int p){subDirItemCount=p;}
	public int getSubDirItemCount(){return subDirItemCount;}
	public boolean isSubDirLoaded() {return subDirLoaded;}
	public void setSubDirLoaded(boolean p) {subDirLoaded=p;}
	public void setTriState(boolean p) {triState=p;}
	public boolean isTriState() {return triState;}
	public void setEnableItem(boolean p) {enableItem=p;}
	public boolean isEnableItem() {return enableItem;}
	public void setHasExtendedAttr(boolean p) {hasExtendedAttr=p;};
	public boolean hasExtendedAttr() {return hasExtendedAttr;}
	
	@Override
	public int compareTo(FileListItem o) {
	if(this.fileName != null) {
		String cmp_c="F",cmp_n="F";
		if (isDir) cmp_c="D";
		if (o.isDir()) cmp_n="D";
		cmp_c+=filePath;
		cmp_n+=o.getPath();
		if (!cmp_c.equals(cmp_n)) return cmp_c.compareToIgnoreCase(cmp_n);
		return fileName.compareToIgnoreCase(o.getName());
	} else 
		throw new IllegalArgumentException();
	}
}

