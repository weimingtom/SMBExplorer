package com.sentaroh.android.SMBExplorer;

import java.io.Serializable;

public class MsglistItem implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String msgCat, msgBody, msgDate, msgTime,msgIssuer;
	
	public MsglistItem(String cat, String mdate, String mtime, 
			String issuer, String msg)
	{
		msgCat=cat;
		msgBody= msg;
		msgDate=mdate;
		msgTime=mtime;
		msgIssuer=issuer;
	}
	public String getCat()
	{
		return msgCat;
	}
	public String getMdate()
	{
		return msgDate;
	}
	public String getMtime()
	{
		return msgTime;
	}
	public String getIssuer()
	{
		return msgIssuer;
	}
	public String getMsg()
	{
		return msgBody;
	}
	public String toString() {
		return msgCat+" "+msgDate+" "+msgTime+" "+
				(msgIssuer+"          ").substring(0,9)+msgBody;
	}
}
