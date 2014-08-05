package com.ivy.ivyengine.control;

import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ivy.ivyengine.im.Im;
import com.ivy.ivyengine.im.Person;

public class UserStateMonitor {
    private static final String TAG = "UserStateMonitor";

    private Context mContext;
    private ImManager mImManager;
    private String mPackageName;
    private BroadcastReceiver mScreenActionReceiver;

    public UserStateMonitor(Context context, ImManager imManager) {
    	mContext = context;
        mImManager = imManager;

        mPackageName = context.getPackageName();
        if (mPackageName == null) {
            mPackageName = "com.ivyshare";
        }

        mScreenActionReceiver = new BroadcastReceiver() {   
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    mImManager.changeUserState(Im.State_Screen_Off);
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    mImManager.changeUserState(Im.State_Idle);
                } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                    if (isTopActivity()) {
                        mImManager.changeUserState(Im.State_Active);
                    } else {
                        mImManager.changeUserState(Im.State_Idle);
                    }
                }
            }
        };

        registerScreenActionReceiver(mContext);
    }

    public void release() {
        unRegisterScreenActionReceiver(mContext);
    }

    private void registerScreenActionReceiver(Context context) {   
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        context.registerReceiver(mScreenActionReceiver, filter);
    }

    private void unRegisterScreenActionReceiver(Context context) {
        if (mScreenActionReceiver != null) {
            context.unregisterReceiver(mScreenActionReceiver);
            mScreenActionReceiver = null;
        }
    }

    public void onResumeMyActivity() {
        Person myself = LocalSetting.getInstance().getMySelf();
        if (myself.mState != Im.State_Active) {
            mImManager.changeUserState(Im.State_Active);
        }
    }

    public void checkMyActive() {
        Person myself = LocalSetting.getInstance().getMySelf();
        boolean isTopActivity = isTopActivity();

        if (isTopActivity && myself.mState != Im.State_Active) {
            mImManager.changeUserState(Im.State_Active);
        } else if (!isTopActivity && myself.mState != Im.State_Idle) {
            mImManager.changeUserState(Im.State_Idle);
        }
    }

    //  Because this method need GET_TASKS permission, so we not use this method.
    //  <uses-permission android:name="android.permission.GET_TASKS" />
/*
     private boolean isTopActivity() {
        ActivityManager activityManager = (ActivityManager) MyApplication.getInstance().getSystemService(Context.ACTIVITY_SERVICE);

        List<RunningTaskInfo>  tasksInfo = activityManager.getRunningTasks(1);
        if(tasksInfo.size() > 0) {
            String topPackageName = tasksInfo.get(0).topActivity.getPackageName();
            Log.d(TAG, "top package name = " + topPackageName);
            if(mPackageName.equals(topPackageName)) {
                return true;
            }
        }
        return false;
    }*/

    private boolean isTopActivity() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> listInfos = activityManager.getRunningAppProcesses();
        if(listInfos.size() == 0) return false;

        for(ActivityManager.RunningAppProcessInfo processInfo:listInfos) {
            /*
            if(processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                Log.d(TAG, "importance = " + processInfo.importance + ", processName = " + processInfo.processName);
            }

            if (processInfo.processName.equals(mPackageName)) {
                Log.d(TAG, "importance = " + processInfo.importance + ", processName = " + processInfo.processName);
            }//*/

            if(processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && processInfo.processName.equals(mPackageName)) {
                return true;
            }
        }
        return false;
    }
}
