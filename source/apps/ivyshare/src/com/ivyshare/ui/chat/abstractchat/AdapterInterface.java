package com.ivyshare.ui.chat.abstractchat;

import com.ivyshare.engin.control.ChatMessage;

import android.view.MotionEvent;
import android.view.View;

public interface AdapterInterface {
    public void notifyProcessChanged();
    public boolean canViewDismissed(View selectedView);
    public ChatMessage handleSwipe(View selectedView);
    public void unInit();
    public int getCount();
    public void onTouchEvent(View v, MotionEvent event);
}