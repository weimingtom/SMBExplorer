package com.sentaroh.android.SMBExplorer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import jcifs.UniAddress;
import jcifs.netbios.NbtAddress;

public class NetworkUtil {
	
	final static public boolean isValidIpAddress(String in_addr) {
		boolean result=false;
		String strip_addr=in_addr;
		if (in_addr.indexOf(":")>=0) strip_addr=in_addr.substring(0,in_addr.indexOf(":")) ;
		String[] addr=strip_addr.split("\\.");
		if (addr.length==4) {
			boolean error=false;
			for (int i=0;i<4;i++) {
				try {
					int num=Integer.parseInt(addr[i]);
					if (num<0 || num>255) {
						error=true;
						break;
					}
				} catch(NumberFormatException e) {
					error=true;
					break;
				}
			}
			if (!error) result=true;
		}
		return result;
	}

	final static public String resolveSmbHostName(String hn) {
		String ipAddress=null;
		try {
			NbtAddress nbtAddress = NbtAddress.getByName(hn);
			InetAddress address = nbtAddress.getInetAddress();
			ipAddress= address.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ipAddress;
	}
	
	final static public boolean isIpAddrReachable(String address) {
		boolean reachable=false;
		Socket socket = new Socket();
        try {
            socket.bind(null);
//            socket.connect((new InetSocketAddress(address, 139)), timeout);
            socket.connect((new InetSocketAddress(address, 445)), 300);
            reachable=true;
            socket.close();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
		}
		return reachable;
	};

	final static public String getSmbHostName(String address) {
		String srv_name="";
    	try {
			UniAddress ua = UniAddress.getByName(address);
			String cn;
	        cn = ua.firstCalledName();
	        do {
	            if (!cn.startsWith("*")) srv_name=cn; 
//            	util.addDebugLogMsg(1,"I","getSmbHostName Address="+address+
//	            		", cn="+cn+", name="+srv_name);
	        } while(( cn = ua.nextCalledName() ) != null );
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	return srv_name;
 	};

 	final static public boolean isNbtAddressActive(String address) {
		boolean result=false;
		try {
			NbtAddress na=NbtAddress.getByName(address);
			result=na.isActive();
		} catch (UnknownHostException e) {
		}
		return result;
	};

}
