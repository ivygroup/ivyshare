package com.ivyshare.ui.main.contact;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ivyshare.R;
import com.ivyshare.engin.connection.ConnectionState;
import com.ivyshare.engin.connection.NetworkManager;
import com.ivyshare.engin.constdefines.IvyMessages;
import com.ivyshare.engin.control.ImManager;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.widget.SimpleImageButton;

public class HotsPotData extends ContactDataBase {
    private static final String TAG = "HotsPotAdapter";

    public HotsPotData(Context context, ImManager imManager, NetworkManager networkManager) {
        super(context, imManager, networkManager);
    }

    @Override
    public int getCount() {
        if (mListPersons == null) {
            deactive();
            return 2;   // Title + (create button)
        }

        if (!isActive()) {
            return 2;   // Title + (create button)
        }

        if (mListPersons.size() > 0) {
            return mListPersons.size() + 1;  // Title + all persons.
        } else {
            return mListPersons.size() + 2;  // Title + "wait other people join..." + all persons.
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
            return VIEWTYPE_HOTSPOT_TITLE;
        }

        if (position == 1 && !isActive()) {
            return VIEWTYPE_HOTSPOT_NOTACTIVE;
        }

        if (position == 1 && isActive() && mListPersons.size() == 0) {
            return VIEWTYPE_HOTSPOT_NOTACTIVE;
        }

        return VIEWTYPE_PERSON;
    }

    @Override
    public View getView(int position, View view, ViewGroup arg2) {
        int positionType = getItemViewType(position);
        switch (positionType) {
            case VIEWTYPE_HOTSPOT_TITLE:
            {
                MyViewTitle title = null;
                if (view == null) {
                    title = new MyViewTitle();
                    LayoutInflater factory = LayoutInflater.from(mContext);
                    view = factory.inflate(R.layout.list_contact_item_hotspot_title, null);
                    title.mTextView = (TextView)view.findViewById(R.id.room_hint_text);
                    title.mImage = (SimpleImageButton)view.findViewById(R.id.btn_right);
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
                    view.setTag(title);
                } else {
                    title = (MyViewTitle)view.getTag();
                }
                
                if(isActive()){
                    title.mImage.setVisibility(View.VISIBLE);
                }else{
                    title.mImage.setVisibility(View.GONE);
                }
                
                title.mTextView.setText(R.string.contact_listitem_hotspot_title);
                return view;
            }

            case VIEWTYPE_HOTSPOT_NOTACTIVE:
            {
                MyViewNoActive noActive = null;
                if (view == null) {
                    noActive = new MyViewNoActive();
                    LayoutInflater factory = LayoutInflater.from(mContext);
                    view = factory.inflate(R.layout.list_contact_item_hotspot_noactive, null);
                    noActive.mTextView = (TextView)view.findViewById(R.id.text);
                    noActive.mLayout = (LinearLayout)view.findViewById(R.id.list_contract_item_hotspot_noactive_layout);
                    noActive.mLayout.setOnClickListener(this);
                    view.setTag(noActive);
                } else {
                    noActive = (MyViewNoActive)view.getTag();
                }

                if (isBusying()) {
                    disableItemOnUI(noActive.mLayout, noActive.mTextView);
                    noActive.mTextView.setText(R.string.contact_listitem_hotspot_button_creating);
                    
                } else if (isExiting()) {
                	disableItemOnUI(noActive.mLayout, noActive.mTextView);
                    noActive.mTextView.setText(R.string.contact_listitem_hotspot_button_exiting);

                } else if (isActive() && mListPersons.size() == 0) {
                    noActive.mTextView.setText(R.string.contact_listitem_hotspot_button_waitjoin);
                    disableItemOnUI(noActive.mLayout, noActive.mTextView);
                    noActive.mTextView.setTextColor(mContext.getResources().getColor(R.color.list_main));
                } else {
                    noActive.mTextView.setText(R.string.contact_listitem_hotspot_button_create);
                    enableItemOnUI(noActive.mLayout, noActive.mTextView);
                }

                return view;
            }

            case VIEWTYPE_PERSON:
                if (isActive()) {
                    return super.innerGetView(position - 1, view, arg2,(getCount()-1 == position));
                } else {
                    return super.innerGetView(position - 2, view, arg2, (getCount()-1 == position));                    
                }

            default:
                break;
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.list_contract_item_hotspot_noactive_layout) {
        	askCreateMyRoom();
        }

        super.onClick(v);
    }

	private void askCreateMyRoom() {
	    if (isBusying()) {
            return;
        }

		Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(mContext)
				.setTitle(R.string.create_title)
				.setMessage(R.string.makesure_create)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,int which) {
					            if (mNetworkManager != null) {
					                /*if (ivyNetService.getConnectionInfo().isBusying()) {
					                    return;
					                }*/

					                if (mImManager != null) {
					                    mImManager.downLine();
					                }
					                int state = mNetworkManager.getConnectionState().getWifiState();
					                if (state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED
					                        || state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTING) {
					                    mNetworkManager.disconnectFromIvyNetwork();
					                }
					                IvyMessages.sendNetworkStateChange(ConnectionState.CONNECTION_TYPE_HOTSPOT,
					                		ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLING,
					                		null);
					                mNetworkManager.createHotspot();
					                IvyMessages.sendNetworkClearIvyRoom();
					                UserTrace.addTrace(UserTrace.ACTION_CREATE_ROOM);
					            }
							}
						}).setNegativeButton(R.string.cancel, null).create();
		alertDialog.show();
	}

    private class MyViewTitle {
        public TextView mTextView;
        public SimpleImageButton mImage;
    }

    private class MyViewNoActive {
        public LinearLayout mLayout;
        public TextView mTextView;
    }
}
