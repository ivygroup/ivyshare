package com.ivyshare.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivyshare.R;

public abstract class BasePopMenuAdapter extends BaseAdapter {
	protected Context mContext;
	protected String[] mMenuItem;
	protected int[] mMenuIcon;

	private boolean[] mChecked;

	protected static final int MENU_CHECKBOX = 0;
	protected static final int MENU_LIST = 1;

	protected int mMenuType = MENU_CHECKBOX;

	public BasePopMenuAdapter(Context context) {
		mContext = context;
		mMenuItem = mContext.getResources().getStringArray(
				R.array.wifi_setting_items);

		initMenuType();
		initMenuItem();
		initCheck();
	}

	/**
	 * should set mMenuType in subclass
	 */
	protected abstract void initMenuType();

	/**
	 * should set mMenuItem & mMenuIcon in subclass
	 */
	protected abstract void initMenuItem();

	private void initCheck() {
		if (mMenuItem == null || mMenuItem.length == 0)
			return;

		mChecked = new boolean[mMenuItem.length];
	}

	/**
	 * @param index
	 */
	public void setItemCheckd(int index) {
		if (mMenuType != MENU_CHECKBOX)
			return;

		for (int i = 0; i < mChecked.length; i++) {
			if (index == i)
				mChecked[i] = true;
			else
				mChecked[i] = false;
		}

		notifyDataSetChanged();
	}

	/**
	 * @param index
	 * @return
	 */
	public boolean isChecked(int index) {
		if (mChecked == null)
			return false;

		return mChecked[index];
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		if (mMenuItem == null)
			return 0;

		return mMenuItem.length;
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mMenuItem[arg0];
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup arg2) {
		// TODO Auto-generated method stub
		ViewHolder holder;
		if (view == null) {
			holder = new ViewHolder();
			view = LayoutInflater.from(mContext).inflate(
					R.layout.pop_menu_item_layout, null);
			holder.leftIcon = (ImageView) view
					.findViewById(R.id.menuitem_icon_left);
			holder.menuItem = (TextView) view.findViewById(R.id.menuTextView);
			holder.rightIcon = (ImageView) view
					.findViewById(R.id.menuitem_icon_right);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		holder.menuItem.setText(mMenuItem[position]);
		if (mMenuIcon != null && mMenuIcon.length > position)
			holder.leftIcon.setImageResource(mMenuIcon[position]);

		if (mChecked[position]) {
			holder.rightIcon.setVisibility(View.VISIBLE);
		} else {
			holder.rightIcon.setVisibility(View.INVISIBLE);
		}

		return view;
	}

	private final class ViewHolder {
		public ImageView leftIcon;
		public TextView menuItem;
		public ImageView rightIcon;
	}
}
