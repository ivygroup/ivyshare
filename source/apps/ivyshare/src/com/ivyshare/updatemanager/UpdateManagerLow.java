package com.ivyshare.updatemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import com.ivyshare.R;

public class UpdateManagerLow extends UpdateManager{
    
    private Notification notification=new Notification();
    private RemoteViews mView = null;
    private NotificationManager mManager=null;
    public final static int DOWNLOAD_STATUS = 7;

    public UpdateManagerLow(Context context) {
        super(context);
        
        mView = new RemoteViews(context.getPackageName(),R.layout.notify_progress_view);
        
        mManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    
    protected void Update(int value) {
        updataProgressBar(value);
        super.Update(value);
        return;
    }
    
    protected void updataProgressBar(int progress) {
        // TODO Auto-generated method stub
     // TODO Auto-generated method stub        
        mView.setTextViewText(R.id.notificationTitle, appName+" download");
        mView.setProgressBar(R.id.notificationProgress, 100, progress, false);
        mView.setTextViewText(R.id.notificationPercent, progress+"%");
        notification.contentView=mView;

        Intent notificationIntent = new Intent(mApplicationContext, mApplicationContext.getClass());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(mApplicationContext, 0, 
            notificationIntent, 0);
        
        notification.contentIntent=contentIntent;
        
        if( progress != 100){
            notification.icon= android.R.drawable.stat_sys_download;
        }
        else {
            notification.icon= R.drawable.update_download;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
        }
        
        mManager.notify(DOWNLOAD_STATUS, notification);
    }
    
    public void cancelDownLoad()
    {
        super.cancelDownLoad();
        mManager.cancelAll();
    }
    
    protected void downAppFile(final String url) {
        super.downAppFile(url);
        new Thread(){
            public void run(){
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(url);
                HttpResponse response;
                String savePathString = null;
                
                savePathString = Environment.getExternalStorageDirectory() + savePath;
                
                try {
                    response = client.execute(get);
                    HttpEntity entity = response.getEntity();
                    
                    long length = entity.getContentLength();
                    Log.isLoggable("DownTag", (int) length);
                    if( length <= 10*1024 ){
                        downLoadError();
                        return;
                    }
                    
                    InputStream is = entity.getContent();
                    FileOutputStream fileOutputStream = null;
                    long hadwrite = 0;
                    if(is == null){
                        throw new RuntimeException("isStream is null");
                    }
                    File file = new File(savePathString);
                    if (!file.exists())
                    {
                        file.mkdir();
                    }
                    
                    File mFilefile = new File(savePathString+appName);
                    
                    fileOutputStream = new FileOutputStream(mFilefile);
                    byte[] buf = new byte[1024*100];
                    int ch = -1;
                    do{
                        if( hadwrite == 0 || hadwrite == length || hadwrite/(1024*10)%9 == 7){
                            Message msg = mHandler.obtainMessage(UPDATE_MANAGER_UPDATE_PROGESS);
                            msg.arg1 = (int) (hadwrite*100/length);
                            msg.what = UPDATE_MANAGER_UPDATE_PROGESS;
                            mHandler.sendMessage(msg);
                        }
                        
                        ch = is.read(buf);
                        if(ch <= 0)break;
                        fileOutputStream.write(buf, 0, ch);
                        hadwrite += ch;
                    }while(isCancel == false);
                    is.close();
                    fileOutputStream.close();
                    
                    if( isCancel == false ){
                        if( hadwrite < length ){
                            downLoadError();
                            return;
                        }
                        
                        haveDownLoad();
                    }else {
                        if(mFilefile.exists() == true)
                        {
                            mFilefile.delete();
                        }
                    }
                    }catch(ClientProtocolException e){
                        e.printStackTrace();
                        }catch(IOException e){
                        e.printStackTrace();
                        }
                }
        }.start();
    }

}
