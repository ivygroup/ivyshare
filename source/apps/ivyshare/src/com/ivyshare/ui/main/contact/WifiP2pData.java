package com.ivyshare.ui.main.contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ivyshare.R;
import com.ivyshare.engin.connection.NetworkManager;
import com.ivyshare.engin.connection.PeerInfo;
import com.ivyshare.engin.control.ImManager;
import com.ivyshare.engin.im.Person;
import com.ivyshare.widget.SimpleImageButton;

public class WifiP2pData extends ContactDataBase {
    private static final String TAG = "WifiP2pData";

    private List<WifiP2pItem> mItems;
    private Map<Integer, MyItem> mMapItemPosition;

    public WifiP2pData(Context context, ImManager imManager, NetworkManager networkManager) {
        super(context, imManager, networkManager);
        mItems = new LinkedList<WifiP2pItem>();
        mMapItemPosition = new HashMap<Integer, WifiP2pData.MyItem>();
    }

    public void setWifiP2pPeers(List<PeerInfo> list) {
        if (list == null || list.size() == 0) {
            mItems.clear();
            return;
        }

        if (isEqualPeers(mItems, list)) {
            return;
        }

        List<Boolean> itemHasExist = new ArrayList<Boolean>();
        for (int i = 0; i < mItems.size(); ++ i) {
            itemHasExist.add(false);
        }
        
        List<Boolean> newInfoExist = new ArrayList<Boolean>();
        for (int i = 0; i < list.size(); ++ i) {
            newInfoExist.add(false);
        }

        for (int i = 0; i < list.size(); ++ i) {
            PeerInfo newInfo = list.get(i);
            for (int j = 0; j < mItems.size(); ++j) {
                if (mItems.get(j).getPeerInfo().getID().equals(newInfo.getID())) {
                    itemHasExist.set(j, true);
                    newInfoExist.set(i, true);
                    break;
                }
            }
        }


        for (int i = itemHasExist.size() - 1; i >= 0; --i) {
            if (itemHasExist.get(i).equals(false)) {
                mItems.remove(i);
            }
        }

        for (int i = 0; i < newInfoExist.size(); ++i) {
            if (newInfoExist.get(i).equals(false)) {
                WifiP2pItem item = new WifiP2pItem(mContext, mImManager, mNetworkManager, list.get(i));
                mItems.add(item);
            }
        }
    }

    @Override
    public int getCount() {
        updateItemPositionMap();
        if (mMapItemPosition.size() > 0) {
            return 1 + mMapItemPosition.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int arg0) {
        if (arg0 == 0) {
            return null;
        } else {
            MyItem myItem = mMapItemPosition.get(arg0);
            return myItem.mItem.getItem(myItem.mNewPosition);
        }
    }

    @Override
    public long getItemId(int arg0) {
        if (arg0 == 0) {
            return 0;
        } else {
            MyItem myItem = mMapItemPosition.get(arg0);
            return myItem.mItem.getItemId(myItem.mNewPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEWTYPE_WIFIP2P_TITLE;
        } else {
            MyItem myItem = mMapItemPosition.get(position);
            return myItem.mItem.getItemViewType(myItem.mNewPosition);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            MyViewTitle title = null;
            if (convertView == null) {
                title = new MyViewTitle();
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_wifip2p_title, null);
                title.mTextView = (TextView)convertView.findViewById(R.id.title);
                title.mImage = (SimpleImageButton)convertView.findViewById(R.id.btn_right);
                convertView.setTag(title);

            } else {
                title = (MyViewTitle)convertView.getTag();
            }

            title.mImage.setOnClickListener(this);
            if (isActive()) {
                title.mImage.setVisibility(View.VISIBLE);
            } else {
                title.mImage.setVisibility(View.GONE);
            }
            title.mTextView.setText(mContext.getString(R.string.contact_listitem_wifip2p_title));

            return convertView;

        } else {
            MyItem myItem = mMapItemPosition.get(position);
            return myItem.mItem.getView(myItem.mNewPosition, convertView, parent);
        }
    }

    @Override
    public void active(List<Person> persons, String id) {
        if (id == null) {
            return;
        }

        WifiP2pItem item = getWifiP2pItemByID(id);
        if (item != null) {
            item.active(persons);
        } else {
            /*for (WifiP2pItem p2pItem: mItems) {
                p2pItem.deactive();
            }*/
        }
    }

    @Override
    public void busy(String id) {
        if (id == null) {
            return;
        }

        WifiP2pItem item = getWifiP2pItemByID(id);
        if (item != null) {
            item.busy();
        }
    }

    @Override
    public void deactive(String id) {
        if (id == null) {
            for (WifiP2pItem item : mItems) {
                item.deactive();
            }
        } else {
            WifiP2pItem item = getWifiP2pItemByID(id);
            if (item != null) {
                item.deactive();
            }
        }
    }

    @Override
    public void exiting(String id) {
        if (id == null) {
            return;
        }

        WifiP2pItem item = getWifiP2pItemByID(id);
        if (item != null) {
            item.exiting();
        }
    }

    private WifiP2pItem getWifiP2pItemByID(String id) {
        for (WifiP2pItem p2pItem: mItems) {
            if (p2pItem.getPeerInfo().getID().equals(id)) {
                return p2pItem;
            }
        }
        return null;
    }



    private class MyItem {
        public WifiP2pItem mItem;
        public int mNewPosition;
    };
    private void updateItemPositionMap() {
        mMapItemPosition.clear();
        int position = 1;
        int count = 0;

        for (WifiP2pItem item : mItems) {
            count = item.getCount();
            for (int i = 0; i < count; ++i) {
                MyItem myItem = new MyItem();
                myItem.mItem = item;
                myItem.mNewPosition = i;
                mMapItemPosition.put(position, myItem);
                position ++;
            }
        }
    }


    private boolean isEqualPeers(List<WifiP2pItem> list1, List<PeerInfo> list2) {
        if (list1 == null || list2 == null) {
            return false;
        }

        if (list1.size() != list2.size()) {
            return false;
        }

        Map<String, PeerInfo> map1 = new HashMap<String, PeerInfo>();
        for (int i = 0; i < list1.size(); ++i) {
            PeerInfo tmp = list1.get(i).getPeerInfo();
            map1.put(tmp.getID(), tmp);
        }

        for (PeerInfo info: list2) {
            if (!map1.containsKey(info.getID())) {
                return false;
            }
        }

        return true;
    }



    private class MyViewTitle {
        public TextView mTextView;
        public SimpleImageButton mImage;
    }

}
