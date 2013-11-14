package com.ivyshare.ui.main.contact;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ivyshare.R;
import com.ivyshare.connection.APInfo;
import com.ivyshare.connection.ConnectionState;
import com.ivyshare.connection.IvyNetService;
import com.ivyshare.connection.IvyNetwork;
import com.ivyshare.connection.implement.AccessPointInfo;
import com.ivyshare.constdefines.IvyMessages;
import com.ivyshare.engin.control.ImService;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.widget.SimpleImageButton;

public class IvyRoomData2 extends ContactDataBase {
    private APInfo mApInfo;

    public IvyRoomData2(Context context, ImService imService, APInfo apinfo) {
        super(context, imService);
        mApInfo = apinfo;
    }

    public APInfo getApInfo() {
        return mApInfo;
    }

    @Override
    public int getCount() {
        if (!isActive()) {
            return 2;
        }

        int personCount = 0;
        if (mListPersons != null) {
            personCount = mListPersons.size();
        }

        if (personCount == 0) {
            return 2;
        } else {
            return 1 + personCount;
        }
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
        if (position == 0) {
            return VIEWTYPE_IVYROOM_ITEM;
        }

        if (position == 1) {
            if (!isActive()) {
                return VIEWTYPE_IVYROOM_ENTRY;
            } else {
                int personCount = 0;
                if (mListPersons != null) {
                    personCount = mListPersons.size();
                }
                if (personCount == 0) {
                    return VIEWTYPE_IVYROOM_NOPERSONTEXT;
                } else {
                    return VIEWTYPE_PERSON;
                }
            }
        }

        return VIEWTYPE_PERSON;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int positionType = getItemViewType(position);

        if (positionType == VIEWTYPE_PERSON) {
            return super.innerGetView(position - 1, convertView, parent, (position == getCount() - 1));
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
            item.mImage.setOnClickListener(this);
            item.mApInfo = mApInfo;
            
            if (isActive()) {
                item.mImage.setVisibility(View.VISIBLE);
            } else {
                item.mImage.setVisibility(View.GONE);
            }

            item.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_item, item.mApInfo.getFriendlyName()));
            return convertView;

        } else if (positionType == VIEWTYPE_IVYROOM_ENTRY) {
            MyViewEntry entry = null;
            if (convertView == null) {
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_ivyroom_entry, null);
                entry = new MyViewEntry();
                entry.mTextView = (TextView)convertView.findViewById(R.id.text);
                entry.mLayout = (LinearLayout)convertView.findViewById(R.id.list_contact_item_ivyroom_entry_layout);
                convertView.setTag(entry);
            } else {
                entry = (MyViewEntry)convertView.getTag();
            }

            entry.mLayout.setOnClickListener(this);
            entry.mApInfo = mApInfo;

            if (isBusying()) {
                disableItemOnUI(entry.mLayout, entry.mTextView);
                entry.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_entrying,
                        entry.mApInfo.getFriendlyName()));
            } else {
                entry.mTextView.setText(mContext.getString(R.string.contact_listitem_ivyroom_entry, entry.mApInfo.getFriendlyName()));
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_contact_item_ivyroom_entry_layout: {
                {
                    MyViewEntry myClass = (MyViewEntry) v.getTag();

                    if (myClass.mApInfo != mApInfo) {
                        return;
                    }

                    if (isBusying()) {
                        return;
                    }

                    IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();
                    if (ivyNetService != null) {
                        /*if (ivyNetService.getConnectionInfo().isBusying()) {
                            return;
                        }*/

                        if (mImService != null) {
                            mImService.downLine();
                        }
                        int state = ivyNetService.getConnectionState().getHotspotState();
                        if (state == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED
                                || state == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLING) {
                            ivyNetService.disableHotspot();
                        }

                        state = ivyNetService.getConnectionState().getWifiState();
                        if (state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED
                                || state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING) {
                            ivyNetService.disconnectFromIvyNetwork();
                        }

                        ivyNetService.connectIvyNetwork(myClass.mApInfo.getSSID());
                        IvyMessages.sendNetworkStateChange(ConnectionState.CONNECTION_TYPE_WIFI,
                        		ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING,
                        		myClass.mApInfo.getSSID());
                        UserTrace.addTrace(UserTrace.ACTION_ENTER_ROOM);
                    }
                    return;
                }
            }
        }

        super.onClick(v);
    }

    private class MyViewItem {
        public TextView mTextView;
        public APInfo mApInfo;
        public SimpleImageButton mImage;
    }

    private class MyViewEntry {
        public LinearLayout mLayout;
        public TextView mTextView;
        public APInfo mApInfo;
    }
}


