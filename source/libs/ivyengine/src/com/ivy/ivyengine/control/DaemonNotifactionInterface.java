package com.ivy.ivyengine.control;

import android.content.Intent;

public interface DaemonNotifactionInterface {
    public void release();

    public void addAndNotify(Intent intent);
    public void addGroupAndNotify(Intent intent);

    public void startBackgroundNotificationIfNeed();
    public void startBackgroundNotification();    
    public int startMessageNotification(String prompt);

    public int getNotificationState();
}
