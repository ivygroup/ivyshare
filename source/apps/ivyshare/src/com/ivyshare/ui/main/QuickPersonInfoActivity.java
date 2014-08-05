package com.ivyshare.ui.main;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.R;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyActivityBase;

public class QuickPersonInfoActivity extends IvyActivityBase {
	private static final String TAG = QuickPersonInfoActivity.class.getSimpleName();

	private Person mPerson;
	private Handler mHandler;
	private static final int MESSAGE_SERVICE_CONNECTED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_person_info);

		mHandler = new Handler(this.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case MESSAGE_SERVICE_CONNECTED:
						boolean isMyself = getIntent().getBooleanExtra("isMyself", false);
						if (isMyself) {
							mPerson = LocalSetting.getInstance().getMySelf();
						} else {
							String keyString = (String) getIntent().getExtras().get("chatpersonKey");
							mPerson = mImManager.getPerson(keyString);
						}

						if (mPerson == null) {
							finish();
						} else {
							TextView text1 = (TextView)findViewById(R.id.nickname);
							text1.setText(mPerson.mNickName);
							
							TextView text2 = (TextView)findViewById(R.id.groupname);
							text2.setText(mPerson.mGroup);

							if (mPerson.mIP != null) {
								TextView text3 = (TextView)findViewById(R.id.address);
								text3.setText(mPerson.mIP.getHostAddress().toString());
							}

							TextView text4 = (TextView)findViewById(R.id.signname);
							text4.setText(mPerson.mSignature);

							UserIconEnvironment userIconEnvironment = LocalSetting.getInstance().getUserIconEnvironment();
							if (userIconEnvironment.isExistHead(mPerson.mImage, -1)) {
							    ImageView headIcon = (ImageView)findViewById(R.id.photoimage);
							    Bitmap bitmap = CommonUtils.DecodeBitmap(userIconEnvironment.getFriendHeadFullPath(mPerson.mImage), 256*256);
							    headIcon.setImageBitmap(bitmap);
							}
						}
				}
			}
		};
	}

	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        mHandler.sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
    }
}
