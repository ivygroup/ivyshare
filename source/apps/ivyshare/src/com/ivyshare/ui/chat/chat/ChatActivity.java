package com.ivyshare.ui.chat.chat;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ivyshare.R;
import com.ivyshare.engin.constdefines.IvyMessages;
import com.ivyshare.engin.control.PersonManager;
import com.ivyshare.engin.data.Table_Message;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.engin.im.Person;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.chat.abstractchat.AbstractChatActivity;
import com.ivyshare.ui.chat.abstractchat.AdapterInterface;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.CommonUtils;

public class ChatActivity extends AbstractChatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private Cursor mCursor;
    private ChatAdapter mAdapter;
    private Person mPerson;
    private String mKeyString;
    private PersonBroadCastReceiver mPersonReceiver = null;
    private boolean mPersonReceiverRegister = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserTrace.addTrace(UserTrace.ACTION_ENTER_ONE2ONE);

        mKeyString = getIntent().getStringExtra("chatpersonKey");
        mImageLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (mKeyString != null && mImManager != null
                        && mImManager.getPerson(mKeyString) != null) {
                    intent.putExtra("chatpersonKey", mKeyString);
                    intent.setClass(ChatActivity.this, QuickPersonInfoActivity.class);
                    startActivity(intent);
                }
            }
        });

        IntentFilter filter = new IntentFilter(IvyMessages.INTENT_PERSON);
        mPersonReceiver = new PersonBroadCastReceiver();
        registerReceiver(mPersonReceiver, filter);
        mPersonReceiverRegister = true;
    }

    @Override
    public void onDestroy() {
        UserTrace.addTrace(UserTrace.ACTION_EXIT_ONE2ONE);
        if (mCursor != null) {
            mCursor.close();
        }
        if (mPersonReceiverRegister) {
            unregisterReceiver(mPersonReceiver);
            mPersonReceiverRegister = false;
        }
        super.onDestroy();
    }

    private void sendOfflineMessage(String personName) {
        String information = String.format(
                getString(R.string.could_not_send_message), personName);
        Toast.makeText(this, information, Toast.LENGTH_SHORT).show();
    }

    private boolean updatePersonState() {
        if (mImManager == null) {
            return false;
        }
        mPerson = mImManager.getPerson(mKeyString);
        if (mPerson == null) {
            return false;
        }

        CommonUtils.getPersonPhoto(mImageLeft, mPerson.mImage);
        mTitle.setText(mPerson.mNickName);
        Resources resource = getResources(); 
        if (mPerson.isOnline()) {
            mTitle.setTextColor(resource.getColor(R.color.action_bar_text_text_color));
        } else {
            mTitle.setTextColor(resource.getColor(R.color.action_bar_text_text_color_disabled));
        }
        return true;
    }

    private class PersonBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
            updatePersonState();
        }
    }

    @Override
    protected AdapterInterface createAdapter() {
        mCursor = mImManager.getPersonHistoryMessage(mPerson);
        mAdapter = new ChatAdapter(mPerson, ChatActivity.this, 
                mCursor, mImManager, mMapFileProcess, mSetExpandMessage, mListView);
        mListView.setAdapter(mAdapter);
        return mAdapter.getAdapterInterface();
    }

    @Override
    protected boolean createMessageReceiver() {
        IntentFilter filter = new IntentFilter(IvyMessages.INTENT_MESSAGE);
        filter.setPriority(500);
        registerReceiver(mMessageReceiver, filter);
        return true;
    }

    @Override
    protected void clearUnReadMessage() {
        if (mImManager != null && mPerson != null) {
            mImManager.clearUnReadMessage(mPerson);
        }
    }

    @Override
    protected int SendMessage(FileType type, String content) {
        if (mImManager == null || mPerson == null) {
            return -1;
        }
        if (!mPerson.isOnline()) {
            sendOfflineMessage(mPerson.mNickName);
            return -2;
        }
        UserTrace.addSendTrace(UserTrace.ACTION_SENDMESSAGE, type.ordinal(), UserTrace.ONE2ONE_CHAT);
        if (type == FileType.FileType_CommonMsg) {
            mImManager.sendMessage(mPerson, content);
        } else {
            mImManager.sendFile(mPerson, "", content, type);
        }
        return 0;
    }
    
    @Override
    protected int SendMessage(FileType type, ArrayList<String> content) {
        if (mImManager == null || mPerson == null) {
            return -1;
        }
        if (!mPerson.isOnline()) {
            sendOfflineMessage(mPerson.mNickName);
            return -2;
        }
		for (int i = 0; i < content.size(); i++) {
			UserTrace.addSendTrace(UserTrace.ACTION_SENDMESSAGE, type.ordinal(), UserTrace.ONE2ONE_CHAT);
			mImManager.sendFile(mPerson, "", content.get(i), type);
		}
        return 0;
    }

    @Override
    protected boolean isReceiveMessage(Intent intent) {
        int type = intent.getIntExtra(IvyMessages.PARAMETER_MESSAGE_TYPE, 0);
        String keyString = (String)intent.getExtras().get(IvyMessages.PARAMETER_MESSAGE_PERSON);

        // make sure the message is chat with mPerson
        if (keyString == null ||    // for failed message
            mPerson != null && keyString.compareTo(PersonManager.getPersonKey(mPerson)) == 0) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean needToScrollListView(Intent intent) {
        int type = intent.getIntExtra(IvyMessages.PARAMETER_MESSAGE_TYPE, 0);
        if (type != IvyMessages.VALUE_MESSAGETYPE_DELETE && type != IvyMessages.VALUE_MESSAGETYPE_RECOVER) {
            return true;
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    protected void changeData() {
        if (mImManager == null) {
            return;
        }

        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mImManager.getPersonHistoryMessage(mPerson);
        if (mSDKVersion < mMinSDK) {
            mAdapter.changeCursor(mCursor);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter.swapCursor(mCursor);
        }
    }

    @Override
    protected boolean onImServiceConnected() {
        return updatePersonState();        
    }

    @Override
    protected void recoverMessage() {
        if (mImManager != null && mPerson != null) {
            mImManager.recoverMessage(mPerson, mMessage.mType, mMessage.mContent, 
                    mMessage.mDirect == Table_Message.DIRECT_LOCALUSER, mMessage.mTime, mMessage.mState);
        }
    }

    @Override
    protected void deleteMesssages() {
        if (mImManager != null && mPerson != null) {
            mImManager.deleteMessage(mPerson);
            UserTrace.addTrace(UserTrace.ACTION_DELETE_CHAT);
        }
    }
}
