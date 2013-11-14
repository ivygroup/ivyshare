package com.ivyshare.ui.setting;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ivyshare.R;
import com.ivyshare.engin.connection.ConnectionState;
import com.ivyshare.engin.constdefines.IvyMessages;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.util.IvyActivityBase;

public class NetworkSettingActivity extends IvyActivityBase implements OnClickListener {
    private LinearLayout mLinearLayoutWifi;
    private LinearLayout mLinearLayoutMySelfRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_setting);

        // for action bar
        initActionBar();

        // for wifi
        ((Button)findViewById(R.id.network_setting_wifi_btnset)).setOnClickListener(this);

		init();
		
		int networkType = mNetworkManager.getConnectionState().getLastType();
		int networkStatus = mNetworkManager.getConnectionState().getLastStateByFast();
		onNetworkChanged(networkType, networkStatus);
    }

	@Override
	public void onDestroy() {
	    uninit();

	    super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		int networkType = mNetworkManager.getConnectionState().getLastType();
		int networkStatus = mNetworkManager.getConnectionState().getLastStateByFast();
		onNetworkChanged(networkType, networkStatus);
	}

    private void initActionBar() {
        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.networksetting);

        ImageView imageleft = (ImageView)actionbar.findViewById(R.id.btn_left);
        imageleft.setImageResource(R.drawable.ic_setting_wifi);
        imageleft.setVisibility(View.VISIBLE);
        imageleft.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.network_setting_wifi_btnset:
                Intent intent;

                if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                    intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                } else {
                    intent = new Intent();
                    ComponentName component = new ComponentName("com.android.settings","com.android.settings.WirelessSettings");
                    intent.setComponent(component);
                    intent.setAction("android.intent.action.VIEW");
                }

                startActivity(intent);
                break;

            case R.id.btn_left:
            	finish();
            default:
                break;
        }
    }

	private NetworkReceiver mNetworkReceiver = new NetworkReceiver();

	private void init() {
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(IvyMessages.INTENT_NETWORK_AIRPLANE);
	    filter.addAction(IvyMessages.INTENT_NETWORK_STATECHANGE);
	    filter.addAction(IvyMessages.INTENT_NETWORK_FINISHSCANIVYROOM);

	    registerReceiver(mNetworkReceiver, filter);
	}

	private void uninit() {
	    unregisterReceiver(mNetworkReceiver);
	}

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();

            if (action.equals(IvyMessages.INTENT_NETWORK_STATECHANGE)) {
            	int type = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_TYPE, 0);
                int state = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_STATE, 0);

                onNetworkChanged(type, state);
            }
        }
    }

    public void onNetworkChanged(int connectionType, int state) {
        if (connectionType == ConnectionState.CONNECTION_UNKNOWN
        		|| state == ConnectionState.CONNECTION_UNKNOWN) {
        	return;	
        }

        // set ssid and IP.
		String ssid = null;
		String ip = null;

		if (state == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED
				|| state == ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_CONNECTED
				|| state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED
				) {
			if (mNetworkManager != null) {
				ssid = mNetworkManager.getConnectionInfo().getSSID();
			}
			if (LocalSetting.getInstance().getMySelf().mIP != null) {
				ip = LocalSetting.getInstance().getMySelf().mIP.getHostAddress();
			}
		}

		if (null != ssid) {
			((RelativeLayout)findViewById(R.id.wifi_name_layout)).setVisibility(View.VISIBLE);
			((FrameLayout)findViewById(R.id.wifi_name_divider)).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.wifi_name)).setText(ssid);
		} else {
			((RelativeLayout)findViewById(R.id.wifi_name_layout)).setVisibility(View.GONE);
			((FrameLayout)findViewById(R.id.wifi_name_divider)).setVisibility(View.GONE);
		}
		if (null != ip) {
			((RelativeLayout)findViewById(R.id.wifi_address_layout)).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.address)).setText(ip);
		} else {
			((RelativeLayout)findViewById(R.id.wifi_address_layout)).setVisibility(View.GONE);
		}

		// set password.
		findViewById(R.id.layoutwifipassword).setVisibility(View.GONE);
		findViewById(R.id.wifi_password_divider).setVisibility(View.GONE);
		if (connectionType == ConnectionState.CONNECTION_TYPE_HOTSPOT
				&& state == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED) {
			findViewById(R.id.layoutwifipassword).setVisibility(View.VISIBLE);
			findViewById(R.id.wifi_password_divider).setVisibility(View.VISIBLE);
			String password = mNetworkManager.getConnectionInfo().getIvyHotspotPassword();
			if (null != password) {
				((TextView)findViewById(R.id.wifi_password)).setText(password);
			}
		}
    }
}
