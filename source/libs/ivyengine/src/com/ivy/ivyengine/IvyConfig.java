
package com.ivy.ivyengine;

import com.ivy.ivyengine.control.LocalSetting;

import android.content.Context;


public class IvyConfig {
    private static String IvyDataName = "ivyshare.db";
    private static String IvySettingName = "ivysetting.db";
    private static String IvyStoragePathName = "IvyShare";

    public static void init(Context context, String dataName, String settingName, String storagePathName) {
        if (dataName != null) {
            IvyDataName = dataName;
        }

        if (settingName != null) {
            IvySettingName = settingName;
        }

        if (storagePathName != null) {
            IvyStoragePathName = storagePathName;
        }

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
        return IvyDataName;
    }

    public String getSettingName() {
        return IvySettingName;
    }

    public String getStoragePathName() {
        return "/" + IvyStoragePathName + "/";
    }
}
