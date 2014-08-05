package com.ivyshare.ui.share;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivyshare.R;

public class SelectFileAdapter extends BaseAdapter {
	private Context mContext;
	private int currFocusId = 0;
	private String[] mItemTitles;
	private TypedArray mItemIcons;

	public SelectFileAdapter(Context c, String[] title, TypedArray icon) {
		mContext = c;
		mItemTitles = title;
		mItemIcons = icon;
	}

	@Override
	public int getCount() {
		return mItemTitles.length;
	}

	@Override
	public Object getItem(int position) {
		return mItemTitles[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertview, ViewGroup arg2) {
		View view = null;
		ViewHolder holder = null;

		if (convertview == null || convertview.getTag() == null) {
			view = LayoutInflater.from(mContext).inflate(
					R.layout.chat_file_select, null);
			holder = new ViewHolder(view);
			holder.image.setImageResource(mItemIcons.getResourceId(position, 0));
			holder.imageName.setText(mItemTitles[position]);
			view.setTag(holder);
		} else {
			view = convertview;
			holder = (ViewHolder) convertview.getTag();
		}

		if (position == currFocusId) {
			view.setBackgroundColor(mContext.getResources().getColor(R.color.selected_share_files));
		}

		return view;

	}

	class ViewHolder {
		ImageView image;
		TextView imageName;

		public ViewHolder(View view) {
			this.image = (ImageView) view.findViewById(R.id.itemImage);
			this.imageName = (TextView) view.findViewById(R.id.itemText);
		}
	}
}
