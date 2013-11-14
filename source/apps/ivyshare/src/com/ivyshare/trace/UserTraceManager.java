package com.ivyshare.trace;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.ivyshare.R;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.util.CommonUtils;

public class UserTraceManager {
    private static final String TAG = UserTraceManager.class.getSimpleName();

    private String mZipPathName;
    private String mRemoteName;
    private FtpUpload mFtpUpload;
    private LocalSetting mLocalSetting;

    private boolean mInitialed = false;
    private boolean mUploadSucess = false;
    private boolean mFirtTimeLaunch = false;

    private Handler mHandler;
    private Context mContext;

	private static UserTraceManager instance = null;
    public static UserTraceManager getInstance() {
        if (instance == null) {
            instance = new UserTraceManager();
        }
        return instance;
    }

    public void init(Context context) {
    	if (mInitialed) {
    		return;
    	}
    	mInitialed = true;

    	Log.d(TAG, "init");

    	mLocalSetting = LocalSetting.getInstance();
    	mContext = context;

    	mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
	            Dialog alertDialog = new AlertDialog.Builder(mContext).
	                setTitle(R.string.sharetrace_title).
	                setMessage(R.string.sharetrace_message).
	                setIcon(android.R.drawable.ic_dialog_alert).
	                setPositiveButton(R.string.sharetrace_yes, new DialogInterface.OnClickListener() {
	                    @Override 
	                    public void onClick(DialogInterface dialog, int which) {
	                    	mLocalSetting.saveTraceAction(true);
	                        mLocalSetting.saveFirstTime(false);
	                        UserTrace.setShareTrace(mLocalSetting.getTraceAction());
	                    }
	                }).setNegativeButton(R.string.sharetrace_no, new DialogInterface.OnClickListener() {
	                    @Override 
	                    public void onClick(DialogInterface dialog, int which) {
	                    	mLocalSetting.saveTraceAction(false);
	                        mLocalSetting.saveFirstTime(false);
	                        UserTrace.setShareTrace(mLocalSetting.getTraceAction());
	                    }
	                }).create();
	            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);  
	            alertDialog.setCanceledOnTouchOutside(false);  
	            alertDialog.show();
	            UserTrace.setShareTrace(mLocalSetting.getTraceAction());
			}
    	};

    	if (mLocalSetting.getFirstTime()) {
    		mFirtTimeLaunch = true;
        	mLocalSetting.saveTraceAction(true);
            mLocalSetting.saveFirstTime(false);
    		UserTrace.setShareTrace(mLocalSetting.getTraceAction());
    		//mHandler.sendEmptyMessage(0);
    	} else {
    		UserTrace.setShareTrace(mLocalSetting.getTraceAction());
    	}
    }

    public void unInit() {
    	mInitialed = false;
    	mUploadSucess = false;
    	mFirtTimeLaunch = false;
    }

    public void startUploadTrace() {
    	Log.d(TAG, "start trace");
    	if (mFirtTimeLaunch) {
    		Log.d(TAG, "first time, no need to trace");
    		return;
    	}

    	if (!mLocalSetting.getTraceAction()) {	// don't need to trace
    		Log.d(TAG, "user don't agree to trace");
    		return;
    	}

    	if (mUploadSucess) {					// have already uploaded once
    		Log.d(TAG, "already trace");
    		return;
    	}

    	if (!CommonUtils.isNetworkAvailable()) {// network error
    		Log.d(TAG, "net work error");
    		return;
    	}

        mZipPathName = LocalSetting.getInstance().getLocalPath() + "tracelog";

        StringBuilder buidler = new StringBuilder(LocalSetting.getInstance().getMySelf().mMac).append('_')
               .append(new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())));
        mRemoteName = buidler.toString();

        int ret = ZipControl.zipFile(UserTrace.getTraceFilePath(), mZipPathName);
        if (ret != 0) {
        	Log.d(TAG, "zip file error");
            return;
        }

        File file = new File(mZipPathName);
        if (!file.exists() || file.length() == 0) {
        	Log.d(TAG, "file length error");
        	return;
        }

        mFtpUpload = new FtpUpload();
        mFtpUpload.setListener(new FtpUpload.FtpUploadListener() {
            @Override
            public void onUploadResult(int ret) {
                File file = new File(mZipPathName);
                if (file.exists()) {
                    file.delete();
                }
                if (ret == FtpUpload.UPLOAD_SUCESS) {
                    UserTrace.clearAllLogs();
                    mUploadSucess = true;
                    Log.d(TAG, "trace success");
                } else {
                	Log.d(TAG, "trace log failed");
                }
            }
        });
        mFtpUpload.uploadFile(mZipPathName, mRemoteName);
    }
}