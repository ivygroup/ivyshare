package com.ivyshare.ui.main.contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.ivyshare.connection.APInfo;
import com.ivyshare.connection.ConnectionState;
import com.ivyshare.connection.IvyNetService;
import com.ivyshare.connection.IvyNetwork;
import com.ivyshare.connection.PeerInfo;
import com.ivyshare.connection.implement.AccessPointInfo;
import com.ivyshare.engin.control.ImService;
import com.ivyshare.engin.im.Person;

public class ContactAdapter extends BaseAdapter {
    private static final String TAG = "ContactAdapter";

    //
    private Context mContext;

    private HotsPotData mHotsPotData;
    // private IvyRoomData mIvyRoomData;
    private List<IvyRoomData2> mIvyRoomData2s;
    private WifiP2pData mWifiP2pDatas;
    private HallData mHallData;
    
    private class AdapterData {
        ContactDataBase mData;
        int mNewPosition;
    }
    private HashMap<Integer, AdapterData> mMapPositionToData;   //

    private ImService mImService = null;


    public ContactAdapter(Context context, ImService imService) {
        mContext = context;
        mHotsPotData = new HotsPotData(context, imService);
        mIvyRoomData2s = new ArrayList<IvyRoomData2>();
        mWifiP2pDatas = new WifiP2pData(context, imService);
        mHallData = new HallData(context, imService);
        mMapPositionToData = new HashMap<Integer, AdapterData>();
        mImService = imService;
    }



    @Override
    public int getCount() {
        updatePositionMap();
        return mMapPositionToData.size();
    }

    @Override
    public Object getItem(int position) {
        AdapterData adapter = getRightAdapter(position);
        if (adapter != null) {
            return adapter.mData.getItem(adapter.mNewPosition);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        AdapterData adapter = getRightAdapter(position);
        if (adapter != null) {
            return adapter.mData.getItemId(adapter.mNewPosition);
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AdapterData adapter = getRightAdapter(position);
        if (adapter != null) {
            return adapter.mData.getView(adapter.mNewPosition, convertView, parent);
        }

        return null;
    }

    @Override
    public int getViewTypeCount() {
        return ContactDataBase.VIEWTYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        AdapterData adapter = getRightAdapter(position);

        if (adapter != null) {
            return adapter.mData.getItemViewType(adapter.mNewPosition);
        }

        return 0;
    }


    public void setNetworkState(int connectionType, int networkState, String ssid) {
        switch (networkState) {
        	case ConnectionState.CONNECTION_STATE_WIFI_ENABLED:
        	    mHotsPotData.deactive();
        	    mHallData.deactive();
                for (IvyRoomData2 room : mIvyRoomData2s) {
                    room.deactive();
                }
        	    break;

        	case ConnectionState.CONNECTION_STATE_WIFI_DISABLED:
        		mHallData.deactive();
                for (IvyRoomData2 room : mIvyRoomData2s) {
                	room.deactive();
                }
        		break;

        	case ConnectionState.CONNECTION_STATE_WIFI_DISCONNECTED:
        	// case ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_DISCONNECTED:
        	// case ConnectionState.CONNECTION_STATE_WIFI_IVY_DISCONNECTED:
            {
                mHallData.deactive();
                if (ssid != null) {
                    ContactDataBase room = getIvyRoomBySSID(ssid);
                    if (room != null) {
                        room.deactive();
                    }
                } else {
                    /*for (IvyRoomData2 room : mIvyRoomData2s) {
                        room.deactive();
                    }*/
                }
            }
                break;

        	case ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_CONNECTING:
                mHotsPotData.deactive();
                for (IvyRoomData2 room : mIvyRoomData2s) {
                    room.deactive();
                }
                mHallData.busy();
                break;

        	case ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_CONNECTED:
                mHotsPotData.deactive();
                for (IvyRoomData2 room : mIvyRoomData2s) {
                    room.deactive();
                }
                mHallData.active(mImService.getPersonListClone());
                break;

        	case ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_DISCONNECTING:
        		break;

        	case ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING:
            {
                mHotsPotData.deactive();
                mHallData.deactive();
                if (ssid != null) {
                    for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
                        if (ivyRoomData2.getApInfo().getSSID().equals(ssid)) {
                            ivyRoomData2.busy();
                        } else {
                            ivyRoomData2.deactive();
                        }
                    }
                }
            }
        		break;

        	case ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED:
            {
                mHotsPotData.deactive();
                mHallData.deactive();
                if (ssid != null) {
                    IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();
                    if (ivyNetService != null) {
                        setPointList(ivyNetService.getScanResult());
                    }

                    ContactDataBase room = getIvyRoomBySSID(ssid);
                    if (room != null) {
                        room.active(mImService.getPersonListClone());
                    }
                }
            }
                break;
        	case ConnectionState.CONNECTION_STATE_WIFI_IVY_DISCONNECTING:
        		break;

        		// hotspot
        	case ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLING:
                mHotsPotData.active(mImService.getPersonListClone());
                for (IvyRoomData2 room : mIvyRoomData2s) {
                    room.deactive();
                }
                mHallData.deactive();

                mHotsPotData.busy();
                break;

        	case ConnectionState.CONNECTION_STATE_HOTSPOT_DISABLING:
        		mHotsPotData.exiting();
        		break;

        	case ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED:
                for (IvyRoomData2 room : mIvyRoomData2s) {
                    room.deactive();
                }
                mHallData.deactive();
                mHotsPotData.active(mImService.getPersonListClone());
                break;

        	case ConnectionState.CONNECTION_STATE_HOTSPOT_DISABLED:
        		mHotsPotData.deactive();
        		break;

        		// wifip2p
        	case ConnectionState.CONNECTION_STATE_WIFIP2P_ENABLED:
        	case ConnectionState.CONNECTION_STATE_WIFIP2P_DISABLED:
        	    break;

        	case ConnectionState.CONNECTION_STATE_WIFIP2P_CONNECTING:
            {
                mWifiP2pDatas.busy(ssid);
            }
            break;

        	case ConnectionState.CONNECTION_STATE_WIFIP2P_CONNECTED:
        	{
        	    mWifiP2pDatas.active(mImService.getPersonListClone(), ssid);
        	}
        	break;
        	
        	case ConnectionState.CONNECTION_STATE_WIFIP2P_DISCONNECTING:
        	{
        	    mWifiP2pDatas.exiting(ssid);
        	}
        	break;

        	case ConnectionState.CONNECTION_STATE_WIFIP2P_DISCONNECTED:
        	{
        	    mWifiP2pDatas.deactive(ssid);
        	}
        	break;

            default:
                break;
        }
    }

    public void setPointList(List<APInfo> list) {
        if (list == null) {
            mIvyRoomData2s.clear();
            return;
        }

        // Log.d(TAG, "setPointList called. old size = " + mIvyRoomData2s.size() + ", new size = " + list.size());

        if (isEqualList(mIvyRoomData2s, list)) {
            return;
        }

        // 1 save old and clear.
        String activeIvyRoomSSID = getActiveIvyRoomSSID();
        mIvyRoomData2s.clear();

        // 2 build new ivyrooms.
        for (APInfo info: list) {
            mIvyRoomData2s.add(new IvyRoomData2(mContext, mImService, info));
        }

        // 3 find active point.
        if (activeIvyRoomSSID != null) {
            for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
                if (ivyRoomData2.getApInfo().getSSID().equals(activeIvyRoomSSID)) {
                    ivyRoomData2.active(mImService.getPersonListClone());
                }
            }
        }
    }

