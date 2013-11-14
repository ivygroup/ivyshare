package com.ivyshare.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FtpUpload {

    private static final String TAG = "UserTraceManager";

//    private String mFtpUploadUrl = "199.33.126.92";
//    private String mPortNumber = "21";
//    private String mUserName = "eyzkzvyl";
//    private String mPassword = "2i8Ih0yH0d";
//    private String mRemoteDir = "/domains/amapig.com/public_html/ivy/Trace";
    private String mFtpUploadUrl = "70.39.99.10";
    private String mPortNumber = "21";
    private String mUserName = "17014";
    private String mPassword = "ivyshare1234";
    private String mRemoteDir = "/Trace";
    private String mFileSource;
    private String mRemoteName;

    private Handler mHandler;
    public static final int UPLOAD_SUCESS = 0;
    public static final int UPLOAD_FAILED = 1;

    public interface FtpUploadListener {
    	public void onUploadResult(int ret);
    }
    private FtpUploadListener mFtpUploadListener;

    public void setListener(FtpUploadListener listener) {
    	mFtpUploadListener = listener;
    }
 
    public FtpUpload() {
    	mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				mFtpUploadListener.onUploadResult(msg.what);
			}
    	};
    }

    public void uploadFile(String source, String remoteName) {
    	mFileSource = source;
    	mRemoteName = remoteName;
    	//Log.d(TAG, "Source " + mFileSource + " Remote " + mRemoteName);

        new Thread(){
            public void run(){
                FTPClient ftpClient = new FTPClient(); 
                FileInputStream fis = null;

                try {
                    ftpClient.connect(mFtpUploadUrl, Integer.parseInt(mPortNumber));
                    boolean loginResult = ftpClient.login(mUserName, mPassword);
                    int returnCode = ftpClient.getReplyCode();

                    if (loginResult && FTPReply.isPositiveCompletion(returnCode)) {
                        //ftpClient.makeDirectory(mRemoteDir); 
                        ftpClient.changeWorkingDirectory(mRemoteDir); 

                        ftpClient.setBufferSize(1024); 
                        ftpClient.setControlEncoding("UTF-8");
                        ftpClient.enterLocalPassiveMode();

                        File file = new File(mFileSource);
                        fis = new FileInputStream(file);
                        boolean ret = ftpClient.storeFile(mRemoteName, fis);
                        if (ret) {
                        	// success here
                        	mHandler.sendEmptyMessage(UPLOAD_SUCESS);
                        } else {
                        	Log.d(TAG, "storeFile failed");
                        	mHandler.sendEmptyMessage(UPLOAD_FAILED);
                        }
                    } else {
                    	Log.d(TAG, "login failed");
                    	mHandler.sendEmptyMessage(UPLOAD_FAILED);
                    }
                } catch (FileNotFoundException fe) {
                	Log.d(TAG, "FileNotFoundException");
                	mHandler.sendEmptyMessage(UPLOAD_FAILED);
                	fe.printStackTrace();
                } catch (IOException ioe) {
                	Log.d(TAG, "IOException");
                	mHandler.sendEmptyMessage(UPLOAD_FAILED);
                	ioe.printStackTrace();
                } finally {
                	try {
                    	if (fis != null) {
                    		fis.close();
                    		fis = null;
                    	}
                	} catch (IOException e) {
                	}

                	try {
	                	if (ftpClient != null) {
	                		ftpClient.disconnect();
	                		ftpClient = null;
	                	}
                	} catch (IOException e) {
                	}
                }
            }
        }.start();
    }
}