package com.ivyshare.ui.main.contact;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.connection.NetworkManager;
import com.ivy.ivyengine.connection.implement.AccessPointInfo;
import com.ivy.ivyengine.control.ImManager;
import com.ivyshare.R;
import com.ivyshare.widget.SimpleImageButton;


//  A 的房间
//      进入
//  B 的房间           -- activePointMin
//      Person 1
//      Person 2
//      Person 3
//  C 的房间           -- activePointMax
//      进入
//  D 的房间
//      进入


public class IvyRoomData extends ContactDataBase {
    private static final String TAG = "IvyRoomData";

    private List<AccessPointInfo> mAccessPointInfos;
    private int mActiveAccessPointIndex;


    public IvyRoomData(Context context, ImManager imManager, NetworkManager networkManager) {
        super(context, imManager, networkManager);
        mAccessPointInfos = new LinkedList<AccessPointInfo>();
        mActiveAccessPointIndex = -1;
    }


    public void setPointList(List<AccessPointInfo> list) {
        // 0
        if (list == null) {
            deactive();
            mActiveAccessPointIndex = -1;
            mAccessPointInfos.clear();
            return;
        }

        Log.d(TAG, "setPointList called. size = " + list.size() + ", old size = " + mAccessPointInfos.size());

        if (isEqualList(mAccessPointInfos, list)) {
            return;
        }

        // 1 save old and clear.
        AccessPointInfo activepoint = getActivePoint();
        mAccessPointInfos.clear();
        mActiveAccessPointIndex = -1;

        // 2 add new points.
        for (AccessPointInfo info: list) {
            mAccessPointInfos.add(info);
        }

        // 3 find active point.
        if (activepoint != null) {
            for (int i = 0; i < mAccessPointInfos.size(); ++ i) {
                AccessPointInfo info = mAccessPointInfos.get(i);
                if (info.getSSID().equals(activepoint.getSSID())) {
                    mActiveAccessPointIndex = i;
                }
            }
        }
    }

    public boolean setActiveAccessPoint(AccessPointInfo info) {
        if (info == null) {
            mActiveAccessPointIndex = -1;
            return false;
        }
        if (mAccessPointInfos == null) {
            mActiveAccessPointIndex = -1;
            return false;
        }

        // Log.d(TAG, "setActiveAccessPoint called. old size = " + mAccessPointInfos.size());

        for (int i = 0; i < mAccessPointInfos.size(); ++i) {
            AccessPointInfo tmp = mAccessPointInfos.get(i);
            if (tmp.getSSID().equals(info.getSSID())) {
                mActiveAccessPointIndex = i;
                return true;
            }
        }

        mAccessPointInfos.add(info);
        mActiveAccessPointIndex = mAccessPointInfos.size() - 1;
        return false;
    }


