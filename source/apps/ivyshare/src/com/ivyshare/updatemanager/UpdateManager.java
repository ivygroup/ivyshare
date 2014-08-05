package com.ivyshare.updatemanager;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.ivyshare.R;
import com.ivyshare.util.CommonUtils;


public class UpdateManager {
    /** Called when the activity is first created. */
    protected static final String TAG = "UpdateManager";

    public String appName = "Ivy_Share.apk";
    public String savePath = "/Download/";
    public Context mContext;
    public Context mApplicationContext;
    
    private String downPath = "http://amapig.com/ivy/";
    private String appVersion = "version.json";
    private int newVerCode = 0;
    private String newVerName = "";
    private String newVerChange = "";
    
    private Dialog mInstallDialog = null;
    private Dialog mUpdateDialog = null;
    private ProgressDialog mProgressDialog = null;
    private ProgressDialog mDownLoadProgressDialog = null;

    private static UpdateManager mUpdateManager = null;
    
    public static final int UPDATE_MANAGER_UPDATE_DIALOG = 701;
    public static final int UPDATE_MANAGER_NETWORK_ERROR = 702;
    public static final int UPDATE_MANAGER_UPDATE_PROGESS = 703;
    public static final int UPDATE_MANAGER_DOWNLOAD_ERROR = 704;
    public static final int UPDATE_MANAGER_LATEST_VERSION = 705;
    
    protected boolean isCancel = true;
    static public boolean isDownLoading = false;
    
    public Handler mHandler=new Handler(){
        public void handleMessage(Message msg) {
            Log.e(TAG, "New Message: " + msg);
            switch (msg.what) {
            case UPDATE_MANAGER_UPDATE_DIALOG:
                try {
                    showUpdateDialog();
                } catch (NameNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            case UPDATE_MANAGER_NETWORK_ERROR:
                Toast.makeText(mContext, R.string.network_available_error,Toast.LENGTH_SHORT).show();
                break;
            case UPDATE_MANAGER_LATEST_VERSION:
                Toast.makeText(mContext, R.string.latest_version,Toast.LENGTH_SHORT).show();
                break;
            case UPDATE_MANAGER_UPDATE_PROGESS:
                Update(msg.arg1);
                break;
            case UPDATE_MANAGER_DOWNLOAD_ERROR:
                Toast.makeText(mApplicationContext, R.string.download_file_fail,Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }  
    };
    
    public static UpdateManager getInstance(Context context) {
        
        if(mUpdateManager != null ){
            destoryInstance();
        }
        
        if (mUpdateManager == null) {
            mUpdateManager = new UpdateManagerLow(context);
        }
        
        mUpdateManager.Init();
        
        return mUpdateManager;
    }

    public static void destoryInstance() {
        Log.e(TAG, "Destory " + mUpdateManager);
        
        if(mUpdateManager == null)
            return;
        
        if( mUpdateManager.mInstallDialog != null )
            mUpdateManager.mInstallDialog.dismiss();
        
        if( mUpdateManager.mUpdateDialog != null )
            mUpdateManager.mUpdateDialog.dismiss();
        
        if( mUpdateManager.mProgressDialog != null )
            mUpdateManager.mProgressDialog.dismiss();
        
        if (mUpdateManager != null) {
            mUpdateManager = null;
        }
    }
    
    public UpdateManager(Context context) {
        this.mContext = context;
        mApplicationContext = context.getApplicationContext();
    }
    
    protected void Init() {
        return;
    }
    
    protected void Update(int value) {
        if( mDownLoadProgressDialog != null )
        mDownLoadProgressDialog.setMessage(mContext.getString(R.string.download_file_progress)
                                            +" "+value+"%");
        return;
    }
    
    public void checkUpdate()
    {
        if( isDownLoading == true ){
            Toast.makeText(mContext, R.string.update_doing,Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(R.string.update_wait_check,R.string.update_check);
        if(CommonUtils.isNetworkAvailable() == false){
            mProgressDialog.dismiss();
            Toast.makeText(mContext, R.string.network_available_error,Toast.LENGTH_SHORT).show();
            return;
        }else{
            new Thread() {
                public void run() {
                    try {
                        checkToUpdate();
                    } catch (NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mProgressDialog.dismiss();
                }
            }.start();

        }
        
    }
    private void showProgress(int strRes,int title){
        if(mProgressDialog == null)
            mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(mContext.getString(title));
        mProgressDialog.setMessage(mContext.getString(strRes));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
    }
    
    private void showDownLoadProgress(int title,int strRes){
        if(mDownLoadProgressDialog == null)
            mDownLoadProgressDialog = new ProgressDialog(mContext);
        mDownLoadProgressDialog.setTitle(mContext.getString(title));
        mDownLoadProgressDialog.setMessage(mContext.getString(strRes) + " 0%");
        mDownLoadProgressDialog.setCancelable(true);
        mDownLoadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDownLoadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                mContext.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDownLoad();
                        mDownLoadProgressDialog.dismiss();
                    }
                });
        mDownLoadProgressDialog.show();
    }
    
    public void cancelDownLoad()
    {
        isCancel = true;
        isDownLoading = false;
    }
    
    //Get ServerVersion from GetUpdateInfo.getUpdateVerJSON
    private boolean getServerVersion() {
        // TODO Auto-generated method stub
        try{
            String newVerJSON = GetUpdateInfo.getUpdataVerJSON(downPath + appVersion);
            JSONArray jsonArray = new JSONArray(newVerJSON);
            if(jsonArray.length() > 0){
                JSONObject obj = jsonArray.getJSONObject(0);
                try{
                    newVerCode = Integer.parseInt(obj.getString("verCode"));
                    newVerName = obj.getString("verName");
                    newVerChange = obj.getString("verChange");
                }catch(Exception e){
                    Log.e(TAG, e.getMessage());
                    newVerCode = -1;
                    newVerName = "";
                    return false;
                }
            }
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }
    
    //check new version and update
    private void checkToUpdate() throws NameNotFoundException {
        // TODO Auto-generated method stub
        if(getServerVersion()){
            int currentCode = CurrentVersion.getVerCode(this.mContext);
            if(newVerCode > currentCode)
            {//Current Version is old
                //弹出更新提示对话框
                Log.e(TAG,"newVerCode = "+newVerCode+" currentCode = "+currentCode );
                mHandler.sendEmptyMessage(UPDATE_MANAGER_UPDATE_DIALOG);
            }else{
                mHandler.sendEmptyMessage(UPDATE_MANAGER_LATEST_VERSION);
            }
        }else{
            mHandler.sendEmptyMessage(UPDATE_MANAGER_NETWORK_ERROR);
        }
    }
    //show Update Dialog
    private void showUpdateDialog() throws NameNotFoundException {
        // TODO Auto-generated method stub
        StringBuffer sb = new StringBuffer();
        sb.append(mContext.getString(R.string.current_version));
        sb.append(CurrentVersion.getVerName(this.mContext));
        sb.append("\n");
        sb.append(mContext.getString(R.string.found_version));
        sb.append(newVerName);
        sb.append("\n");
        sb.append(mContext.getString(R.string.update_content));
        sb.append("\n");
        sb.append(newVerChange);
        sb.append("\n");
        sb.append(mContext.getString(R.string.whether_update));
        
        mUpdateDialog = new AlertDialog.Builder(new ContextThemeWrapper(this.mContext, R.style.AlertDialogCustom))
        .setTitle(R.string.software_update)
        .setMessage(sb.toString())
        .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            @Suppress
            public void onClick(DialogInterface dialog, int which) {
                
                File downLoadApk = new File(Environment.getExternalStorageDirectory()+savePath,
                        appName);
                if(downLoadApk.exists()){
                    downLoadApk.delete();
                }
                
                // TODO Auto-generated method stub
                isDownLoading = true;
                
                downAppFile(downPath + appName);
            }
        })
        .setNegativeButton(R.string.not_update, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                isDownLoading = false;
            }
        }).create();
        mUpdateDialog.show();
    }
    
