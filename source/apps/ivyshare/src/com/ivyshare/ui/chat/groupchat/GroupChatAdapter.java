package com.ivyshare.ui.chat.groupchat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.ivyshare.engin.control.ChatMessage;
import com.ivyshare.engin.control.GroupMessage;
import com.ivyshare.engin.control.ImService;
import com.ivyshare.engin.im.Person;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.ui.chat.abstractchat.AdapterHelper;
import com.ivyshare.ui.chat.abstractchat.AdapterInterface;

public class GroupChatAdapter extends BaseAdapter {
    private static final String TAG = GroupChatAdapter.class.getSimpleName();

    private List<GroupMessage> mMessages;
    private boolean mIsBroadCast;
    private String mGroupName;
    private GroupChatAdapterHelper mAdapterHelper;
    private ImService mImService;

    public GroupChatAdapter(boolean isBroadCast, String groupName, List<GroupMessage> message, Context context,
            ImService imService, Map<Integer, Integer> mapFileProcess, Set<Integer> setExpandMessage, ListView listView) {
        mMessages = message;
        mIsBroadCast = isBroadCast;
        mGroupName = groupName;
        mImService = imService;

        mAdapterHelper = new GroupChatAdapterHelper(context, imService, mapFileProcess, setExpandMessage, listView);
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
                ImService service, Map<Integer, Integer> process, Set<Integer> expand, ListView view) {
            super(context, service, process, expand, view);
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
            if (mImService != null) {
                mImService.deleteGroupMessage(mIsBroadCast, mGroupName, message.mId);
                return 0;
            }
            return -1;
        }

        @Override
        protected int resendMesssage(ChatMessage message) {
            if (mImService != null) {
                deleteMessage(message);
                if (message.mType == FileType.FileType_CommonMsg) {
                    mImService.sendGroupMessage(mIsBroadCast, mGroupName, message.mContent);
                } else {
                    mImService.sendGroupFile(mIsBroadCast, mGroupName, "", message.mContent, message.mType);
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
