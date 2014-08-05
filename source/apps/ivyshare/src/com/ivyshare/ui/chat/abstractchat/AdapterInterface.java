package com.ivyshare.ui.chat.abstractchat;

import android.view.MotionEvent;
import android.view.View;

import com.ivy.ivyengine.control.ChatMessage;

public interface AdapterInterface {
    public void notifyProcessChanged();
    public boolean canViewDismissed(View selectedView);
    public ChatMessage handleSwipe(View selectedView);
    public void unInit();
    public int getCount();
    public void onTouchEvent(View v, MotionEvent event);
}