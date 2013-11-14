package com.ivyshare.ui.main;

import android.content.Context;

import com.ivyshare.R;
import com.ivyshare.ui.setting.BasePopMenuAdapter;

public class NetworkMenuAdapter extends BasePopMenuAdapter {
	public NetworkMenuAdapter(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initMenuType() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void initMenuItem() {
		// TODO Auto-generated method stub
		mMenuItem = mContext.getResources().getStringArray(
				R.array.wifi_setting_items);
	}
}
