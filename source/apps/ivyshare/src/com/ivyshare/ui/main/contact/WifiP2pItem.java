package com.ivyshare.ui.main.contact;

import java.net.InetAddress;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ivy.ivyengine.connection.NetworkManager;
import com.ivy.ivyengine.connection.PeerInfo;
import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.control.PersonManager;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.ui.chat.chat.ChatActivity;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.StringUtils;

public class WifiP2pItem extends ContactDataBase {
    private PeerInfo mPeerInfo;
    private Person mPerson;
    
    public WifiP2pItem(Context context, ImManager imManager, NetworkManager networkManager, PeerInfo peerInfo) {
        super(context, imManager, networkManager);
        mPeerInfo = peerInfo;
        mPerson = null;
    }

    public PeerInfo getPeerInfo() {
        return mPeerInfo;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return VIEWTYPE_WIFIP2P_SECONDTEXT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int positionType = getItemViewType(position);

        //
        if (positionType == VIEWTYPE_WIFIP2P_SECONDTEXT) {
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

            myClass.mWifiP2pItem = this;
            myClass.mImage.setTag(myClass);
            myClass.mLayout.setTag(myClass);

            myClass.mPersonview.setVisibility(View.VISIBLE);
            myClass.mPhotoview.setVisibility(View.VISIBLE);

            showPerson(myClass);

            return convertView;
        }

        return null;
    }

    private void showPerson(MyViewPersonItem myClass) {
        int currentStatus = getCurrentStatus();
        switch (currentStatus) {
            case CURRENT_STATUS_NOTIVYDEVICE:
            {
                myClass.mImage.setImageResource(R.drawable.ic_contact_picture_holo_light);
                myClass.mName.setText(mPeerInfo.getFriendlyName());

                myClass.mState.setVisibility(View.VISIBLE);
                myClass.mState.setText(R.string.state_wifip2p_unknown);

                myClass.mFrameLayoutunread.setVisibility(View.GONE);
                myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_offline));
                break;
            }

