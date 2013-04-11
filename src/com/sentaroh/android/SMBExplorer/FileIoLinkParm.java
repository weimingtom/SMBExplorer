package com.sentaroh.android.SMBExplorer;

public class FileIoLinkParm {
	private String targetUrl1="";
	private String targetUrl2="";
	private String targetName="";
	private String targetNew="";
	private String smbUser="";
	private String smbPass="";
	private String smbDomain="";
	private String smbWorkGroup="";
	private boolean allcopy=true;
	
	FileIoLinkParm () {}
	
	FileIoLinkParm (String tu1, String tu2,  
			String tname, String tnew, String tu, String tp) {
		targetUrl1=tu1;
		targetUrl2=tu2;
		targetName=tname;
		targetNew=tnew;
		smbUser=tu;
		smbPass=tp;
	}

	public String getUrl1() {return targetUrl1;}
	public String getUrl2() {return targetUrl2;}
	public String getName() {return targetName;}
	public String getNew() {return targetNew;}
	public String getUser() {return smbUser;}
	public String getPass() {return smbPass;}
	public String getDomain() {return smbDomain;}
	public String getWorkGroup() {return smbWorkGroup;}
	public boolean isAllCopyEnabled() {return allcopy;}

	public void setUrl1(String p) {targetUrl1=p;}
	public void setUrl2(String p) {targetUrl2=p;}
	public void setName(String p) {targetName=p;}
	public void setNew(String p) {targetNew=p;}
	public void setUser(String p) {smbUser=p;}
	public void setPass(String p) {smbPass=p;}
	public void setDomain(String p) {smbDomain=p;}
	public void setWorkGroup(String p) {smbWorkGroup=p;}
	public void setAllCopy(boolean p) {allcopy=p;}
	
	
}
