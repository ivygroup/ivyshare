package com.ivyshare.ui.main;

import java.sql.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.control.PersonManager;
import com.ivy.ivyengine.control.SessionMessages;
import com.ivy.ivyengine.control.SessionMessages.SessionMessage;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivyshare.R;
import com.ivyshare.ui.chat.chat.ChatActivity;
import com.ivyshare.ui.chat.groupchat.GroupChatActivity;
import com.ivyshare.util.CommonUtils;

public class SessionAdapter extends BaseAdapter implements OnClickListener{
	private static final String TAG = SessionAdapter.class.getSimpleName();

	private Context mContext;
	private List<SessionMessage> mListSessionMessages;
	private ImManager mImManager = null;

	public SessionAdapter(Context context, List<SessionMessage> list, ImManager imManager) {
		mContext = context;
		mListSessionMessages = list;
		mImManager = imManager;
	}

	public void ChangeList(List<SessionMessage> list) {
		mListSessionMessages = list;
	}

	@Override
	public int getCount() {
		return mListSessionMessages.size();
	}

	public int getUnReadCount() {
		int nSize = mListSessionMessages.size();
		int nCount = 0;
		for (int i=0; i<nSize; i++) {
			SessionMessage sessionMessage = mListSessionMessages.get(i);
			if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_GROUPCHAT) {
				nCount += sessionMessage.mUnReadGroupCount;
			} else if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_CHAT){
				nCount += sessionMessage.mPerson.mDynamicStatus.unReadMsgCount;
			}
		}
		return nCount;
	}

	@Override
	public Object getItem(int position) {
		return mListSessionMessages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position < 0 || position >= getCount()) {
			return null;
		}

		ViewClass myClass = null;
		if(convertView == null) {
			LayoutInflater factory = LayoutInflater.from(mContext);
			convertView = factory.inflate(R.layout.list_session_item , null);

            myClass = new ViewClass();
            myClass.image = (ImageView)convertView.findViewById(R.id.contact_photo_default);
            myClass.name = (TextView)convertView.findViewById(R.id.contact_name);
            myClass.message = (TextView)convertView.findViewById(R.id.message_content);
            myClass.file = (ImageView)convertView.findViewById(R.id.file_content);
            myClass.time = (TextView)convertView.findViewById(R.id.message_time);
            myClass.viewunread = (View)convertView.findViewById(R.id.layoutunread);
            myClass.unread = (TextView)convertView.findViewById(R.id.unreadnum);
            myClass.layout = (LinearLayout)convertView.findViewById(R.id.session_listitem);
            myClass.photo = (ImageView)convertView.findViewById(R.id.contact_photo_default);
			myClass.layout.setOnClickListener((OnClickListener) this);
			myClass.photo.setOnClickListener((OnClickListener) this);

			myClass.layout.setTag(myClass);
			myClass.photo.setTag(myClass);
		} else {
			myClass = (ViewClass)convertView.getTag();
		}

		myClass.position = position;

		SessionMessage sessionMessage = mListSessionMessages.get(position);

		long time = 0;
		int unReadCount = 0;
		FileType type;
		String content;
		if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_GROUPCHAT) {
			myClass.image.setImageResource(R.drawable.broadcast);
		    myClass.name.setText(R.string.group_chat_broadcastname);

		    time = sessionMessage.mGroupMessage.mTime;
		    unReadCount = sessionMessage.mUnReadGroupCount;
		    type = sessionMessage.mGroupMessage.mType;
		    content = sessionMessage.mGroupMessage.mContent;
		} else if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_FREESHARE) {
			myClass.image.setImageResource(R.drawable.ic_share_uri);
		    myClass.name.setText(R.string.tab_freeshare);

		    time = sessionMessage.mFreeShareMessage.mTime;
		    unReadCount = 0;
		    type = sessionMessage.mFreeShareMessage.mType;
		    content = sessionMessage.mFreeShareMessage.mContent;
		} else {
		    UserIconEnvironment userIconEnvironment = LocalSetting.getInstance().getUserIconEnvironment();
		    if (sessionMessage.mPerson.mImage != null && userIconEnvironment.isExistHead(sessionMessage.mPerson.mImage, -1)) {
		        String headimagepath = userIconEnvironment.getFriendHeadFullPath(sessionMessage.mPerson.mImage);
				Bitmap bitmap = CommonUtils.DecodeBitmap(headimagepath, 256*256);
				if (bitmap != null) {
					myClass.image.setImageBitmap(bitmap);
				}
		    } else {
		        myClass.image.setImageResource(R.drawable.ic_contact_picture_holo_light);
		    }
		    myClass.name.setText(sessionMessage.mPerson.mNickName);
	        Resources resource = mContext.getResources(); 
	        if (sessionMessage.mPerson.isOnline()) {
	        	myClass.name.setTextColor(resource.getColor(R.color.list_main));
	        } else {
	        	myClass.name.setTextColor(resource.getColor(R.color.list_secondray));
	        }

		    time = sessionMessage.mMessage.mTime;
		    unReadCount = sessionMessage.mPerson.mDynamicStatus.unReadMsgCount;
		    type = sessionMessage.mMessage.mType;
		    content = sessionMessage.mMessage.mContent;
		}

	    Date today = new Date(System.currentTimeMillis());
	    Date messageDay = new Date(time);
	    if (today.getYear() == messageDay.getYear() &&
	    		today.getMonth() == messageDay.getMonth() &&
	    		today.getDate() == messageDay.getDate()) {
			myClass.time.setText(DateFormat.format("kk:mm", time).toString());
	    } else {
		    myClass.time.setText(DateFormat.format("MM-dd kk:mm", time).toString());
	    }

		if (unReadCount > 0) {
	        myClass.viewunread.setVisibility(View.VISIBLE);
	        myClass.unread.setText(String.valueOf(unReadCount));
	    } else {
	        myClass.viewunread.setVisibility(View.INVISIBLE);
	    }

		myClass.message.setVisibility(View.GONE);
		myClass.file.setVisibility(View.VISIBLE);
		switch (type) {
		case FileType_CommonMsg:
			myClass.message.setVisibility(View.VISIBLE);
			myClass.file.setVisibility(View.GONE);
			myClass.message.setText(content);
			break;
		case FileType_App:
			myClass.file.setImageResource(R.drawable.ic_file_type_apk);
			break;
		case FileType_Contact:
			myClass.file.setImageResource(R.drawable.ic_file_type_vcard);
			break;
		case FileType_Picture:
			myClass.file.setImageResource(R.drawable.ic_file_type_image);
			break;
		case FileType_Music:
			myClass.file.setImageResource(R.drawable.ic_file_type_music);
			break;
		case FileType_Video:
			myClass.file.setImageResource(R.drawable.ic_file_type_video);
			break;
		case FileType_OtherFile:
			myClass.file.setImageResource(R.drawable.ic_file_type_other_file);
			break;
		case FileType_Record:
			myClass.file.setImageResource(R.drawable.record_voice_left);
			break;
		}
		return convertView;
	}

	class ViewClass {
		ImageView image;
		ImageView file;
		TextView name;
		TextView time;
		TextView message;
		View viewunread;
		TextView unread;
		LinearLayout layout;
		ImageView photo;
		int position;
	}

	@Override
	public void onClick(View v) {
		ViewClass myClass = (ViewClass)v.getTag();
		switch (v.getId()) {
		case R.id.session_listitem:
			startTargetActivity(myClass, ChatActivity.class);
			break;

		case R.id.contact_photo_default:
			startTargetActivity(myClass, QuickPersonInfoActivity.class);
			break;
		}
	}

	private void startTargetActivity(ViewClass myClass, Class targetActivity) {
		SessionMessage sessionMessage = mListSessionMessages.get(myClass.position);
		if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_GROUPCHAT) {
			Intent intentNew1 = new Intent(mContext, GroupChatActivity.class);
			intentNew1.putExtra("isBroadCast", sessionMessage.mGroupMessage.mIsBroadCast);
			intentNew1.putExtra("groupName", sessionMessage.mGroupMessage.mGroupName);
			mContext.startActivity(intentNew1);
		} else if (sessionMessage.mSessionType == SessionMessages.SESSION_TYPE_FREESHARE) {
			Intent intent = new Intent();
			intent.setClass(mContext, FreeShareHistoryActivity.class);
			mContext.startActivity(intent);
		} else {
			String key = PersonManager.getPersonKey(mListSessionMessages
					.get(myClass.position).mPerson);
			if (key != null && mImManager != null
					&& mImManager.getPerson(key) != null) {
				Intent intent = new Intent();
				intent.putExtra("chatpersonKey", key);
				intent.setClass(mContext,targetActivity);
				mContext.startActivity(intent);
			}			
		}
	}
}
