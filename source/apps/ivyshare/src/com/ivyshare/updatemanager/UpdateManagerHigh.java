package com.ivyshare.updatemanager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

 public class UpdateManagerHigh extends UpdateManager{
     
    private DownloadManager mDownloadManager;
    private SharedPreferences mPrefs;
    private static final String DL_ID = "updateId";  
    
    public UpdateManagerHigh(Context context) {
        super(context);
        mDownloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);  
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);  
    }
    @SuppressLint("NewApi")
    protected void Init() {
        this.mDownloadManager.remove(this.mPrefs.getLong(DL_ID, 0));   
        this.mPrefs.edit().clear().commit();   
    }

    
    @SuppressLint("NewApi") 
    protected void downAppFile(final String url) {
        if(!mPrefs.contains(DL_ID)) {   
            //开始下载   
            Uri resource = Uri.parse(url);   
            DownloadManager.Request request = new DownloadManager.Request(resource);   
            request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);   
            request.setAllowedOverRoaming(false);   
            //设置文件类型  
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();  
            String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));  
            request.setMimeType(mimeString);  
            //在通知栏中显示   
            //request.setNotificationVisibility(View.VISIBLE);
            request.setShowRunningNotification(true);
            request.setVisibleInDownloadsUi(true);
            //sdcard的目录下的download文件夹
            request.setDestinationInExternalPublicDir(savePath, appName);  
            request.setTitle(appName);   
            request.setDescription(appName+" DownLoad");
            long id = mDownloadManager.enqueue(request);   
            //保存id   
            mPrefs.edit().putLong(DL_ID, id).commit();   
            
            mContext.registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {   
            //下载已经开始，检查状态  
            queryDownloadStatus();   
        } 
    }
    
    private BroadcastReceiver completeReceiver = new BroadcastReceiver() {   
        @Override   
        public void onReceive(Context context, Intent intent) {   
            //这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听  
            Log.e(TAG, ""+intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));  
            queryDownloadStatus();   
        }   
    }; 
    @SuppressLint("NewApi") 
    private void queryDownloadStatus() {   
        DownloadManager.Query query = new DownloadManager.Query();   
        query.setFilterById(mPrefs.getLong(DL_ID, 0));   
        Cursor c = mDownloadManager.query(query);   
        if(c.moveToFirst()) {   
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));   
            switch(status) {   
            case DownloadManager.STATUS_PAUSED:   
                Log.e(TAG, "STATUS_PAUSED");  
            case DownloadManager.STATUS_PENDING:   
                Log.e(TAG, "STATUS_PENDING");  
            case DownloadManager.STATUS_RUNNING:   
                //正在下载，不做任何事情  
                Log.e(TAG, "STATUS_RUNNING");  
                break;   
            case DownloadManager.STATUS_SUCCESSFUL:   
                //完成
                //haveDownLoad();
                installNewApk();
                break;   
            case DownloadManager.STATUS_FAILED:   
                //清除已下载的内容，重新下载  
                Log.e("down", "STATUS_FAILED");  
                mDownloadManager.remove(mPrefs.getLong(DL_ID, 0));   
                mPrefs.edit().clear().commit();   
                break;   
            }   
        }  
    }  
}