            case CURRENT_STATUS_BUSY:
            {
                myClass.mImage.setImageResource(R.drawable.ic_contact_picture_holo_light);
                myClass.mName.setText(mPeerInfo.getFriendlyName());

                myClass.mState.setVisibility(View.VISIBLE);
                myClass.mState.setText(R.string.state_wifip2p_busy);

                myClass.mFrameLayoutunread.setVisibility(View.GONE);
                myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_offline));
                break;
            }

            case CURRENT_STATUS_ACTIVE_PERSON:
            {
                Person person = mPerson;
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
                if (mPerson.mIP != null) {
                    myClass.mIp.setText(person.mIP.getHostAddress());
                }

                myClass.mState.setVisibility(View.VISIBLE);
                myClass.mState.setText(StringUtils.getStateString(person.getState()));

                myClass.mFrameLayoutunread.setVisibility(View.GONE);

                if (person.isOnline()) {
                    myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_online));
                } else {
                    myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_offline));
                }

                break;
            }

            case CURRENT_STATUS_ACTIVE_NOPERSON:
            {
                myClass.mImage.setImageResource(R.drawable.ic_contact_picture_holo_light);
                myClass.mName.setText(mPeerInfo.getFriendlyName());

                myClass.mState.setVisibility(View.VISIBLE);
                myClass.mState.setText(R.string.state_wifip2p_activebutnoperson);

                myClass.mFrameLayoutunread.setVisibility(View.GONE);
                myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_offline));
                break;
            }

            case CURRENT_STATUS_EXITING:
                // empty.
                break;

            case CURRENT_STATUS_DEACTIVE:
            {
                myClass.mImage.setImageResource(R.drawable.ic_contact_picture_holo_light);
                myClass.mName.setText(mPeerInfo.getFriendlyName());

                myClass.mState.setVisibility(View.VISIBLE);
                myClass.mState.setText(R.string.state_wifip2p_inactive);

                myClass.mFrameLayoutunread.setVisibility(View.GONE);
                myClass.mName.setTextColor(mContext.getResources().getColor(R.color.name_online));
                break;
            }

            default:
                break;
        }
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.listitem: {
                MyViewPersonItem myitem = (MyViewPersonItem)v.getTag();
                int currentStatus = myitem.mWifiP2pItem.getCurrentStatus();

                switch (currentStatus) {
                    case CURRENT_STATUS_NOTIVYDEVICE:
                    {
                        Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(mContext)
                            .setTitle(R.string.contact_listitem_wifip2p_notivydevice_title)
                            .setMessage(R.string.contact_listitem_wifip2p_notivydevice_message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.ok,null)
                            .setNegativeButton(null, null).create();
                        alertDialog.show();
                        break;
                    }

                    case CURRENT_STATUS_BUSY:
                    {
                        if (mNetworkManager != null) {
                            mNetworkManager.cancelConnectWifiP2p();
                            deactive();
                        }
                        break;
                    }

                    case CURRENT_STATUS_ACTIVE_PERSON:
                    {
                        Intent intent = new Intent();
                        intent.setClass(myitem.mWifiP2pItem.mContext, ChatActivity.class);
                        String key = PersonManager.getPersonKey(myitem.mWifiP2pItem.mPerson);
                        if (key != null) {
                            intent.putExtra("chatpersonKey", key);
                            myitem.mWifiP2pItem.mContext.startActivity(intent);
                        }
                        break;
                    }

                    case CURRENT_STATUS_ACTIVE_NOPERSON:
                    {
                        break;
                    }

                    case CURRENT_STATUS_EXITING:
                    {
                        break;
                    }

                    case CURRENT_STATUS_DEACTIVE:
                    {
                        if (mNetworkManager != null) {
                            mNetworkManager.connectToWifiP2pPeer(myitem.mWifiP2pItem.mPeerInfo.getID());
                        }
                        break;
                    }
                }
                

                return;
            }

            case R.id.photo: {
                int currentStatus = getCurrentStatus();
                if (currentStatus == CURRENT_STATUS_ACTIVE_PERSON) {
                    Intent intent = new Intent();
                    Person person = (Person)v.getTag();
                    String key = PersonManager.getPersonKey(person);
                    if (key != null) {
                        intent.putExtra("chatpersonKey", key);
                        intent.setClass(mContext, QuickPersonInfoActivity.class);
                        mContext.startActivity(intent);
                    }
                }

                return;
            }
        }

        super.onClick(v);
    }

    @Override
    public void active(List<Person> persons) {
        super.active(persons);

        InetAddress myselfIP = mNetworkManager.getMySelfIpOfWifiP2p();
        int myselfMask = mNetworkManager.getNetMaskOfWifiP2p();

        for (Person person : mListPersons) {
            if (isSameNetRange(myselfMask, myselfIP, person.mIP)) {
                mPerson = person;
                break;
            }
        }
    }


    private static final int CURRENT_STATUS_NOTIVYDEVICE = 0;
    private static final int CURRENT_STATUS_BUSY = 1;
    private static final int CURRENT_STATUS_ACTIVE_PERSON = 2;
    private static final int CURRENT_STATUS_ACTIVE_NOPERSON = 3;
    private static final int CURRENT_STATUS_EXITING = 4;
    private static final int CURRENT_STATUS_DEACTIVE = 5;

    private int getCurrentStatus() {
        if (!mPeerInfo.isIvyDevice()) {
            return CURRENT_STATUS_NOTIVYDEVICE;
        }

        if (isBusying()) {
            return CURRENT_STATUS_BUSY;
        } else if (isActive()) {
            if (mPerson != null) {
                return CURRENT_STATUS_ACTIVE_PERSON;
            } else {
                return CURRENT_STATUS_ACTIVE_NOPERSON;
            }

        } else if (isExiting()) {
            return CURRENT_STATUS_EXITING;
        } else if (isDeactive()) {
            return CURRENT_STATUS_DEACTIVE;
        }

        return CURRENT_STATUS_NOTIVYDEVICE;
    }


    private static class MyViewPersonItem {
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

        FrameLayout mDivider;

        WifiP2pItem mWifiP2pItem;
    }
    
    
    private static boolean isSameNetRange(int netMask, InetAddress ip1, InetAddress ip2) {
        if (ip1 == null || ip2 == null) {
            return false;
        }

        int theIp1 = convertIp(ip1);
        int theIp2 = convertIp(ip2);

        if ((theIp1 & netMask) == (theIp2 & netMask)) {
            return true;
        } else {
            return false;
        }
    }
    
    private static int convertIp(InetAddress ip) {
        byte tmp[] = ip.getAddress();
        int theIp = 0;
        for (int i = 0; i < tmp.length; ++ i) {
            theIp = theIp << i + tmp[i];
        }
        return theIp;
    }
}
