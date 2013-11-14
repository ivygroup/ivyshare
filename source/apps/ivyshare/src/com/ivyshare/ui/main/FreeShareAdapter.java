package com.ivyshare.ui.main;

import java.sql.Date;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivyshare.R;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.engin.control.LocalSetting.UserIconEnvironment;
import com.ivyshare.engin.data.Table_Share;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.util.CommonUtils;

class FreeShareAdapter extends CursorAdapter {
//	private static final String TAG = FreeShareAdapter.class.getSimpleName();
	private Context mContext;

    public FreeShareAdapter(Context context, Cursor cursor) {
        super(context,cursor, true);
        mContext = context;
    }

    @Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = null;
		LayoutInflater factory = LayoutInflater.from(context);
		view = factory.inflate(R.layout.list_freeshare_item, null);
		return view;
	}

    private void setDefaultValue(ImageView picture, int imageid, TextView text, String txtvalue) {
		LayoutParams param = picture.getLayoutParams();
		param.width = param.height = 80;
		picture.setImageResource(imageid);
		picture.setScaleType(ImageView.ScaleType.FIT_XY);
		picture.setLayoutParams(param);
		text.setText(CommonUtils.getFileNameByPath(txtvalue));
    }

    @Override
	public void bindView(View view, Context context, Cursor cursor) {
		FileType type = FileType.values()[cursor.getInt(cursor.getColumnIndex(Table_Share.TYPE))];
    	String content = cursor.getString(cursor.getColumnIndex(Table_Share.CONTENT));
    	long time = cursor.getLong(cursor.getColumnIndex(Table_Share.TIME));

    	ImageView photo = (ImageView)view.findViewById(R.id.freeshare_right_image);
		ImageView picture = (ImageView)view.findViewById(R.id.freeshare_right_pic);
		TextView text = (TextView)view.findViewById(R.id.freeshare_right_pic_prompt);
		TextView textTime = (TextView)view.findViewById(R.id.freeshare_right_time);

		setMessageTime(textTime, time);

		final LocalSetting mLocalSetting = LocalSetting.getInstance();
		String personHeadIcon = mLocalSetting.getMySelf().mImage;
		UserIconEnvironment userIconEnvironment = mLocalSetting
				.getUserIconEnvironment();

		if (userIconEnvironment.isExistHead(personHeadIcon, -1)) {
			Bitmap bitmap = CommonUtils.DecodeBitmap(
					userIconEnvironment.getFriendHeadFullPath(personHeadIcon),
					256 * 256);
			photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
			photo.setImageBitmap(bitmap);
		}

		photo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("isMyself", true);
				intent.putExtra("chatpersonKey",mLocalSetting.getMySelf().mName);
				intent.setClass(mContext, QuickPersonInfoActivity.class);
				mContext.startActivity(intent);

			}

		});

		switch (type) {
    		case FileType_App: {
    			setDefaultValue(picture, R.drawable.ic_file_type_apk, text, 
    					CommonUtils.getFileNameByPath(content));
 
    	        PackageManager pm = mContext.getPackageManager();
    	        PackageInfo info = pm.getPackageArchiveInfo(content, PackageManager.GET_ACTIVITIES);
    	        ApplicationInfo appInfo;
    	        try {
    	            appInfo = info.applicationInfo;
    	            appInfo.sourceDir = content;
    	            appInfo.publicSourceDir = content;
    	            picture.setImageDrawable(appInfo.loadIcon(pm));
    	            text.setText(appInfo.loadLabel(pm));
    	        } catch (Exception e) {
    	        	e.printStackTrace();
    	        }
    	        break;
    		}
    		case FileType_Contact: {
    			setDefaultValue(picture, R.drawable.ic_file_type_vcard, text, 
    					CommonUtils.getFileNameByPath(content));
    			break;
    		}
    		case FileType_Record: {
    			setDefaultValue(picture, R.drawable.record_voice_right, text, null);
    			break;
    		}
    		case FileType_Picture: {
    			LayoutParams param = picture.getLayoutParams();
    			picture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    			picture.setLayoutParams(param);
    			text.setText(null);

    			Bitmap bitmap = CommonUtils.DecodeBitmap(content, 256*256);
    			if (bitmap != null) {
    				picture.setImageBitmap(bitmap);
    			}
    			break;
    		}
            case FileType_Music: {
    			setDefaultValue(picture, R.drawable.ic_file_type_music, text, 
    					CommonUtils.getFileNameByPath(content));
            	break;
            }
            case FileType_Video: {
    			setDefaultValue(picture, R.drawable.ic_file_type_video, text, 
    					CommonUtils.getFileNameByPath(content));
                break;
            }
            case FileType_OtherFile: {
    			setDefaultValue(picture, R.drawable.ic_file_type_other_file, text, 
    					CommonUtils.getFileNameByPath(content));
    			break;
    		}
		}
	}

    private void setMessageTime(TextView text, long time) {
        Date today = new Date(System.currentTimeMillis());
        Date messageDay = new Date(time);
        if (today.getYear() == messageDay.getYear()
                && today.getMonth() == messageDay.getMonth()
                && today.getDate() == messageDay.getDate()) {
        	text.setText(DateFormat.format("kk:mm", time).toString());
        } else {
        	text.setText(DateFormat.format("MM-dd kk:mm", time).toString());
        }
    }
}