package com.ivyshare.ui.main.contact;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.connection.ConnectionState;
import com.ivy.ivyengine.connection.NetworkManager;
import com.ivy.ivyengine.control.ImManager;
import com.ivyshare.R;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.widget.SimpleImageButton;

public class HallData extends ContactDataBase {
    private static final String TAG = "HallAdapter";

    public HallData(Context context, ImManager imManager, NetworkManager networkManager) {
        super(context, imManager, networkManager);
    }
    
    @Override
    public int getCount() {
        if (!isActive()) {
            return 2;   // Title + enable wifi
        } else {
            int personCount = 0;
            if (mListPersons != null) {
                personCount = mListPersons.size();
            }

            if (personCount == 0) {
                return 2;   // Title + no persons.
            } else {
                return mListPersons.size() + 1; // Title + all persons.
            }
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
        if (position == 0) {
            return VIEWTYPE_HALL_TITLE;
        } else if (position == 1) {
            int personCount = 0;
            if (mListPersons != null) {
                personCount = mListPersons.size();
            }

            if (!isActive()) {
                return VIEWTYPE_HALL_ENTRY;
            } else {
                if (personCount == 0) {
                    return VIEWTYPE_HALL_ENTRY;
                }
            }
            return VIEWTYPE_PERSON;

        } else {
            return VIEWTYPE_PERSON;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int positionType = getItemViewType(position);

        if (VIEWTYPE_PERSON == positionType) {
            return super.innerGetView(position - 1, convertView, parent, false);

        } else if (VIEWTYPE_HALL_TITLE == positionType) {
            MyClassTitle title = null;
            if (convertView == null) {
                title = new MyClassTitle();
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_hall_title, null);
                title.mTitle = (TextView)convertView.findViewById(R.id.room_title);
                title.mImage = (SimpleImageButton)convertView.findViewById(R.id.btn_right);
                title.mImage.setOnClickListener(this);
                title.mImage.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						Toast msg = Toast.makeText(mContext, R.string.group_chat_broadcastname, Toast.LENGTH_SHORT);
						msg.setGravity(Gravity.TOP, msg.getXOffset(), msg.getYOffset() * 2);
						msg.show();
						return true;
					}
                	
                });
                convertView.setTag(title);
            } else {
                title = (MyClassTitle)convertView.getTag();
            }

