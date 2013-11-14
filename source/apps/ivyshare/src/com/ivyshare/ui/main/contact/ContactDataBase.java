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
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.engin.connection.NetworkManager;
import com.ivyshare.engin.control.ImManager;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.engin.control.LocalSetting.UserIconEnvironment;
import com.ivyshare.engin.control.PersonManager;
import com.ivyshare.engin.im.Person;
import com.ivyshare.ui.chat.chat.ChatActivity;
import com.ivyshare.ui.chat.groupchat.GroupChatActivity;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.widget.SimplePopMenu;


public abstract class ContactDataBase extends BaseAdapter implements AdapterState, OnClickListener {
    private static final String TAG = "CmontactDataBase";
    protected Context mContext;
    private int mAdapterState;
    protected List<Person> mListPersons;
    protected ImManager mImManager;
    protected NetworkManager mNetworkManager;
    
    SimplePopMenu mPopMenu;
    
    protected static final int VIEWTYPE_HOTSPOT_TITLE = 0;
    protected static final int VIEWTYPE_HOTSPOT_NOTACTIVE = 1;        // This line is create and exit item. should change the text.
    protected static final int VIEWTYPE_IVYROOM_ITEM = 2;
    protected static final int VIEWTYPE_IVYROOM_ENTRY = 3;
    protected static final int VIEWTYPE_IVYROOM_NOPERSONTEXT = 4;
    protected static final int VIEWTYPE_WIFIP2P_TITLE = 5;
    protected static final int VIEWTYPE_WIFIP2P_SECONDTEXT = 6;
    protected static final int VIEWTYPE_HALL_TITLE = 7;
    protected static final int VIEWTYPE_HALL_ENTRY = 8;
    protected static final int VIEWTYPE_PERSON = 9;
    public static final int VIEWTYPE_COUNT = 10;


    public ContactDataBase(Context context, ImManager imManager, NetworkManager networkManager) {
        mContext = context;
        mListPersons = new ArrayList<Person>();
        mImManager = imManager;
        mNetworkManager = networkManager;
        mAdapterState = AdapterState.STATE_DEACTIVE;
        
    }

    @Override
    public void active(List<Person> persons) {
        active(persons, null);
    }

    @Override
    public void busy() {
        busy(null);
    }

    @Override
    public void deactive() {
        deactive(null);
    }

    @Override
    public void exiting() {
        exiting(null);
    }

    @Override
    public void active(List<Person> persons, String id) {
        getOnlinePersonList(persons, mListPersons);
        mAdapterState = AdapterState.STATE_ACTIE;
    }

    @Override
    public void busy(String id) {
        mAdapterState = AdapterState.STATE_BUSY;
    }

    @Override
    public void deactive(String id) {
        mAdapterState = AdapterState.STATE_DEACTIVE;
    }

    @Override
    public void exiting(String id) {
    	mAdapterState = AdapterState.STATE_EXITING;
    }

    @Override
    public boolean isActive() {
        return mAdapterState == AdapterState.STATE_ACTIE;
    }

    @Override
    public boolean isDeactive() {
        return mAdapterState == AdapterState.STATE_DEACTIVE; 
    }

    @Override
    public boolean isBusying() {
        return mAdapterState == AdapterState.STATE_BUSY;
    }

    @Override
    public boolean isExiting() {
    	return mAdapterState == AdapterState.STATE_EXITING;
    }

    @Override
    public void setData(List<Person> persons) {
        getOnlinePersonList(persons, mListPersons);
    }


    //
    private void getOnlinePersonList(List<Person> lists, List<Person> outList) {
        outList.clear();

        if (lists == null) {
            return;
        }

        int nSize = lists.size();
        for (int i = 0; i < nSize; i++) {
            Person person = lists.get(i);
            if (person.isOnline()) {
                outList.add(person);
            }
        }
    }


