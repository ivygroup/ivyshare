package com.ivyshare.ui.main;

import java.util.List;

import com.ivyshare.R;
import com.ivyshare.ui.chat.abstractchat.AppsInfo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FreeShareAppAdapter extends BaseAdapter implements View.OnClickListener
{
    public interface SelectChangeListener {
    	public void onSelectChanged();
    }

    private Context mContext;
    private SelectChangeListener mListener;
    private List<AppsInfo> mlistAppInfo = null;
    public FreeShareAppAdapter(Context c,  List<AppsInfo> apps)
    {
        mContext = c;
        mlistAppInfo = apps;
    }

    public void setListener(SelectChangeListener listener) {
    	mListener = listener;
    }

    @Override  
    public int getCount()
    {
        return mlistAppInfo.size();
    }

    @Override  
    public Object getItem(int position)
    {
        return mlistAppInfo.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertview, ViewGroup arg2) {
        View view = null;
        ViewHolder holder = null;
        if (convertview == null || convertview.getTag() == null) {
            view =  LayoutInflater.from(mContext).inflate(R.layout.list_app_item, null);
            view.setClickable(true);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } 
        else{
            view = convertview ;
            holder = (ViewHolder)convertview.getTag() ;
        }
        holder.pos = position;

        view.setOnClickListener(this);

        AppsInfo appInfo = (AppsInfo) getItem(position);
		holder.appIcon.setImageDrawable(appInfo.appIcon);
        holder.tvAppLabel.setText(appInfo.appLabel);

        if (appInfo.isSelected) {
        	holder.appIcon.setBackgroundColor(mContext.getResources().getColor(R.color.selected_share_files));
        } else {
        	holder.appIcon.setBackgroundColor(Color.TRANSPARENT);
        }

        return view;

    }

    class ViewHolder {
    	public View view;
        public ImageView appIcon;
        public TextView tvAppLabel;
        public int pos;

        public ViewHolder(View view) {
        	this.view = view;
            this.appIcon = (ImageView) view.findViewById(R.id.imgApp);
            this.tvAppLabel = (TextView) view.findViewById(R.id.tvAppLabel);
        }
    }

	@Override
	public void onClick(View v) {
		ViewHolder holder = (ViewHolder)v.getTag();
		int pos = holder.pos;
		int nSize = mlistAppInfo.size();
		if (pos < 0 || pos >= nSize) {
			return;
		}
		for (int i=0; i<nSize; i++) {
			if (i != pos) {
				mlistAppInfo.get(i).isSelected = false;
			}
		}
		AppsInfo info = mlistAppInfo.get(pos);
		info.isSelected = !info.isSelected;
		if (mListener != null) {
			mListener.onSelectChanged();
		}
		notifyDataSetChanged();
	}

	public AppsInfo getSelectAppInfo() {
		int nSize = mlistAppInfo.size();

		for (int i=0; i<nSize; i++) {
			AppsInfo info = mlistAppInfo.get(i);
			if (info.isSelected) {
				return info;
			}
		}
		return null;
	}
}

