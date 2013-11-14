package com.ivyshare.ui.chat.abstractchat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ivyshare.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ApplicationAdapter extends BaseAdapter implements View.OnClickListener
{
    private Context mContext;
    private List<AppsInfo> mlistAppInfo = null;
    private List<AppsInfo> mData = null;
    private SelectChangeListener mListener;
    
    public interface SelectChangeListener {
		public void onSelectedChanged();
	}
    
    public ApplicationAdapter(Context c,  List<AppsInfo> apps, SelectChangeListener listener)
    {        
        mContext = c;
        mlistAppInfo = apps;
        mListener = listener;
        mData = new LinkedList<AppsInfo>();
    }
    
    public List<AppsInfo> getAllFiles() {
        synchronized (mData) {
            return new ArrayList<AppsInfo>(mData);
        }
    }
    
    public boolean isPathInBasket(String path) {
        int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
            AppsInfo info = mData.get(i);
            if (info.sourceDir.compareToIgnoreCase(path) == 0) {
                return true;
            }
        }
        return false;
    }
    
    public void addToBasket(AppsInfo info) {
        synchronized (mData) {
        	if (!isPathInBasket(info.sourceDir)) {
                mData.add(info);
            }
        }
    }

    public void removeFromBasket(AppsInfo path) {
        int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
        	AppsInfo info = mData.get(i);
            if (info.sourceDir.compareTo(path.sourceDir) == 0) {
                removeFile(i);
                break;
            }
        }
    }
    
    public void removeFile(int position) {
        if (position < 0 || position >= mData.size()) {
            return;
        }

        synchronized (mData) {
            removeFile_l(position);
        }
    }

    public void removeAllFiles() {
        synchronized (mData) {
            for (int i = 0; i < mData.size(); i++) {
                removeFile_l(i);
            }
            mData.clear();
        }
    }

    private void removeFile_l(int position) {
        mData.remove(position);
    }

    public ArrayList<String> getFilesPathList() {
        ArrayList<String> list = new ArrayList<String>();
        int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
            list.add(mData.get(i).sourceDir);
        }
        return list;
    }

    public int getFilesCount() {
        synchronized (mData) {
            return mData.size();
        }
    }
    
    public void updateSelectItem() {
        int nSize = mlistAppInfo.size();
        for (int i=0; i<nSize; i++) {
            AppsInfo info = mlistAppInfo.get(i);
        }
        notifyDataSetChanged();
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
            view = LayoutInflater.from(mContext).inflate(R.layout.list_app_item, null);
            view.setClickable(true);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            view = convertview;
            holder = (ViewHolder) convertview.getTag();
        }
        holder.pos = position;

        view.setOnClickListener(this);

        AppsInfo appInfo = (AppsInfo) getItem(position);
        holder.appIcon.setImageDrawable(appInfo.appIcon);
        holder.tvAppLabel.setText(appInfo.appLabel);

        if (appInfo.isSelected) {
        	addToBasket(appInfo);
            holder.tvAppLabel.setTextColor(mContext.getResources().getColor(R.color.list_main));
            holder.appSelected.setVisibility(View.VISIBLE);
        } else {
        	removeFromBasket(appInfo);
            holder.tvAppLabel.setTextColor(mContext.getResources().getColor(R.color.list_secondray));
            holder.appSelected.setVisibility(View.GONE);
        }
       
        return view;
    }

    class ViewHolder {
        public View view;
        public ImageView appIcon;
        public TextView tvAppLabel;
        public ImageView appSelected;
        public int pos;

        public ViewHolder(View view) {
            this.view = view;
            this.appIcon = (ImageView) view.findViewById(R.id.imgApp);
            this.tvAppLabel = (TextView) view.findViewById(R.id.tvAppLabel);
            this.appSelected = (ImageView) view.findViewById(R.id.app_selected);
        }

    }
    
    @Override
    public void onClick(View v) {
        ViewHolder holder = (ViewHolder) v.getTag();
        int pos = holder.pos;
        int nSize = mlistAppInfo.size();
        if (pos < 0 || pos >= nSize) {
            return;
        }

        AppsInfo info = mlistAppInfo.get(pos);
        info.isSelected = !info.isSelected;
        notifyDataSetChanged();
        
        mListener.onSelectedChanged();
    }
    
    public void disSelectAll() {
		int nSize = mlistAppInfo.size();
		for (int i=0; i<nSize; i++) {
			mlistAppInfo.get(i).isSelected = false;
		}
		notifyDataSetChanged();

		mListener.onSelectedChanged();
		removeAllFiles();
	}
    
	public int getSelectItemCount() {
		int nSize = mlistAppInfo.size();
		int nCount = 0;
		for (int i=0; i<nSize; i++) {
			if (mlistAppInfo.get(i).isSelected) {
				nCount++;
			}
		}
		return nCount;
	}

}