    protected View innerGetView(int position, View convertView, ViewGroup parent,boolean isLast) {
        int pos = position;
        if (mListPersons == null) {
            return null;
        }

        if (pos >= mListPersons.size()) {
            return null;
        }

        if (!isActive()) {
            Log.e(TAG, "ERROR!!!!!  Please check the code");
            return null;
        }

        MyViewPersonItem myClass = null;
        if (convertView == null) {
            LayoutInflater factory = LayoutInflater.from(MyApplication.getInstance());
            convertView = factory.inflate(R.layout.list_contact_item_persons, null);

            myClass = new MyViewPersonItem();
            myClass.mLayout = (LinearLayout) convertView.findViewById(R.id.listitem);
            myClass.mListlayout = (LinearLayout) convertView.findViewById(R.id.listitemlayout);
            myClass.mPersonview = convertView.findViewById(R.id.main_persons_content);
            myClass.mName = (TextView) convertView.findViewById(R.id.main_persons_name);
            myClass.mIp = (TextView) convertView.findViewById(R.id.main_persons_ip);
            myClass.mState = (TextView) convertView.findViewById(R.id.main_persons_state);                    
            myClass.mPhotoview = convertView.findViewById(R.id.layout_person_photo);
            myClass.mImage = (ImageView) convertView.findViewById(R.id.photo);
            myClass.mFrameLayoutunread = (FrameLayout) convertView.findViewById(R.id.layoutunread);
            myClass.mUnreadNumber = (TextView) convertView.findViewById(R.id.unreadnum);
            myClass.mDivider = (FrameLayout)convertView.findViewById(R.id.item_divider);
            myClass.mLayout.setOnClickListener(this);
            myClass.mImage.setOnClickListener(this);
            convertView.setTag(myClass);
            
        } else {
            myClass = (MyViewPersonItem) convertView.getTag();
        }
        myClass.mPerson = mListPersons.get(pos);
        myClass.mImage.setTag(myClass.mPerson);

        myClass.mPersonview.setVisibility(View.VISIBLE);
        myClass.mPhotoview.setVisibility(View.VISIBLE);
        
        if(isLast)
            myClass.mDivider.setVisibility(View.GONE);
        else {
            myClass.mDivider.setVisibility(View.VISIBLE);
        }

        showPerson(myClass);
        return convertView;
    }
    

    // position is always base 0.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int pos = position;
        if (mListPersons == null) {
            return null;
        }

        if (pos >= mListPersons.size()) {
            return null;
        }

        if (!isActive()) {
            Log.e(TAG, "ERROR!!!!!  Please check the code");
            return null;
        }

        MyViewPersonItem myClass = null;
        if (convertView == null) {
            LayoutInflater factory = LayoutInflater.from(MyApplication.getInstance());
            convertView = factory.inflate(R.layout.list_contact_item_persons, null);

            myClass = new MyViewPersonItem();
            myClass.mLayout = (LinearLayout) convertView.findViewById(R.id.listitem);
            myClass.mListlayout = (LinearLayout) convertView.findViewById(R.id.listitemlayout);
            myClass.mPersonview = convertView.findViewById(R.id.main_persons_content);
            myClass.mName = (TextView) convertView.findViewById(R.id.main_persons_name);
            myClass.mIp = (TextView) convertView.findViewById(R.id.main_persons_ip);
            myClass.mState = (TextView) convertView.findViewById(R.id.main_persons_state);                    
            myClass.mPhotoview = convertView.findViewById(R.id.layout_person_photo);
            myClass.mImage = (ImageView) convertView.findViewById(R.id.photo);
            myClass.mFrameLayoutunread = (FrameLayout) convertView.findViewById(R.id.layoutunread);
            myClass.mUnreadNumber = (TextView) convertView.findViewById(R.id.unreadnum);
            myClass.mDivider = (FrameLayout)convertView.findViewById(R.id.item_divider);
            myClass.mLayout.setOnClickListener(this);
            myClass.mImage.setOnClickListener(this);
            convertView.setTag(myClass);
            
        } else {
            myClass = (MyViewPersonItem) convertView.getTag();
        }
        myClass.mPerson = mListPersons.get(pos);
        myClass.mImage.setTag(myClass.mPerson);

