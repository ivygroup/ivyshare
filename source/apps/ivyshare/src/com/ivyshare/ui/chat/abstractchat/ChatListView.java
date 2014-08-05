package com.ivyshare.ui.chat.abstractchat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.ListView;

public class ChatListView extends ListView implements SwipeHelper.Callback {
    private static final String TAG = ChatListView.class.getSimpleName();

    public interface ListViewCallBack {
        public void onChildDismissed(View v);
        public void showCannotSwipe();
        public boolean canDismissed(View v);
    }

    private SwipeHelper mSwipeHelper = null;
    private ListViewCallBack mCallBack = null;
    private Context mContext;

    public ChatListView(Context context) {
        this(context, null);
    }

    public ChatListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        float densityScale = getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
         mSwipeHelper = new SwipeHelper(SwipeHelper.X, this, densityScale,
                    pagingTouchSlop, context);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
           return mSwipeHelper.onInterceptTouchEvent(ev) | super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
           return mSwipeHelper.onTouchEvent(ev) | super.onTouchEvent(ev);
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        final int count = getChildCount();
        int touchY = (int) ev.getY();
        int childIdx = 0;
        View slidingChild;
        for (; childIdx < count; childIdx++) {
            slidingChild = getChildAt(childIdx);
            if (touchY >= slidingChild.getTop() && touchY <= slidingChild.getBottom()) {
                return slidingChild;
            }
        }
        return null;
    }

    @Override
    public View getChildContentView(View v) {
        if (AbstractChatActivity.mSDKVersion < AbstractChatActivity.mMinSDK) {
            v.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
            ImageView imageView = new ImageView(mContext);
            imageView.setBackgroundColor(0xffffffff);
            imageView.setImageBitmap(bitmap);
            return imageView;
        }
        return v;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        if (mCallBack != null) {
            return mCallBack.canDismissed(v);
        }
        return true;
    }

    public void setStatusBarHeight(int height) {
        mSwipeHelper.setStatusBarHeight(height);
    }

    public void setListViewCallBack(ListViewCallBack callback) {
        mCallBack = callback;
    }

    @Override
    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
    }

    @Override
    public void onChildDismissed(View v) {
        if (mCallBack != null) {
            mCallBack.onChildDismissed(v);
        }
    }

    @Override
    public void showCannotSwipe() {
        if (mCallBack != null) {
            mCallBack.showCannotSwipe();
        }
    }

    @Override
    public void onDragCancelled(View v) {
    }
}