    protected void downAppFile(final String url) {
        isCancel = false;
        //showDownLoadProgress(R.string.download_file,R.string.download_file_progress);
        return;
    }
    
    //cancel progressBar and start new App
    protected void haveDownLoad() {
        installNewApk();
        isDownLoading = false;
        // TODO Auto-generated method stub
//        if( mDownLoadProgressDialog != null)
//            mDownLoadProgressDialog.dismiss();
//        mHandler.post(new Runnable(){
//            public void run(){
//                //弹出警告框 提示是否安装新的版本
//                mInstallDialog = new AlertDialog.Builder(mContext)
//                .setTitle(mContext.getString(R.string.download_complete))
//                .setMessage(mContext.getString(R.string.install_application))
//                .setPositiveButton(mContext.getString(R.string.ok), new DialogInterface.OnClickListener(){
//                    @Suppress
//                    public void onClick(DialogInterface dialog, int which) {
//                        // TODO Auto-generated method stub
//                        installNewApk();
//                        }
//                    })
//                    .setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                        @Suppress
//                        public void onClick(DialogInterface dialog, int which) {
//                            // TODO Auto-generated method stub
//                            }
//                        })
//                        .create();
//                mInstallDialog.show();
//                }
//            });
    }
    
    protected void downLoadError() {
        mHandler.sendEmptyMessage(UPDATE_MANAGER_DOWNLOAD_ERROR);
        isDownLoading = false;
    }
    //安装新的应用
    protected void installNewApk() {
        // TODO Auto-generated method stub
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(
                new File(Environment.getExternalStorageDirectory()+savePath,appName)),
                CommonUtils.MIMETYPE_APPLICATION);
        mApplicationContext.startActivity(intent);
    }
}