        myClass.mPersonview.setVisibility(View.VISIBLE);
        myClass.mPhotoview.setVisibility(View.VISIBLE);
        
        showPerson(myClass);
        return convertView;
    }

    //
    private void showPerson(MyViewPersonItem myClass) {
        if (myClass.mPerson == null) {
            return;
        }

        Person person = myClass.mPerson;
        UserIconEnvironment userIconEnvironment = LocalSetting.getInstance()
                .getUserIconEnvironment();
        if (person.mImage != null
                && userIconEnvironment.isExistHead(person.mImage, -1)) {
            String headimagepath = userIconEnvironment
                    .getFriendHeadFullPath(person.mImage);
            Bitmap bitmap = CommonUtils.DecodeBitmap(headimagepath, 256 * 256);
            if (bitmap != null) {
                myClass.mImage.setImageBitmap(bitmap);
            }
        } else {
            myClass.mImage.setImageResource(R.drawable.ic_contact_picture_holo_light);
        }

        myClass.mName.setText(person.mNickName);
        if (myClass.mPerson.mIP != null) {
            myClass.mIp.setText(person.mIP.getHostAddress());
        }

        myClass.mState.setVisibility(View.VISIBLE);
        myClass.mState.setText(person.getStateString());

        myClass.mFrameLayoutunread.setVisibility(View.GONE);

        if (person.isOnline()) {
            myClass.mName.setTextColor(0xFF000000);
        } else {
            myClass.mName.setTextColor(0xFFB0B0B0);
        }
    }


    private class MyViewPersonItem {
        LinearLayout mLayout;    //1
        LinearLayout mListlayout;//1

        View mPersonview;           // 2
        TextView mName;                 // 3
        TextView mIp;                   // 3
        TextView mState;                // 3

        View mPhotoview;            // 2
        ImageView mImage;               // 3
        FrameLayout mFrameLayoutunread; // 3
        TextView mUnreadNumber;         // 3

        Person mPerson;
        
        FrameLayout mDivider;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.listitem: {
                MyViewPersonItem myClass = (MyViewPersonItem) v.getTag();
                Intent intent = new Intent();
                intent.setClass(mContext, ChatActivity.class);
                String key = PersonManager.getPersonKey(myClass.mPerson);
                if (key != null) {
                    intent.putExtra("chatpersonKey", key);
                    mContext.startActivity(intent);
                }

                break;
            }

            case R.id.btn_right:{
				Intent intentNew1 = new Intent(mContext, GroupChatActivity.class);
				intentNew1.putExtra("isBroadCast", true);
				intentNew1.putExtra("groupName", "");
				mContext.startActivity(intentNew1);

                break;
            }

            case R.id.photo: {
                Intent intent = new Intent();
                Person person = (Person)v.getTag();
                String key = PersonManager.getPersonKey(person);
                if (key != null) {
                    intent.putExtra("chatpersonKey", key);
                    intent.setClass(mContext, QuickPersonInfoActivity.class);
                    mContext.startActivity(intent);
                }
                break;
            }
        }
        
    }

    protected void disableItemOnUI(LinearLayout layout, TextView textView) {
        textView.setTextColor(mContext.getResources().getColor(R.color.list_secondray));
        layout.setClickable(false);
        // layout.setBackgroundColor(mContext.getResources().getColor(android.R.color.transparent));
        layout.setBackgroundResource(R.drawable.list_selector_disable);
    }

    protected void enableItemOnUI(LinearLayout layout, TextView textView) {
        textView.setTextColor(mContext.getResources().getColor(R.color.list_main));
        layout.setClickable(true);
        layout.setBackgroundResource(R.drawable.list_selector);
    }
}
