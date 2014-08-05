
package com.ivy.ivyengine;

import com.ivy.ivyengine.control.LocalSetting;

import android.content.Context;


public class IvyConfig {
    private static String sIvyDataName = "ivyshare.db";
    private static String sIvySettingName = "ivysetting.db";
    private static String sIvyStoragePathName = "IvyShare";
    private static boolean sIsUseKeepAlive = false;

    public static void init(Context context, String dataName, String settingName, String storagePathName, boolean isUseKeepAlive) {
        if (dataName != null) {
            sIvyDataName = dataName;
        }

        if (settingName != null) {
            sIvySettingName = settingName;
        }

        if (storagePathName != null) {
            sIvyStoragePathName = storagePathName;
        }

        sIsUseKeepAlive = isUseKeepAlive;

        LocalSetting.initInstance(context);
    }

    private static IvyConfig instance;
    public static IvyConfig getInstance() {
        if(instance == null) {
            instance = new IvyConfig();
        }
        return instance;
    }

    private IvyConfig() {
    }

    public String getDataName() {
        return sIvyDataName;
    }

    public String getSettingName() {
        return sIvySettingName;
    }

    public String getStoragePathName() {
        return "/" + sIvyStoragePathName + "/";
    }

    public boolean isUseKeepAlive() {
        return sIsUseKeepAlive;
    }
}
