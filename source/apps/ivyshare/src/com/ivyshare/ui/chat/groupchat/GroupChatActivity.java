package com.ivyshare.ui.chat.groupchat;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.ivyshare.R;
import com.ivyshare.engin.constdefines.IvyMessages;
import com.ivyshare.engin.control.GroupMessage;
import com.ivyshare.engin.data.Table_Message;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.chat.abstractchat.AbstractChatActivity;
import com.ivyshare.ui.chat.abstractchat.AdapterInterface;
import com.ivyshare.ui.chat.chat.ChatActivity;


public class GroupChatActivity extends AbstractChatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private GroupChatAdapter mListViewAdapter;

    private boolean mIsBroadCast;
    private String mGroupName;

    List<GroupMessage> mListMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserTrace.addTrace(UserTrace.ACTION_ENTER_BROADCAST);

        mIsBroadCast = getIntent().getBooleanExtra("isBroadCast", true);
        mGroupName = getIntent().getStringExtra("groupName");

        mTitle.setText(R.string.group_chat_broadcastname);
        mImageLeft.setImageResource(R.drawable.broadcast);
    }

    @Override
    public void onDestroy() {
        UserTrace.addTrace(UserTrace.ACTION_EXIT_BROADCAST);
        super.onDestroy();
    }

    @Override
    protected AdapterInterface createAdapter() {
        mListMessage = mImManager.getGroupMessageListClone(mIsBroadCast, mGroupName);
        mListViewAdapter = new GroupChatAdapter(mIsBroadCast, mGroupName, mListMessage, GroupChatActivity.this, 
                mImManager, mMapFileProcess, mSetExpandMessage, mListView);
        mListView.setAdapter(mListViewAdapter);
        return mListViewAdapter.getAdapterInterface();
    }

    @Override
    protected boolean createMessageReceiver() {
        IntentFilter filter = new IntentFilter(IvyMessages.INTENT_GROUP_MESSAGE);
        filter.setPriority(500);
        registerReceiver(mMessageReceiver, filter);
        return true;
    }

    @Override
    protected void clearUnReadMessage() {
        if (mImManager != null) {
            mImManager.clearGroupUnReadMessage(mIsBroadCast, mGroupName);
        }
    }

    @Override
    protected int SendMessage(FileType type, String content) {
        if (mImManager == null) {
            return -1;
        }

        UserTrace.addSendTrace(UserTrace.ACTION_SENDMESSAGE, type.ordinal(), UserTrace.BROADCAST_CHAT);
        if (type == FileType.FileType_CommonMsg) {
            mImManager.sendGroupMessage(mIsBroadCast, mGroupName, content);
        } else {
            mImManager.sendGroupFile(mIsBroadCast, mGroupName, "", content, type);
        }
        return 0;
    }

    @Override
    protected int SendMessage(FileType type, ArrayList<String> content) {
        if (mImManager == null) {
            return -1;
        }

		for (int i = 0; i < content.size(); i++) {
			UserTrace.addSendTrace(UserTrace.ACTION_SENDMESSAGE, type.ordinal(), UserTrace.BROADCAST_CHAT);
			mImManager.sendGroupFile(mIsBroadCast, mGroupName, "", content.get(i), type);
		}
        return 0;
    }
    
    @Override
    protected boolean isReceiveMessage(Intent intent) {
        int type = intent.getIntExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_TYPE, 0);
        boolean isBroadCast = intent.getBooleanExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_BROADCAST, false);
        //String groupName = intent.getStringExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_GROUPNAME);

        if (isBroadCast && isBroadCast == mIsBroadCast) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean needToScrollListView(Intent intent) {
        int type = intent.getIntExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_TYPE, 0);
        boolean isBroadCast = intent.getBooleanExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_BROADCAST, false);
        if (type != IvyMessages.VALUE_MESSAGETYPE_DELETE && type != IvyMessages.VALUE_MESSAGETYPE_RECOVER) {
            return true;
        }
        return false;
    }

    @Override
    protected void changeData() {
        mListMessage = mImManager.getGroupMessageListClone(mIsBroadCast, mGroupName);
        mListViewAdapter.changList(mListMessage);
        mListViewAdapter.notifyDataSetChanged();
    }

    @Override
    protected boolean onImServiceConnected() {
        return true;
    }

    @Override
    protected void recoverMessage() {
        if (mImManager != null) {
            GroupMessage message = (GroupMessage)mMessage;
            mImManager.recoverGroupMessage(mIsBroadCast, mGroupName, message.mFromPerson, mMessage.mType, 
                    mMessage.mContent, mMessage.mDirect == Table_Message.DIRECT_LOCALUSER, mMessage.mTime, mMessage.mState);
        }
    }

    @Override
    protected void deleteMesssages() {
        if (mImManager != null) {
            mImManager.deleteGroupMessage(mIsBroadCast, mGroupName);
            UserTrace.addTrace(UserTrace.ACTION_DELETE_CHAT);
        }
    }
}
