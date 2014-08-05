package com.ivyshare.ui.chat.groupchat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.ivy.ivyengine.control.ChatMessage;
import com.ivy.ivyengine.control.GroupMessage;
import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.ui.chat.abstractchat.AdapterHelper;
import com.ivyshare.ui.chat.abstractchat.AdapterInterface;

public class GroupChatAdapter extends BaseAdapter {
    private static final String TAG = GroupChatAdapter.class.getSimpleName();

    private List<GroupMessage> mMessages;
    private boolean mIsBroadCast;
    private String mGroupName;
    private GroupChatAdapterHelper mAdapterHelper;
    private ImManager mImManager;

    public GroupChatAdapter(boolean isBroadCast, String groupName, List<GroupMessage> message, Context context,
            ImManager imManager, Map<Integer, Integer> mapFileProcess, Set<Integer> setExpandMessage, ListView listView) {
        mMessages = message;
        mIsBroadCast = isBroadCast;
        mGroupName = groupName;
        mImManager = imManager;

        mAdapterHelper = new GroupChatAdapterHelper(context, imManager, mapFileProcess, setExpandMessage, listView);
    }

    @Override
    public int getCount() {
        if (mMessages != null) {
            return mMessages.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int arg0) {
        if (mMessages != null) {
            if (arg0 < 0) {
                return 0;
            }
            if (arg0 >= mMessages.size()) {
                return 0;
            }
            return mMessages.get(arg0);
        }
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null || view != null && mAdapterHelper.needNewView(view)) {
            view = mAdapterHelper.newView();
        }

        GroupMessage message = mMessages.get(position);
        mAdapterHelper.setViewTag(view, message);
        mAdapterHelper.initViewItem(view);
        return view;
    }

    public void changList(List<GroupMessage> message) {
        mMessages = message;
    }

    public AdapterInterface getAdapterInterface() {
        return mAdapterHelper;
    }

    public class GroupChatAdapterHelper extends AdapterHelper {
        public GroupChatAdapterHelper (Context context,
                ImManager imManager, Map<Integer, Integer> process, Set<Integer> expand, ListView view) {
            super(context, imManager, process, expand, view);
        }

        protected Person getRemotePerson(ChatMessage message) {
            GroupMessage groupMessage = (GroupMessage)message;
            return groupMessage.mFromPerson;
        }

        public void notifyDataSetChanged() {
            GroupChatAdapter.this.notifyDataSetChanged();
        }

        @Override
        protected int deleteMessage(ChatMessage message) {
            if (mImManager != null) {
                mImManager.deleteGroupMessage(mIsBroadCast, mGroupName, message.mId);
                return 0;
            }
            return -1;
        }

        @Override
        protected int resendMesssage(ChatMessage message) {
            if (mImManager != null) {
                deleteMessage(message);
                if (message.mType == FileType.FileType_CommonMsg) {
                    mImManager.sendGroupMessage(mIsBroadCast, mGroupName, message.mContent);
                } else {
                    mImManager.sendGroupFile(mIsBroadCast, mGroupName, "", message.mContent, message.mType);
                }
                return 0;
            }
            return -1;
        }

        @Override
        public int getCount() {
            return GroupChatAdapter.this.getCount();
        }
    }
}
