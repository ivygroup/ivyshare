package com.ivyshare.util;

import com.ivyshare.engin.IvyService;
import com.ivyshare.engin.connection.NetworkManager;
import com.ivyshare.engin.control.ImManager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class IvyActivityBase extends Activity implements ServiceConnection {
    private static final String TAG = "IvyActivityBase";

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
/*
    @Override
    protected void onStop() {
        super.onStop();
        if (mImService != null) {
            mImService.checkMyActive();
        }
    } //*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
    	mIvyService = ((IvyService.LocalBinder)service).getService();
    	mImManager = mIvyService.getImManager();
    	mNetworkManager = mIvyService.getNetworkManager();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    	mImManager = null;
    	mNetworkManager = null;
    	mIvyService = null;
    }
}