    public void changeList(List<Person> listPersons) {
        ContactDataBase adapter = getActiveAdapter();
        if (adapter != null) {
            adapter.setData(listPersons);
        }
    }

    public void setWifiP2pPeers(List<PeerInfo> list) {
        mWifiP2pDatas.setWifiP2pPeers(list);
    }

    //
    private void updatePositionMap() {
        mMapPositionToData.clear();
        int position = 0;
        int count = 0;

        // hotspot
        count = mHotsPotData.getCount();
        for (int i = 0; i < count; ++i) {
            AdapterData data = new AdapterData();
            data.mData = mHotsPotData;
            data.mNewPosition = i;
            mMapPositionToData.put(position, data);
            position++;
        }

        // ivy room
        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            count = ivyRoomData2.getCount();
            for (int i = 0; i < count; ++i) {
                AdapterData data = new AdapterData();
                data.mData = ivyRoomData2;
                data.mNewPosition = i;
                mMapPositionToData.put(position, data);
                position++;
            }
        }

        // wifi direct
        count = mWifiP2pDatas.getCount();
        for (int i = 0; i < count; ++i) {
            AdapterData data = new AdapterData();
            data.mData = mWifiP2pDatas;
            data.mNewPosition = i;
            mMapPositionToData.put(position, data);
            position++;
        }


        // hall
        count = mHallData.getCount();
        for (int i = 0; i < count; ++i) {
            AdapterData data = new AdapterData();
            data.mData = mHallData;
            data.mNewPosition = i;
            mMapPositionToData.put(position, data);
            position++;
        }
    }

    private AdapterData getRightAdapter(int position) {
        if (position < 0) {
            return null;
        }

        if (!mMapPositionToData.containsKey(position)) {
            return null;
        }

        AdapterData data = mMapPositionToData.get(position);
        return data;
    }

    private ContactDataBase getActiveAdapter() {
        if (mHotsPotData.isActive()) {
            return mHotsPotData;
        }

        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            if (ivyRoomData2.isActive()) {
                return ivyRoomData2;
            }
        }

        if (mHallData.isActive()) {
            return mHallData;
        }
        return null;
    }

    public int getActiveRoomHead() {
    	int nHeadPos = 0;
        if (mHotsPotData.isActive()) {
            return nHeadPos;
        }
        nHeadPos += mHotsPotData.getCount();

        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            if (ivyRoomData2.isActive()) {
                return nHeadPos;
            }
            nHeadPos += ivyRoomData2.getCount();
        }

        return nHeadPos;
    }

    private ContactDataBase getActiveIvyRoom() {
        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            if (ivyRoomData2.isActive()) {
                return ivyRoomData2;
            }
        }

        return null;
    }

    private ContactDataBase getIvyRoomBySSID(String ssid) {
        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            if (ivyRoomData2.getApInfo().getSSID().equals(ssid)) {
                return ivyRoomData2;
            }
        }

        return null;
    }

    private String getActiveIvyRoomSSID() {
        for (IvyRoomData2 ivyRoomData2 : mIvyRoomData2s) {
            if (ivyRoomData2.isActive()) {
                return ivyRoomData2.getApInfo().getSSID();
            }
        }

        return null;
    }

    private boolean isEqualList(List<IvyRoomData2> list1, List<APInfo> list2) {
        if (list1 == null || list2 == null) {
            return false;
        }

        if (list1.size() != list2.size()) {
            return false;
        }

        Map<String, APInfo> map1 = new HashMap<String, APInfo>();
        for (int i = 0; i < list1.size(); ++i) {
            APInfo tmp = list1.get(i).getApInfo();
            map1.put(tmp.getSSID(), tmp);
        }

        for (APInfo info: list2) {
            if (!map1.containsKey(info.getSSID())) {
                return false;
            }
        }

        return true;
    }
}
