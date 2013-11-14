package com.ivyshare.ui.setting;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.ivyshare.R;

public class SettingMenuAdapter extends BasePopMenuAdapter implements
		OnItemClickListener {
	public int[] mMenuId;

	public SettingMenuAdapter(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initMenuType() {
		// TODO Auto-generated method stub
		mMenuType = MENU_LIST;
	}

	@Override
	protected void initMenuItem() {
		// TODO Auto-generated method stub
		mMenuId = new int[] { R.id.action_usersetting,
				R.id.action_systemsetting, /*R.id.action_update,*/ R.id.action_quit };

		mMenuItem = new String[] {
				mContext.getResources().getString(R.string.action_usersetting),
				mContext.getResources()
						.getString(R.string.action_systemsetting),
				/*mContext.getResources().getString(R.string.action_update),*/
				mContext.getResources().getString(R.string.action_exit) };

		mMenuIcon = new int[] { R.drawable.ic_side_user_setting,
				R.drawable.system_setting_disabled,
				/*R.drawable.ic_side_sync_soft,*/ R.drawable.ic_side_exit };
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

	}
}
