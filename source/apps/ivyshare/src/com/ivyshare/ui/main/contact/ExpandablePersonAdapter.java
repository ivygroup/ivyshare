package com.ivyshare.ui.main.contact;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.control.PersonManager;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.ui.chat.chat.ChatActivity;
import com.ivyshare.ui.chat.groupchat.GroupChatActivity;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.StringUtils;

/**
 * 
 * @author b456
 * 
 */
public class ExpandablePersonAdapter extends BaseExpandableListAdapter
		implements OnClickListener {
	private static final String TAG = "ExpandablePersionAdapter";
	private Context mContext;
	private List<Person> mListPersons;
	private List<Person> mListOfflinePersons;
	private List<Person> mListOnlinePersons;
	private List<Person> mListGroupPersons;
	private ImManager mImManager = null;

	// private static final int TYPE_DIVIDER_ONLINE = 0;
	private static final int TYPE_LIST_ONLINE = 1;
	// private static final int TYPE_DIVIDER_ALL = 2;
	private static final int TYPE_LIST_OFFLINE = 3;
	private static final int TYPE_LIST_GROUP = 4;

	private String[] mGroupData;
	private List<List<Person>> mChildData = new ArrayList<List<Person>>();

	public ExpandablePersonAdapter(Context context, List<Person> listPersons,
			ImManager imManager) {
		Log.d(TAG, "ExpandablePersionAdapter construct");
		mContext = context;
		mListOnlinePersons = new ArrayList<Person>();
		mListOfflinePersons = new ArrayList<Person>();
		mListGroupPersons = new ArrayList<Person>();
		mImManager = imManager;

		mListPersons = listPersons;
		getOnOfflinePersonList();
		initGroupPersons();
		initData();
	}

	private void getOnOfflinePersonList() {
		mListOnlinePersons.clear();
		mListOfflinePersons.clear();
		int nSize = mListPersons.size();
		for (int i = 0; i < nSize; i++) {
			Person person = mListPersons.get(i);
			if (person.isOnline()) {
				mListOnlinePersons.add(person);
			}  else {
				mListOfflinePersons.add(person);
			}
		}
	}

	private void initGroupPersons() {
	    Person p = new Person();
	    p.mIsFakePerson = true;
	    p.mNickName = MyApplication.getInstance().getString(R.string.group_chat_broadcastname);
	    p.mImage = null;
	    mListGroupPersons.add(p);
	}

	public int getPersonCount() {
	    int groupSize = mListGroupPersons.size();
		if (mListPersons != null) {
			return mListPersons.size() + groupSize;
		}
		return 0 + groupSize;
	}

	public void ChangeList(List<Person> listPersons) {
		mListPersons = listPersons;
		getOnOfflinePersonList();
		onDataChange();
	}

	/**
	 * the first data set
	 */
	private void initData() {
		// group data
		mGroupData = new String[] {
		        mContext.getString(R.string.group_chat_format),
				mContext.getString(R.string.online_person_format),
				mContext.getString(R.string.offline_person_format) };

		// child data
		onDataChange();
	}

	/**
	 * reset the child data
	 */
	private void onDataChange() {
		// child data
		mChildData.clear();
		mChildData.add(mListGroupPersons);
		mChildData.add(mListOnlinePersons);
		mChildData.add(mListOfflinePersons);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		if (mChildData == null)
			return null;

		return mChildData.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return 0;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View view, ViewGroup parent) {
		ViewClass myClass = null;
		if (view == null) {
			LayoutInflater factory = LayoutInflater.from(mContext);
			view = factory.inflate(R.layout.main_listview_persons, null);

			myClass = new ViewClass();
			myClass.statistic = (TextView) view
					.findViewById(R.id.person_statistic);
			myClass.personview = view.findViewById(R.id.main_persons_content);
			myClass.photoview = view.findViewById(R.id.layout_person_photo);
			myClass.image = (ImageView) view.findViewById(R.id.photo);
			myClass.name = (TextView) view.findViewById(R.id.main_persons_name);
			myClass.ip = (TextView) view.findViewById(R.id.main_persons_ip);
			myClass.state = (TextView) view.findViewById(R.id.main_persons_state);
			myClass.layout = (LinearLayout) view.findViewById(R.id.listitem);
			myClass.photo = (ImageView) view.findViewById(R.id.photo);
			myClass.layoutunread = (FrameLayout) view
					.findViewById(R.id.layoutunread);
			myClass.unread = (TextView) view.findViewById(R.id.unreadnum);
			myClass.layout.setOnClickListener(this);
			myClass.photo.setOnClickListener(this);

			view.setTag(myClass);
			myClass.photo.setTag(myClass);

		} else {
			myClass = (ViewClass) view.getTag();
		}

		myClass.statistic.setVisibility(View.GONE);
		myClass.personview.setVisibility(View.VISIBLE);
		myClass.photoview.setVisibility(View.VISIBLE);

		myClass.position = childPosition;
		Person person = mChildData.get(groupPosition).get(childPosition);
		switch (groupPosition) {
            case 0:
                myClass.type = TYPE_LIST_GROUP;
                break;
            case 1:
                myClass.type = TYPE_LIST_ONLINE;
                break;
            case 2:
                myClass.type = TYPE_LIST_OFFLINE;
                break;
            default:
                break;
        }

		if (!person.mIsFakePerson) {
		    showPerson(myClass, person);
		} else {
		    showGroupPerson(myClass, person);
		}
		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (mChildData == null)
			return 0;

		return mChildData.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mGroupData[groupPosition];
	}

	@Override
	public int getGroupCount() {
		return mGroupData.length;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return 0;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		GroupView holder = null;
		if (convertView == null) {
			holder = new GroupView();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.person_expand_item, null);
			holder.titleTextInfo = (TextView)convertView.findViewById(R.id.text_info);
			holder.titleTextNum = (TextView)convertView.findViewById(R.id.text_num);
			convertView.setTag(holder);
		} else {
			holder = (GroupView) convertView.getTag();
		}

		holder.titleTextInfo.setText(mGroupData[groupPosition]);
		if (groupPosition == 0) {
		    holder.titleTextNum.setVisibility(View.VISIBLE);
		    holder.titleTextNum.setText(String.format("[%1$d]", getChildrenCount(groupPosition)));
		} else {
		    holder.titleTextNum.setVisibility(View.VISIBLE);
		    holder.titleTextNum.setText(String.format("[%1$d/%2$d]", 
	                getChildrenCount(groupPosition), mListPersons.size()));    
		}
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	private void showPerson(ViewClass myClass, Person person) {
		if (person == null) {
			return;
		}

		UserIconEnvironment userIconEnvironment = LocalSetting.getInstance()
				.getUserIconEnvironment();
		if (person.mImage != null
				&& userIconEnvironment.isExistHead(person.mImage, -1)) {
			String headimagepath = userIconEnvironment
					.getFriendHeadFullPath(person.mImage);
			Bitmap bitmap = CommonUtils.DecodeBitmap(headimagepath, 256 * 256);
			if (bitmap != null) {
				myClass.image.setImageBitmap(bitmap);
			}
		} else {
			myClass.image.setImageResource(R.drawable.ic_contact_picture_holo_light);
		}

		myClass.name.setText(person.mNickName);
		if (person.mIP != null) {
			myClass.ip.setText(person.mIP.getHostAddress());
		}

		myClass.state.setVisibility(View.VISIBLE);
		myClass.state.setText(StringUtils.getStateString(person.getState()));

		myClass.layoutunread.setVisibility(View.GONE);

		if (person.isOnline()) {
			myClass.name.setTextColor(0xFF000000);
		} else {
			myClass.name.setTextColor(0xFFB0B0B0);
		}
	}
	
	private void showGroupPerson(ViewClass myClass, Person person) {
        if (person == null) {
            return;
        }

        myClass.image.setImageResource(R.drawable.broadcast);
        myClass.name.setText(person.mNickName);
        myClass.state.setVisibility(View.GONE);
        myClass.layoutunread.setVisibility(View.GONE);
        myClass.name.setTextColor(0xFF000000);
	}

	class ViewClass {
		TextView statistic;
		View personview;
		View photoview;
		ImageView image;
		TextView name;
		TextView ip;
		TextView state;
		LinearLayout layout;
		FrameLayout layoutunread;
		TextView unread;
		ImageView photo;
		int type;
		int position;
	}

	private final class GroupView {
		TextView titleTextInfo;
		TextView titleTextNum;
	}

	private String getKey(ViewClass myClass) {
		String key = null;
		if (myClass.type == TYPE_LIST_ONLINE &&
				myClass.position >= 0 && myClass.position < mListOnlinePersons.size()) {
			key = PersonManager.getPersonKey(mListOnlinePersons.get(myClass.position));
		} else if (myClass.type == TYPE_LIST_OFFLINE && 
				myClass.position >= 0 && myClass.position < mListOfflinePersons.size()) {
			key = PersonManager.getPersonKey(mListOfflinePersons.get(myClass.position));
		}
		return key;
	}
	@Override
	public void onClick(View v) {
		ViewClass myClass = (ViewClass) v.getTag();

		switch (v.getId()) {
			case R.id.listitem: {
			    if (myClass.type == TYPE_LIST_GROUP) {
			        Intent intent = new Intent();
                    intent.setClass(mContext, GroupChatActivity.class);
                    intent.putExtra("title", mListGroupPersons.get(myClass.position).mNickName);
                    mContext.startActivity(intent);
			    } else {
			        Intent intent = new Intent();
	                intent.setClass(mContext, ChatActivity.class);
	                String key = getKey(myClass);
	                if (key != null && mImManager != null
	                        && mImManager.getPerson(key) != null) {
	                    intent.putExtra("chatpersonKey", key);
	                    mContext.startActivity(intent);
	                }    
			    }
				break;
			}

			case R.id.photo: {
			    if (myClass.type == TYPE_LIST_GROUP) {
			        // TODO:
			    } else {
			        Intent intent = new Intent();
			        String key = getKey(myClass);
			        if (key != null && mImManager != null
			                && mImManager.getPerson(key) != null) {
			            intent.putExtra("chatpersonKey", key);
			            intent.setClass(mContext, QuickPersonInfoActivity.class);
			            mContext.startActivity(intent);
			        }
			    }
				break;
			}
		}
	}
}
