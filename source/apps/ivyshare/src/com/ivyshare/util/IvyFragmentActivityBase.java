package com.ivyshare.util;

import com.ivyshare.engin.control.ImService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class IvyFragmentActivityBase extends FragmentActivity implements ServiceConnection {
    private static final String TAG = "IvyFragmentActivityBase";
    protected ImService mImService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, ImService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mImService != null) {
            mImService.onResumeMyActivity();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mImService != null) {
            mImService.checkMyActive();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mImService != null) {
            mImService.checkMyActive();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mImService = ((ImService.LocalBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mImService = null;
    }
}
