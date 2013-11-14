package com.ivyshare.ui.setting;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ivyshare.R;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyActivityBase;

public class SystemSettingActivity extends IvyActivityBase implements
        OnClickListener, OnCheckedChangeListener {
	//private static final String TAG = "SystemSettingActivity";

    private LocalSetting mLocalSetting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_system_setting);

		// for action bar
        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.systemsetting);

        ImageView imageleft= (ImageView)actionbar.findViewById(R.id.btn_left);
        imageleft.setImageResource(R.drawable.system_setting_enabled);
        imageleft.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				finish();
			}
		});


        //
        mLocalSetting = LocalSetting.getInstance();


        // for checkboxes:
        CheckBox checkBox = null;

        checkBox = (CheckBox)findViewById(R.id.system_setting_ring_checkbox);
        checkBox.setChecked(mLocalSetting.getRing());
        checkBox.setOnCheckedChangeListener(this);

        checkBox = (CheckBox)findViewById(R.id.system_setting_vibrate_checkbox);
        checkBox.setChecked(mLocalSetting.getVibrate());
        checkBox.setOnCheckedChangeListener(this);

        checkBox = (CheckBox)findViewById(R.id.system_setting_traceaction_checkbox);
        checkBox.setChecked(mLocalSetting.getTraceAction());
        checkBox.setOnCheckedChangeListener(this);

        // for other button click
		RelativeLayout layoutClick = null;

		layoutClick = (RelativeLayout)findViewById(R.id.layoutnetwork);
        layoutClick.setOnClickListener(this);

        layoutClick = (RelativeLayout)findViewById(R.id.system_setting_ring);
        layoutClick.setOnClickListener(this);

        layoutClick = (RelativeLayout)findViewById(R.id.system_setting_vibrate);
        layoutClick.setOnClickListener(this);

        layoutClick = (RelativeLayout)findViewById(R.id.system_setting_traceaction);
        layoutClick.setOnClickListener(this);

		layoutClick = (RelativeLayout)findViewById(R.id.layout_about);
		layoutClick.setOnClickListener(this);

		layoutClick = (RelativeLayout)findViewById(R.id.layout_clear_history);
		layoutClick.setOnClickListener(this);

		//layoutClick = (RelativeLayout)findViewById(R.id.layout_update_version);
		//layoutClick.setOnClickListener(this);

		layoutClick = (RelativeLayout)findViewById(R.id.feed_back);
		layoutClick.setOnClickListener(this);

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	    switch (buttonView.getId()) {
	        case R.id.system_setting_ring_checkbox:
	            mLocalSetting.saveRing(isChecked);
	            break;
	        case R.id.system_setting_vibrate_checkbox:
	            mLocalSetting.saveVibrate(isChecked);
	            break;
	        case R.id.system_setting_traceaction_checkbox:
	            mLocalSetting.saveTraceAction(isChecked);
	            UserTrace.setShareTrace(isChecked);
	            break;
	    }
	}

	@Override
	public void onClick(View arg0) {
	    switch(arg0.getId()) {
	        case R.id.layoutnetwork:
	        {
	            // Log.d(TAG, "click network layout.");
	            CheckBox checkBox = (CheckBox)findViewById(R.id.networkcheck);
	            checkBox.setChecked(!checkBox.isChecked());
	            break;
	        }
	        case R.id.system_setting_ring:
	        {
                CheckBox checkBox = (CheckBox)findViewById(R.id.system_setting_ring_checkbox);
                checkBox.setChecked(!checkBox.isChecked());
            	UserTrace.addTrace(UserTrace.ACTION_MODIFY_NOTI);
                break;
            }
	        case R.id.system_setting_vibrate:
	        {
                CheckBox checkBox = (CheckBox)findViewById(R.id.system_setting_vibrate_checkbox);
                checkBox.setChecked(!checkBox.isChecked());
                UserTrace.addTrace(UserTrace.ACTION_VIEW_ABOUT);
                break;
            }
	        case R.id.system_setting_traceaction:
	        {
                CheckBox checkBox = (CheckBox)findViewById(R.id.system_setting_traceaction_checkbox);
                checkBox.setChecked(!checkBox.isChecked());
                break;
            }

	        case R.id.layout_about: {
	            Intent intent = new Intent(SystemSettingActivity.this, AboutActivity.class);
	            startActivity(intent);
	            UserTrace.addTrace(UserTrace.ACTION_MODIFY_NOTI);
	        }
	        break;
	        case R.id.feed_back: {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=" + getPackageName()));
				startActivity(intent);
	            UserTrace.addTrace(UserTrace.ACTION_ADD_FEEDBACK);
	        }
	        break;
	        case R.id.layout_clear_history: {
	            Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(this).
	                    setTitle(R.string.clear_title).
	                    setMessage(R.string.makesure_clear).
	                    setIcon(android.R.drawable.ic_dialog_alert).
	                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                        @Override 
	                        public void onClick(DialogInterface dialog, int which) {
	                            if (mImManager != null) {
	                                mImManager.deleteAllMessage();
	                                UserTrace.addTrace(UserTrace.ACTION_DELETE_ALL);
	                            }
	                        }
	                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                        @Override 
	                        public void onClick(DialogInterface dialog, int which) {
	                            dialog.cancel();
	                        }
	                    }).create();
	            alertDialog.show();
	        }
	        break;
	        /*case R.id.layout_update_version: {
	        	UserTrace.addTrace(UserTrace.ACTION_CHECK_UPDATE);
	            UpdateManager.getInstance(this).checkUpdate();
	        }
	        break;*/
	    }
	}

	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	}
}
