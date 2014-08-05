package com.ivyshare.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;

import com.ivy.ivyengine.IvyService;
import com.ivy.ivyengine.connection.NetworkManager;
import com.ivy.ivyengine.control.ImManager;
import com.ivyshare.ui.DaemonNotifaction;

public class IvyFragmentActivityBase extends FragmentActivity implements ServiceConnection {
    private static final String TAG = "IvyFragmentActivityBase";
    
    private IvyService mIvyService;
    protected ImManager mImManager;
    protected NetworkManager mNetworkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, IvyService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mImManager != null) {
        	mImManager.onResumeMyActivity();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mImManager != null) {
        	mImManager.checkMyActive();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mImManager != null) {
        	mImManager.checkMyActive();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mIvyService = ((IvyService.LocalBinder)service).getService();
        ImManager imManager = mIvyService.getImManager();
        if (imManager.getDaemonNotifaction() == null) {
            DaemonNotifaction tmp = new DaemonNotifaction(this.getApplicationContext(),
                    imManager.getImData(), imManager.getPersonMessage(), imManager.getGroupMessage());
            imManager.setDaemonNotifaction(tmp);
        }
        mImManager = imManager;
        mNetworkManager = mIvyService.getNetworkManager();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    	mImManager = null;
    	mNetworkManager = null;
    	mIvyService = null;
    }
}
