package com.sentaroh.android.SMBExplorer;

public class Constants {
	final public static int FILEIO_PARM_LOCAL_CREATE = 1;
	final public static int FILEIO_PARM_LOCAL_RENAME = 2;
	final public static int FILEIO_PARM_LOCAL_DELETE = 3;
	final public static int FILEIO_PARM_REMOTE_CREATE = 4;
	final public static int FILEIO_PARM_REMOTE_RENAME = 5;
	final public static int FILEIO_PARM_REMOTE_DELETE = 6;
	final public static int FILEIO_PARM_COPY_REMOTE_TO_LOCAL = 7;
	final public static int FILEIO_PARM_COPY_REMOTE_TO_REMOTE = 8;
	final public static int FILEIO_PARM_COPY_LOCAL_TO_LOCAL = 9;
	final public static int FILEIO_PARM_COPY_LOCAL_TO_REMOTE = 10;
	final public static int FILEIO_PARM_MOVE_REMOTE_TO_LOCAL = 11;
	final public static int FILEIO_PARM_MOVE_REMOTE_TO_REMOTE = 12;
	final public static int FILEIO_PARM_MOVE_LOCAL_TO_LOCAL = 13;
	final public static int FILEIO_PARM_MOVE_LOCAL_TO_REMOTE = 14;
	final public static int FILEIO_PARM_DOWLOAD_RMOTE_FILE = 15;
	final public static String SMBEXPLORER_PROFILE_NAME = "profile.txt";
	
	final public static int MAX_DLG_BOX_SIZE_WIDTH=600; 
	final public static int MAX_DLG_BOX_SIZE_HEIGHT=0;
	
	final public static String SMBEXPLORER_TAB_LOCAL="Local";
	final public static String SMBEXPLORER_TAB_REMOTE="Remote";
}
