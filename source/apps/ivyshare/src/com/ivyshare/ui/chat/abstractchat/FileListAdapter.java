package com.ivyshare.ui.chat.abstractchat;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivyshare.R;

public class FileListAdapter extends ArrayAdapter<FileInfo> {
    private LayoutInflater mInflater;
    private Context mContext;
	private static final String TAG = FileListAdapter.class.getSimpleName();

    public FileListAdapter(Context context, int resource,
            List<FileInfo> fileList) {
        super(context, resource, fileList);
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        ViewHolder holder = null;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.file_browser_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
        	view = convertView;
        	holder = (ViewHolder)view.getTag();
        }

        FileInfo info = getItem(position);
        if (info.mIsDirecotry) {
        	holder.mViewCheckArea.setVisibility(View.GONE);
        } else {
        	holder.mViewCheckArea.setVisibility(View.VISIBLE);
        	if (info.mSelected) {
        		holder.mImageCheck.setVisibility(View.VISIBLE);
        	} else {
        		holder.mImageCheck.setVisibility(View.GONE);
        	}
        }

        holder.mTextFileName.setText(info.mFileName);
        holder.mTextFileSize.setText(info.mIsDirecotry ? "" : convertStorage(info.mFileSize));
        holder.mTextModifyTime.setText(formatDateString(mContext, info.mModifiedDate));

        if (info.mIsDirecotry) {
        	holder.mImageFile.setImageResource(R.drawable.file_icon_folder);
        } else {
        	holder.mImageFile.setImageResource(R.drawable.file_icon_default);
        }

        return view;
    }

    public static String formatDateString(Context context, long time) {
        DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat
                .getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date) + " " + timeFormat.format(date);
    }

    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }

    public class ViewHolder {
    	View mViewCheckArea;
    	ImageView mImageCheck;
    	TextView mTextFileName;
    	TextView mTextFileCount;
    	TextView mTextModifyTime;
    	TextView mTextFileSize;
    	ImageView mImageFile;

    	public ViewHolder(View view) {
    		mViewCheckArea = view.findViewById(R.id.file_checkbox_area);
    		mImageCheck = (ImageView)view.findViewById(R.id.file_checkbox);
    		mTextFileName = (TextView)view.findViewById(R.id.file_name);
    		mTextFileCount = (TextView)view.findViewById(R.id.file_count);
    		mTextFileSize = (TextView)view.findViewById(R.id.file_size);
    		mTextModifyTime = (TextView)view.findViewById(R.id.modified_time);
    		mImageFile = (ImageView)view.findViewById(R.id.file_image);
    	}
    }
}
