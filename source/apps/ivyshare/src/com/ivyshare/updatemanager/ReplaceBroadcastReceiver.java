package com.ivyshare.updatemanager;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

public class ReplaceBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG="UpdateManager";
	private String appName = "Ivy_Share.apk";
	private String savePath = "/Download/";
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		File downLoadApk = new File(Environment.getExternalStorageDirectory()+savePath,
		        appName);
		if(downLoadApk.exists()){
			downLoadApk.delete();
		}
		Log.i(TAG, "downLoadApkFile was deleted!"+Environment.DIRECTORY_DOWNLOADS);
		
		//resart the application
		//Intent i = arg0.getPackageManager()  
		//        .getLaunchIntentForPackage(arg0.getPackageName());  
		//i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
		//arg0.startActivity(i);  
	}

}
