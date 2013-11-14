package com.ivyshare.ui.chat.chat;

import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.ivyshare.R;
import com.ivyshare.engin.control.ChatMessage;
import com.ivyshare.engin.control.ImManager;
import com.ivyshare.engin.data.Table_Message;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.engin.im.Person;
import com.ivyshare.ui.chat.abstractchat.AdapterHelper;
import com.ivyshare.ui.chat.abstractchat.AdapterInterface;

public class ChatAdapter extends CursorAdapter {
    private static final String TAG = ChatAdapter.class.getSimpleName();
    private Person mPerson;
    private ChatAdapterHelper mAdapterHelper;
    private ImManager mImManager;

    public ChatAdapter(Person person, Context context, Cursor cursor,
            ImManager imManager, Map<Integer, Integer> process, Set<Integer> expand, ListView view) {
        super(context, cursor, false);

        mPerson = person;
        mImManager = imManager;
        mAdapterHelper = new ChatAdapterHelper(context, imManager, process, expand, view);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mAdapterHelper.newView();
    }

    @SuppressLint("NewApi")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!getCursor().moveToPosition(position)) {
            return null;
        }
        View v;
        if (convertView == null) {
            v = newView(mContext, getCursor(), parent);
        } else {
            if (mAdapterHelper.needNewView(convertView)) {
                v = newView(mContext, getCursor(), parent);
            } else {
                v = convertView;
            }
        }
        bindView(v, mContext, getCursor());
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ChatMessage message = new ChatMessage();
        message.mId = cursor.getInt(cursor.getColumnIndex(Table_Message._ID));
        message.mState = cursor.getInt(cursor.getColumnIndex(Table_Message.STATE));
        message.mDirect = cursor.getInt(cursor.getColumnIndex(Table_Message.DIRECT));
        message.mType = FileType.values()[cursor.getInt(cursor.getColumnIndex(Table_Message.TYPE))];
        message.mContent = cursor.getString(cursor.getColumnIndex(Table_Message.CONTENT));
        message.mTime = cursor.getLong(cursor.getColumnIndex(Table_Message.TIME));

        mAdapterHelper.setViewTag(view, message);

        mAdapterHelper.initViewItem(view);
    }

    public AdapterInterface getAdapterInterface() {
        return mAdapterHelper;
    }

    public class ChatAdapterHelper extends AdapterHelper {
        public ChatAdapterHelper (Context context,
                ImManager imManager, Map<Integer, Integer> process, Set<Integer> expand, ListView view) {
            super(context, imManager, process, expand, view);
        }

        protected Person getRemotePerson(ChatMessage message) {
            return mPerson;
        }

        public void notifyDataSetChanged() {
            ChatAdapter.this.notifyDataSetChanged();
        }

        @Override
        protected int deleteMessage(ChatMessage message) {
            if (mImManager != null && mPerson != null) {
                mImManager.deleteMessage(mPerson, message.mId);
                return 0;
            }
            return -1;
        }

        @Override
        protected int resendMesssage(ChatMessage message) {

			if (mImManager == null || mPerson == null) {
				return -1;
			}
			if (!mPerson.isOnline()) {
				String information = String.format(
						mContext.getString(R.string.could_not_send_message),
						mPerson.mNickName);
				Toast.makeText(mContext, information, Toast.LENGTH_SHORT).show();
				return -2;
			}

            if (mImManager != null && mPerson != null && mPerson.isOnline()) {
                deleteMessage(message);
                if (message.mType == FileType.FileType_CommonMsg) {
                    mImManager.sendMessage(mPerson, message.mContent);
                } else {
                    mImManager.sendFile(mPerson, "", message.mContent, message.mType);
                }
                return 0;
            }
            return -1;
        }

        @Override
        public int getCount() {
            return ChatAdapter.this.getCount();
        }
    }
}
