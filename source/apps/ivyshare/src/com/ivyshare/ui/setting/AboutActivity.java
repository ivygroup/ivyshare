package com.ivyshare.ui.setting;

import com.ivyshare.R;
import com.ivyshare.updatemanager.CurrentVersion;
import com.ivyshare.util.IvyActivityBase;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends IvyActivityBase {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

		// for action bar
        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.about);

        ImageView imageleft= (ImageView)actionbar.findViewById(R.id.btn_left);
		imageleft.setImageResource(R.drawable.system_setting_enabled);
        imageleft.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
        TextView mVersionNumber = (TextView)findViewById(R.id.versioninformation);

		try{
			PackageInfo info = getPackageManager().getPackageInfo(CurrentVersion.appPackName, 0);
			String information = String.format(getString(R.string.version_information),info.versionName);
			mVersionNumber.setText(information);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		TextView mBlog = (TextView)findViewById(R.id.weibo);
		mBlog.setText(R.string.weibo);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
}
