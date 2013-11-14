package com.ivyshare.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.util.Log;

import com.ivyshare.engin.control.LocalSetting;

public class UserTrace {
	private static final String TAG = UserTrace.class.getSimpleName();

	// room type
//	public static final int PUBLIC_ROOM = 0;
//	public static final int MYSELF_ROOM = 1;
//	public static final int OTHERS_ROOM	= 2;

	// chat type
	public static final int ONE2ONE_CHAT 	= 0;
	public static final int BROADCAST_CHAT 	= 1;

	// share from
	public static final int MAINPAGE_SHARE 	= 0;
	public static final int OTHERAPP_SHARE 	= 1;

	// action type
	public static final int ACTION_CREATE_ROOM		= 0;
	public static final int ACTION_DESTROY_ROOM		= 1;
	public static final int ACTION_ENTER_ROOM		= 2;
	public static final int ACTION_EXIT_ROOM		= 3;
	public static final int ACTION_ENTER_ONE2ONE	= 4;
	public static final int ACTION_EXIT_ONE2ONE		= 5;
	public static final int ACTION_ENTER_BROADCAST	= 6;
	public static final int ACTION_EXIT_BROADCAST	= 7;
	public static final int ACTION_ENTER_SHARE		= 8;
	public static final int ACTION_EXIT_SHARE		= 9;
	public static final int ACTION_ENTER_APP		= 10;
	public static final int ACTION_EXIT_APP			= 11;

	public static final int ACTION_DELETE_CHAT		= 21;
	public static final int ACTION_DELETE_ALL		= 22;
	public static final int ACTION_DELETE_FREESHARE	= 23;

	public static final int ACTION_SELF_SETTING		= 30;
	public static final int ACTION_WIFI_SETTING		= 31;
	public static final int ACTION_CHECK_UPDATE		= 32;
	public static final int ACTION_ADD_FEEDBACK		= 33;
	public static final int ACTION_VIEW_ABOUT		= 34;
	public static final int ACTION_MODIFY_NOTI		= 35;

	public static final int ACTION_SENDMESSAGE		= 100;
	public static final int ACTION_DELETE_ONE		= 101;

	public static final int ACTION_FREESHARE		= 110;	// create a url share
	public static final int ACTION_SHARE_MULTISEND	= 111;	// send from share UI

	private static boolean mShareTrace = false;

	public static void setShareTrace(boolean value) {
		mShareTrace = value;
	}

	public static void addTrace(int actionType) {
		if (!mShareTrace) {
			return;
		}
		UserTrace.getInstance().addToFile(actionType);
	}

	public static void addSendTrace(int actionType, int fileType, int chatType) {
		if (!mShareTrace) {
			return;
		}
		UserTrace.getInstance().addToFile(actionType, fileType, chatType);
	}

	public static void addShareTrace(int actionType, int fileType, int from) {
		if (!mShareTrace) {
			return;
		}
		UserTrace.getInstance().addToFile(actionType, fileType, from);
	}

    private static UserTrace mInstance = null;
    private FileOutputStream mFileOutputStream;
    private DataOutputStream mDataOutputStream;
	private static final String mTraceFilePath = LocalSetting.getInstance().getLocalPath() + "Log/";

    public static UserTrace getInstance() {
        if (mInstance == null) {
        	mInstance = new UserTrace();
        	mInstance.init();
        }
        return mInstance;
    }

    public static void clearAllLogs() {
    	if (mInstance == null) {
    		return;
    	}

    	mInstance.clearLogs();
    	mInstance.init();
    }

    private void init() {
    	File filePath = new File(mTraceFilePath);
    	if (!filePath.exists()) {
    		filePath.mkdir();
    	}

    	String traceFile = mTraceFilePath + "trace";
    	File file = new File(traceFile);
    	if (file.exists() && file.length() > 1024*1024*5) {
    		int count=1;
    		File temp = new File(traceFile + "." + count);
    		while(temp.exists()) {
    			count++;
    			temp = new File(traceFile + "." + count);
    		}
    		file.renameTo(temp);
    	}

    	try {
        	mFileOutputStream = new FileOutputStream(traceFile, true);
        	mDataOutputStream = new DataOutputStream(mFileOutputStream);
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	}
    }

    private void clearLogs() {
    	File filePath = new File(mTraceFilePath);
    	if (!filePath.exists()) {
    		return;
    	}

    	try {
    		if (mFileOutputStream != null) {
    			mFileOutputStream.close();
    			mFileOutputStream = null;
    		}
    		if (mDataOutputStream != null) {
    			mDataOutputStream.close();
    			mFileOutputStream = null;
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}

    	for (File file:filePath.listFiles()) {
    		file.delete();
    	}
    }

    public static String getTraceFilePath() {
    	return mTraceFilePath;
    }

    public static void readContent(String path) {
    	try {
        	FileInputStream fis = new FileInputStream(path);
        	DataInputStream dis = new DataInputStream(fis);

        	try {
            	int read;
            	while((read = dis.read()) != -1) {
            		if (read >= 100) {
            			int arg0 = dis.readByte();
            			int arg1 = dis.readByte();
            			long time = dis.readLong();
            			Log.d(TAG, "Action: " + read + " arg0 " + arg0 + " arg1 " + arg1 + " time " + new Date(time).toString());
            		} else {
            			long time = dis.readLong();
            			Log.d(TAG, "Action: " + read + " time " + new Date(time).toString());
            		}
            	}
        	} catch (IOException e) {
        		Log.e(TAG, "read error");
        		e.printStackTrace();
        	}
    	} catch (FileNotFoundException e) {
    		Log.e(TAG, "open error");
    		e.printStackTrace();
    	}
    }

    private void addToFile(int actionType, int arg0, int arg1) {
    	if (mFileOutputStream == null) {
    		return;
    	}
    	try {
    		mDataOutputStream.writeByte(actionType);
    		mDataOutputStream.writeByte(arg0);
    		mDataOutputStream.writeByte(arg1);
    		mDataOutputStream.writeLong(System.currentTimeMillis());
    		mFileOutputStream.flush();
    	} catch (IOException e) {
    		
    	}
    }
    public void addToFile(int actionType) {
    	if (mFileOutputStream == null) {
    		return;
    	}
    	try {
    		mDataOutputStream.writeByte(actionType);
    		mDataOutputStream.writeLong(System.currentTimeMillis());
    		mFileOutputStream.flush();
    	} catch (IOException e) {
    		
    	}
    }
}