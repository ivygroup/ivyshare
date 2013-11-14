package com.ivyshare.util;

import com.ivyshare.MyApplication;
import com.ivyshare.updatemanager.CurrentVersion;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.Log;

public class APKCheck implements Callback{
	private static final String TAG = APKCheck.class.getSimpleName();

	public static final int APK_TYPE_NOTINSTALL 	= 0;
    public static final int APK_TYPE_SAME_VERSION	= 1;
    public static final int APK_TYPE_LOWER_VERSION	= 2;
    public static final int APK_TYPE_HIGHER_VERSION	= 3;

    public static final int APK_TYPE_NOTSELF		= 0;
	public static final int APK_TYPE_SELF 			= 1;

    private final Handler mMainThreadHandler = new Handler(this);
    private CheckFinishListener mCheckListener;
    private APKLoadThread mLoaderThread;

    public interface CheckFinishListener {
        public boolean onAPKCheckFinished(Object obj, int type, int self);
    }

    public APKCheck(CheckFinishListener listener) {
    	mCheckListener = listener;
    }

    public void unInit() {
    	if (mLoaderThread != null) {
    		Log.d(TAG, "Quit Loader Thread");
    		mLoaderThread.quit();
    	}
    }

    public void requestAPK(Object obj, String packageName, int version) {
        if (mLoaderThread == null) {
            mLoaderThread = new APKLoadThread();
            mLoaderThread.start();
        }
        mLoaderThread.requestAPK(new APKInfo(obj, packageName, version));
    }

	@Override
	public boolean handleMessage(Message msg) {
		if (mCheckListener != null) {
			APKInfo info = (APKInfo)msg.obj;
			mCheckListener.onAPKCheckFinished(info.mObject, msg.arg1, msg.arg2);
		}
		return true;
	}
	
	private class APKInfo {
		public String mPackageName;
		public int mVersion;
		public Object mObject;
		APKInfo(Object obj, String packageName, int version) {
			mPackageName = packageName;
			mObject = obj;
			mVersion = version;
		}
	}

    private class APKLoadThread extends HandlerThread implements Callback {
        private Handler mThreadHandler;
        public APKLoadThread() {
            super("APKLoadThread");
        }

        public void requestAPK(APKInfo info) {
            if (mThreadHandler == null) {
            	mThreadHandler = new Handler(getLooper(), this);
            }
            mThreadHandler.sendMessage(mThreadHandler.obtainMessage(0, info));
        }

        public boolean handleMessage(Message msg) {
        	APKInfo info = (APKInfo)msg.obj;
    		String packageName = info.mPackageName;
    		PackageManager pm = MyApplication.getInstance().getPackageManager();
            PackageInfo packageInfo = null;
            try {
            	packageInfo = pm.getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        	int type = APK_TYPE_NOTINSTALL;
            if (packageInfo == null) {
            	type = APK_TYPE_NOTINSTALL;
            } else if (info.mVersion > packageInfo.versionCode){
            	type = APK_TYPE_HIGHER_VERSION;
            } else if (info.mVersion == packageInfo.versionCode) {
            	type = APK_TYPE_SAME_VERSION;
            } else {
            	type = APK_TYPE_LOWER_VERSION;
            }
            int self = APK_TYPE_NOTSELF;
            if (info.mPackageName.compareTo(CurrentVersion.appPackName) == 0) {
            	self = APK_TYPE_SELF;
            }
            mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(0, type, self, msg.obj));
        	return true;
        }
    }
}