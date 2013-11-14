package com.ivyshare.ui.main;

import com.ivyshare.ui.setting.UserEditActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LuncherActivity extends Activity {
 
 @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		boolean hasInited = sp.getBoolean("INIT", false);
		if (hasInited) {
			startMainActivity (MainPagerActivity.class);
		} else {
			startMainActivity (UserEditActivity.class);
		}
 }
	private void startMainActivity (Class targetActivity) {
		Intent intent = new Intent();
		intent.setClass(this, targetActivity);
		startActivity(intent);
		finish();
	}
}
