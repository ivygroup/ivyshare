package com.ivyshare;

import com.ivy.ivyengine.IvyConfig;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication gInstance = null;
    
    public static MyApplication getInstance() {
        return gInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gInstance = this;
        IvyConfig.init(this, "ivyshare.db", "ivysetting.db", "IvyShare", false);
    }
}