    @Override
    public int getCount() {
        int itemCount = 0;
        if (mAccessPointInfos != null) {
            itemCount = mAccessPointInfos.size();
        }
        int personCount = 0;
        if (mListPersons != null) {
            personCount = mListPersons.size();
        }

        if (itemCount == 0) {
            return 0;
        }

        if (!isActive()) {
            return itemCount * 2;
        }

        if (personCount == 0) {
            return itemCount * 2;
        } else {
            return itemCount * 2 - 1 + personCount;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (!isActive() || mActiveAccessPointIndex < 0 || mActiveAccessPointIndex >= mAccessPointInfos.size()) {
            if (position % 2 == 0) {
                return VIEWTYPE_IVYROOM_ITEM;
            } else {
                return VIEWTYPE_IVYROOM_ENTRY;
            }
        } else {
            int personCount = 0;
            if (mListPersons != null && isActive()) {
                personCount = mListPersons.size();
            }
            int activePointMin = getActivePointMin();
            int activePointMax = getActivePointMax();

            // Log.d(TAG, "activePointMin = " + activePointMin + ", activePointMax = " + activePointMax);
            
            if (position < activePointMin) {
                if (position % 2 == 0) {
                    return VIEWTYPE_IVYROOM_ITEM;
                } else {
                    return VIEWTYPE_IVYROOM_ENTRY;
                }
            } else if (position == activePointMin) {
                return VIEWTYPE_IVYROOM_ITEM;
            } else if (position > activePointMin && position < activePointMax) {
                if (personCount == 0) {
                    if (isBusying() && position == activePointMin + 1) {
                        return VIEWTYPE_IVYROOM_ENTRY;
                    } else if (isActive()) {
                        return VIEWTYPE_IVYROOM_ENTRY;    
                    } else {
                        return VIEWTYPE_IVYROOM_NOPERSONTEXT;
                    }
                }
                return VIEWTYPE_PERSON;
            } else {
                if ((position-activePointMax) % 2 == 0) {
                    return VIEWTYPE_IVYROOM_ITEM;
                } else {
                    return VIEWTYPE_IVYROOM_ENTRY;
                }
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int positionType = getItemViewType(position);

        int activePointMin = getActivePointMin();
        int activePointMax = getActivePointMax();
        if (positionType == VIEWTYPE_PERSON) {
            return super.innerGetView(position - activePointMin - 1, convertView, parent
                                        ,(activePointMax-1 == position));
        }

        //
        if (positionType == VIEWTYPE_IVYROOM_ITEM) {
            MyViewItem item = null;
            if (convertView == null) {
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_ivyroom_item, null);
                item = new MyViewItem();
                item.mTextView = (TextView)convertView.findViewById(R.id.room_item);
                item.mImage = (SimpleImageButton)convertView.findViewById(R.id.btn_right);
                item.mImage.setOnClickListener(this);
                item.mImage.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						Toast msg = Toast.makeText(mContext, R.string.group_chat_broadcastname, Toast.LENGTH_SHORT);
						msg.setGravity(Gravity.TOP, msg.getXOffset(), msg.getYOffset() * 2);
						msg.show();
						return true;
					}
                	
                });

                convertView.setTag(item);
            } else {
                item = (MyViewItem)convertView.getTag();
            }
            item.mAccessPointInfo = getAccessPointInfoByPosition(position);
            
            if(isActive() && activePointMin == position ){
                item.mImage.setVisibility(View.VISIBLE);
            }else{
                item.mImage.setVisibility(View.GONE);
            }

            item.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_item, item.mAccessPointInfo.getFriendlyName()));
            return convertView;

        } else if (positionType == VIEWTYPE_IVYROOM_ENTRY) {
            MyViewEntry entry = null;
            if (convertView == null) {
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_ivyroom_entry, null);
                entry = new MyViewEntry();
                entry.mTextView = (TextView)convertView.findViewById(R.id.text);
                entry.mLayout = (LinearLayout)convertView.findViewById(R.id.list_contact_item_ivyroom_entry_layout);
                entry.mLayout.setOnClickListener(this);

                convertView.setTag(entry);
            } else {
                entry = (MyViewEntry)convertView.getTag();
            }
            entry.mAccessPointInfo = getAccessPointInfoByPosition(position);

            Log.d(TAG, "isbusy = " + isBusying() + ", position = " + position + ", activepointmin = " + activePointMin);
            if (isBusying() && ((activePointMin != -1) && (position == activePointMin + 1))) {
                disableItemOnUI(entry.mLayout, entry.mTextView);
                entry.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_entrying,
                        entry.mAccessPointInfo.getFriendlyName()));
            } else {
                entry.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_entry, entry.mAccessPointInfo.getFriendlyName()));
                enableItemOnUI(entry.mLayout, entry.mTextView);
            }

            return convertView;

        } else if (positionType == VIEWTYPE_IVYROOM_NOPERSONTEXT) {
            MyViewEntry nopersontext = null;
            if (convertView == null) {
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_ivyroom_entry, null);
                nopersontext = new MyViewEntry();
                nopersontext.mTextView = (TextView)convertView.findViewById(R.id.text);

                convertView.setTag(nopersontext);
            } else {
                nopersontext = (MyViewEntry)convertView.getTag();
            }
            nopersontext.mTextView.setText(R.string.contact_listitem_ivyroom_nopersontext);
            return convertView;
        } else {
            return null;
        }
    }
    
    private AccessPointInfo getAccessPointInfoByPosition(int position) {
        int index = 0;
        if (!isActive() || mActiveAccessPointIndex < 0 || mActiveAccessPointIndex >= mAccessPointInfos.size()) {
            index = position / 2;
        } else {
            int activePointMin = getActivePointMin();
            int activePointMax = getActivePointMax();

            if (position < activePointMin) {
                index = position / 2;
            } else if (position >= activePointMin && position < activePointMax) {
                index = mActiveAccessPointIndex;
            } else {
                index = (position - activePointMax) / 2 + mActiveAccessPointIndex + 1;
            }
        }

        // Log.d(TAG, "position = " + position +", size = " + mAccessPointInfos.size() + ", index = " + index);
        
        return mAccessPointInfos.get(index);
    }

    private AccessPointInfo getActivePoint() {
        AccessPointInfo activepoint = null;
        if (mActiveAccessPointIndex >=0 && mActiveAccessPointIndex < mAccessPointInfos.size()) {
            activepoint = mAccessPointInfos.get(mActiveAccessPointIndex);
        }
        return activepoint;
    }

    private int getActivePointMin() {
        if (mActiveAccessPointIndex < 0 || mActiveAccessPointIndex >= mAccessPointInfos.size()) {
            return -1;
        }
        return mActiveAccessPointIndex * 2;
    }

    private int getActivePointMax() {
        if (mActiveAccessPointIndex < 0 || mActiveAccessPointIndex >= mAccessPointInfos.size()) {
            return -1;
        }
        
        int personCount = 0;
        if (mListPersons != null) {
            personCount = mListPersons.size();
        }
        if (personCount == 0) {
            return getActivePointMin() + 1 + 1; // min + noactivetext + skip
        } else {
            return getActivePointMin() + personCount + 1;   // min + persons + skip
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_contact_item_ivyroom_entry_layout: {
                {
                	/*
                    MyViewEntry myClass = (MyViewEntry) v.getTag();
                    IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();
                    if (ivyNetService != null) {
                        if (ivyNetService.getConnectionInfo().isBusying()) {
                            return;
                        }

                        if (mImService != null) {
                            mImService.downLine();
                        }
                        setActiveAccessPoint(myClass.mAccessPointInfo);
                        ConnectionState state = ivyNetService.getConnectionInfo().getConnectionState();
                        if (state == ConnectionState.IVY_HOTSPOT_ENABLED) {
                            // IvyMessages.sendNetworkStateChange(ConnectionState.IVY_HOTSPOT_DISABLING);
                            ivyNetService.disableHotspot();
                        } else {
                            // IvyMessages.sendNetworkStateChange(ConnectionState.IVY_WIFI_CONNECTING);
                            ivyNetService.disconnectFromIvyNetwork();
                        }

                        ivyNetService.connectIvyNetwork(myClass.mAccessPointInfo.getSSID());
                        IvyMessages.sendNetworkStateChange(ConnectionState.IVY_WIFI_CONNECTING, myClass.mAccessPointInfo.getSSID());
                    }
                    return;*/
                }
            }
        }

        super.onClick(v);
    }

    @Override
    public void deactive() {
        if (isBusying()) {
            return;
        }

        super.deactive();
    }


    private class MyViewItem {
        public TextView mTextView;
        public AccessPointInfo mAccessPointInfo;
        public SimpleImageButton mImage;
    }

    private class MyViewEntry {
        public LinearLayout mLayout;
        public TextView mTextView;
        public AccessPointInfo mAccessPointInfo;
    }
    
    private boolean isEqualList(List<AccessPointInfo> list1, List<AccessPointInfo> list2) {
        if (list1 == null || list2 == null) {
            return false;
        }

        if (list1.size() != list2.size()) {
            return false;
        }
        
        Map<String, AccessPointInfo> map1 = new HashMap<String, AccessPointInfo>();
        for (AccessPointInfo info: list1) {
            map1.put(info.getSSID(), info);
        }
        
        for (AccessPointInfo info: list2) {
            if (!map1.containsKey(info.getSSID())) {
                return false;
            }
        }

        return true;
    }
}