            if (isActive()) {
                if (mNetworkManager != null) {
                    // title.mTitle.setText(ivyNetService.getConnectionInfo().getFriendlyName());
                    title.mTitle.setText(R.string.contact_listitem_hall_title);
                } else {
                    title.mTitle.setText(R.string.contact_listitem_hall_title);
                }

                title.mImage.setVisibility(View.VISIBLE);
                
            } else {
                title.mTitle.setText(R.string.contact_listitem_hall_title);
                title.mImage.setVisibility(View.GONE);
            }
            return convertView;

        } else if (VIEWTYPE_HALL_ENTRY == positionType) {
            MyClassHallEntry hallEntry = null;
            if (convertView == null) {
                hallEntry = new MyClassHallEntry();
                LayoutInflater factory = LayoutInflater.from(mContext);
                convertView = factory.inflate(R.layout.list_contact_item_hall_entry, null);
                hallEntry.mLinearLayout = (LinearLayout)convertView.findViewById(R.id.list_contact_item_hall_entry_layout);
                hallEntry.mTextView = (TextView)convertView.findViewById(R.id.text);
                hallEntry.mImageView = (ImageView)convertView.findViewById(R.id.info_image);
                convertView.setTag(hallEntry);                
                hallEntry.mLinearLayout.setOnClickListener(this);
            } else {
                hallEntry = (MyClassHallEntry)convertView.getTag();
            }

            // set the text
            int currentState = getCurrentState();
            switch (currentState) {
                case CURRENT_STATE_ENTRY:
                    hallEntry.mTextView.setText(R.string.contact_listitem_hall_entry);
                    hallEntry.mImageView.setVisibility(View.GONE);
                    break;

                case CURRENT_STATE_ENTRYING:
                    hallEntry.mTextView.setText(R.string.contact_listitem_hall_entrying);
                    hallEntry.mImageView.setVisibility(View.GONE);
                    break;

                case CURRENT_STATE_NOPERSON:
                    hallEntry.mTextView.setText(R.string.contact_listitem_ivyroom_nopersontext);
                    hallEntry.mImageView.setVisibility(View.VISIBLE);
                    break;

                case CURRENT_STATE_OPENNETWORKSETTING:
                    hallEntry.mTextView.setText(R.string.contact_listitem_hall_opennetworksetting);
                    hallEntry.mImageView.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }

            if (isBusying()) {
                hallEntry.mLinearLayout.setClickable(false);
                hallEntry.mLinearLayout.setBackgroundResource(R.drawable.list_selector_disable);
                hallEntry.mTextView.setTextColor(mContext.getResources().getColor(R.color.list_secondray));
            } else {
                hallEntry.mLinearLayout.setClickable(true);
                hallEntry.mLinearLayout.setBackgroundResource(R.drawable.list_selector);
                hallEntry.mTextView.setTextColor(mContext.getResources().getColor(R.color.list_main));
            }

            return convertView;
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.list_contact_item_hall_entry_layout) {
            if (isBusying()) {
                return;
            }

            int currentState = getCurrentState();
            switch (currentState) {
                case CURRENT_STATE_ENTRY:
                    downLineAndCloseOldNetwork();
                    break;

                case CURRENT_STATE_ENTRYING:
                    // empty
                    break;

                case CURRENT_STATE_NOPERSON:
                	infoDialog();
                    break;

                case CURRENT_STATE_OPENNETWORKSETTING:
                    downLineAndCloseOldNetwork();
                    {
                        Intent intent;
                        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                            intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                        } else {
                            intent = new Intent();
                            ComponentName component = new ComponentName("com.android.settings","com.android.settings.WirelessSettings");
                            intent.setComponent(component);
                            intent.setAction("android.intent.action.VIEW");
                        }
                        mContext.startActivity(intent);
                    }
                    break;
                default:
                    break;
            }
            return;

        }   // end id == list_contact_item_hall_entry_layout


        super.onClick(v);
    }

    private void infoDialog() {
		Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(mContext)
				.setTitle(R.string.no_friends_title)
				.setMessage(R.string.no_friends_message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.ok,null)
				.setNegativeButton(null, null).create();
		alertDialog.show();
	}

    private static final int CURRENT_STATE_ENTRY = 0;       // back to hall
    private static final int CURRENT_STATE_ENTRYING = 1;    // entrying ...
    private static final int CURRENT_STATE_NOPERSON = 2;    // no persons
    private static final int CURRENT_STATE_OPENNETWORKSETTING = 3;    // go to setting
    private int getCurrentState() {
        //      1
        boolean bIsWifiEnabled = true;
        int hotspotstate = ConnectionState.CONNECTION_UNKNOWN;
        int wifistate = ConnectionState.CONNECTION_UNKNOWN;

        //      2
        if (mNetworkManager != null) {
            bIsWifiEnabled = mNetworkManager.isWifiEnabled();
            hotspotstate = mNetworkManager.getConnectionState().getHotspotState();
            wifistate = mNetworkManager.getConnectionState().getWifiState();
        }

        //      3
        if (isBusying()) {
            return CURRENT_STATE_ENTRYING;
        }

        if (!bIsWifiEnabled) {
            if (hotspotstate == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED
                    || hotspotstate == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLING
                    || hotspotstate == ConnectionState.CONNECTION_STATE_HOTSPOT_DISABLING) {
                return CURRENT_STATE_ENTRY;
            }
        } else {
            if (isActive() && wifistate == ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_CONNECTED) {
                return CURRENT_STATE_NOPERSON;
            } else if (isDeactive()
                    && (wifistate == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED) || (wifistate == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING)) {
                return CURRENT_STATE_ENTRY;
            }
        }

        return CURRENT_STATE_OPENNETWORKSETTING;
    }

    private void downLineAndCloseOldNetwork() {
        if (mNetworkManager != null) {

            if (mImManager != null) {
                mImManager.downLine();
            }

            int hotspotstate = mNetworkManager.getConnectionState().getHotspotState();
            if (hotspotstate == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED
                    || hotspotstate == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLING) {
                mNetworkManager.disableHotspot();
                UserTrace.addTrace(UserTrace.ACTION_DESTROY_ROOM);
            }

            int wifistate = mNetworkManager.getConnectionState().getWifiState();
            if (wifistate == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED
            		|| wifistate == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING) {
                mNetworkManager.disconnectFromIvyNetwork();
                UserTrace.addTrace(UserTrace.ACTION_EXIT_ROOM);
            }
        }
    }
    
    private class MyClassTitle {
        public TextView mTitle;
        public SimpleImageButton mImage;
    }
    
    private class MyClassHallEntry {
        public LinearLayout mLinearLayout;
        public TextView mTextView;
        public ImageView mImageView;
    }
}
