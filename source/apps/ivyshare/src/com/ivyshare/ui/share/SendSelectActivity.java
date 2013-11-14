package com.ivyshare.ui.share;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.engin.constdefines.IvyMessages;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.engin.im.Person;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyFragmentActivityBase;

public class SendSelectActivity extends IvyFragmentActivityBase {
	public static final String TAG = SendSelectActivity.class.getSimpleName();

    private SharePersonAdapter mAdapter;

	private Handler mHandler;

	private SendFragment mSendFragment = null;
	private ShareFragment mShareFragment = null;
	
    private int mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
    private String mShareFilePath;
    private String mShareFileDisplayName;
    private ImageView mFileImage;
    private TextView mFileText;

    private int mFileCount = 0;
    private Integer mListType = ShareType.SHARE_TYPE_UNKNOWN;
    private List<String> mListPath = null;
    
	private LocalSetting mLocalSetting;
    private View actionbar;

	private ViewPager mPagerView;
	private SectionsPagerAdapter mSectionsPagerAdapter;
    private ImageView mTabCursorImage;
    private TextView mTabText1, mTabText2;
    private int mOneStep;
    private int mOffset, mCurrentTabIndex, mCursorWidth;

	private PersonBroadCastReceiver mPersonReceiver = null;
    private boolean mPersonReceiverRegister = false;

    private NetworkReceiver mNetworkReceiver;

	private static final int MESSAGE_SERVICE_CONNECTED = 0;
    private static final int MESSAGE_NETWORK_STATE_CHANGED = 10;
    private static final int MESSAGE_INTENT_PROCESSED = 20;

    public static final int POS_SHARE = 0;
    public static final int POS_SEND = 1;
    public static final int POS_COUNT = 2;

    public static int mLoadSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserTrace.addTrace(UserTrace.ACTION_ENTER_SHARE);

        startService(new Intent("com.ivyshare.IVYNETWORKSERVICE_START"));
        setContentView(R.layout.activity_send_select);

		mLocalSetting = LocalSetting.getInstance();
		actionbar = (View) findViewById(R.id.layout_title);

		initTabTitleBar();
		initTabText();
		initViewPager();


        mFileImage = (ImageView)findViewById(R.id.file_icon);
        mFileText = (TextView)findViewById(R.id.file_name);
        mFileText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(MyApplication.getInstance(), mShareFileDisplayName, Toast.LENGTH_SHORT).show();
			}
        });

        mPersonReceiver = new PersonBroadCastReceiver();

		mHandler = new Handler(this.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case MESSAGE_SERVICE_CONNECTED:
						doOnCreate();
						doUpLine();
						break;
					case MESSAGE_NETWORK_STATE_CHANGED:
						doUpLine();
						doSetNetworkState();
						break;
                    case MESSAGE_INTENT_PROCESSED:
                        UpdateSendContent();
                        doSetShareContent();
                        doSetSendContent();
                        break;
				}
				super.handleMessage(msg);
			}
		};
		
        handleSendIntent();

        mNetworkReceiver = new NetworkReceiver();
		init();

		doOnCreate();
    }

	private void initTabTitleBar(){
		mTabCursorImage = (ImageView) findViewById(R.id.cursor);
		mCursorWidth = BitmapFactory.decodeResource(getResources(), R.drawable.tab_titlebar_cursor)
				.getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		mOffset = (screenW / 2 - mCursorWidth) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(mOffset, 0);
		mTabCursorImage.setImageMatrix(matrix);// 设置动画初始位置
		mOneStep = screenW / 2;// tab1 -> tab2 offect
	}
	
	private class TabTextClickListener implements OnClickListener{
		private int index = 0;
		
		public TabTextClickListener(int i){
			index = i;
		}
		
		@Override
		public void onClick(View v) {
			mPagerView.setCurrentItem(index);
		}
	}

	private void initTabText(){
		mTabText1 = (TextView) findViewById(R.id.tab_text1);
		mTabText2 = (TextView) findViewById(R.id.tab_text2);
	
		mTabText1.setOnClickListener(new TabTextClickListener(0));
		mTabText2.setOnClickListener(new TabTextClickListener(1));
	}
	
	private void initTabCursor(){
		Matrix matrix = new Matrix();
		matrix.postTranslate(mOffset, 0);
		mTabCursorImage.setImageMatrix(matrix);
	}
	
	private void initViewPager() {
	    Button sendButton = (Button)findViewById(R.id.PushPull);
        sendButton.setVisibility(View.GONE);

		// Init view pager
		mPagerView = (ViewPager) findViewById(R.id.pager);
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		mPagerView.setAdapter(mSectionsPagerAdapter);

		mPagerView.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {

				Animation animation = null;
				switch (arg0) {
				case 0:
					if (mCurrentTabIndex == 1) {
						animation = new TranslateAnimation(mOneStep, 0, 0, 0);
						Button sendButton = (Button)findViewById(R.id.PushPull);
						sendButton.setVisibility(View.GONE);
					}
					break;
				case 1:
					if (mCurrentTabIndex == 0) {
						animation = new TranslateAnimation(0, mOneStep, 0, 0);
						Button sendButton = (Button)findViewById(R.id.PushPull);
                        sendButton.setVisibility(View.VISIBLE);
					}
					break;
				}
				mCurrentTabIndex = arg0;
				animation.setFillAfter(true);// True: image stop position
				animation.setDuration(200);
				mTabCursorImage.startAnimation(animation);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
		
		initTabCursor();
		mPagerView.setCurrentItem(POS_SHARE);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public void onResume() {
    	super.onResume();
    	getPersonInformation();
    }
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POS_SEND:
                	return new SendFragment();
                    
                case POS_SHARE:
                    return new ShareFragment();
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public int getCount() {
            return POS_COUNT;
        }
    }

	private class PersonBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
        	mAdapter.ChangeList(getOnlinePersonList(mImManager.getPersonListClone()));
        	mAdapter.notifyDataSetChanged();
        }
	}

    private List<Person> getOnlinePersonList(List<Person> mListPersons) {
        List<Person> listOnlinePersons = new ArrayList<Person>();
        int nSize = mListPersons.size();
        for (int i=0; i<nSize; i++) {
            Person person = mListPersons.get(i);
            if (person.isOnline()) {
            	listOnlinePersons.add(person);
            }
        }
        return listOnlinePersons;
    }	
	
	private void doOnCreate() {
    	if (mImManager != null) {
    		if(!mPersonReceiverRegister) {

    			mAdapter = new SharePersonAdapter(SendSelectActivity.this, 
						getOnlinePersonList(mImManager.getPersonListClone()),mImManager);
    			if (mSendFragment != null) {
    				mSendFragment.setAdapter(mAdapter);
    				mSendFragment.setService(mImManager);
    			}

        		if (mShareFragment != null && mImManager != null) {
     			    mShareFragment.setImManager(mImManager);
     			    mShareFragment.setNetworkManager(mNetworkManager);
        			doShareFile();
        		}

				Log.d(TAG, "register person receiver");
				IntentFilter filter = new IntentFilter(IvyMessages.INTENT_PERSON);
				registerReceiver(mPersonReceiver, filter);

				mPersonReceiverRegister = true;
    		}
    	}
	}

	private void doUpLine() {
	    if (mImManager == null) {
	        return;
	    }
	    if (!mNetworkManager.getConnectionState().isConnected()) {
	        return;
	    }

	    mImManager.upLine();
	}

	private void doSetNetworkState() {
		if (mSendFragment == null) {
		    return;
		}
		if (!mNetworkManager.getConnectionState().isConnected()) {
            mShareFragment.configNetworkDisplay();
			return;
        }
		mShareFragment.configNetworkDisplay();
		doShareFile();
    }

    private void doShareFile() {
        if (mShareFragment != null) {
            mShareFragment.shareFile();
        }
    }
    
    private void doSetShareContent() {
        if (mShareFragment != null && mFileCount > 0 && mListPath != null && mListType != null) {
            mShareFragment.setShareContent(mFileCount, mListPath, mListType);
            doShareFile();
        } else if (mShareFilePath != null && mShareFileType > 0) {
        	mShareFragment.setShareContent(mShareFilePath, mShareFileType);
        }
    }

    private void doSetSendContent() {
        if (mSendFragment != null && mFileCount > 0 && mListPath != null && mListType != null) {
            mSendFragment.setSendContent(mFileCount, mListPath, mListType);
        } else if (mShareFilePath != null && mShareFileDisplayName != null && mShareFileType > 0) {
        	mSendFragment.setShareContent(mShareFilePath, mShareFileDisplayName, mShareFileType);
        }
    }

	private void init() {
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(IvyMessages.INTENT_NETWORK_AIRPLANE);
	    filter.addAction(IvyMessages.INTENT_NETWORK_STATECHANGE);
	    filter.addAction(IvyMessages.INTENT_NETWORK_FINISHSCANIVYROOM);
	    registerReceiver(mNetworkReceiver, filter);
	}
	
	public void setFragment(int pos, Fragment fragment) {
		switch(pos) {
			case POS_SHARE:
				mShareFragment = (ShareFragment)fragment;
				/*if (mImManager != null) {
	    			mShareFragment.setService(mImManager);
				}*/
				doSetShareContent();
                doSetNetworkState();
                doSetShareContent();
				break;
			case POS_SEND:
				mSendFragment = (SendFragment)fragment;
				if (mAdapter != null) {
					mSendFragment.setAdapter(mAdapter);
				}
				if (mImManager != null) {
					mSendFragment.setService(mImManager);
				}
				doSetShareContent();
				doSetSendContent();
				break;
		}
	}

    private boolean handleSendIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        
        if (extras == null) {
            return false;
        }
        
        final String mimeType = intent.getType();
        String action = intent.getAction();

        if (extras.containsKey("filesource")) {
        	mLoadSource = UserTrace.MAINPAGE_SHARE;
        } else {
        	mLoadSource = UserTrace.OTHERAPP_SHARE;
        }

		if (Intent.ACTION_SEND.equals(action)) {
			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				final Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				IntentAsyncTask task = new IntentAsyncTask(mHandler);
				task.execute(new Runnable[] { new Runnable() {
					@Override
					public void run() {
						addAttachment(mimeType, uri, 1);
					}
				} });
				return true;
			} else if (extras.containsKey(Intent.EXTRA_TEXT)) {
				// extras.getString(Intent.EXTRA_TEXT);
				return true;
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			setAttachmentType(mimeType);
			mListPath = extras.getStringArrayList(Intent.EXTRA_STREAM);
			mFileCount = mListPath.size();
			mListType = mShareFileType;
			StringBuilder sBuilder = new StringBuilder();
			for (int i = 0; i < mFileCount; i++) {
				sBuilder.append(CommonUtils.getFileNameByPath(mListPath.get(i)));
				if (i < mFileCount - 1) {
					sBuilder.append("\r\n");
				}
			}
			mShareFileDisplayName = sBuilder.toString();
			mHandler.sendEmptyMessage(MESSAGE_INTENT_PROCESSED);
		}
        return true;
    }
    
    private void setAttachmentType(String type) {
		mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
		if (type.startsWith("image/")) {
			mShareFileType = ShareType.SHARE_TYPE_IMAGE;
		} else if (type.startsWith("video/")) {
			mShareFileType = ShareType.SHARE_TYPE_VIDEO;
		} else if (type.startsWith("audio/")) {
			mShareFileType = ShareType.SHARE_TYPE_AUDIO;
		} else if (CommonUtils.MIMETYPE_VCARD.equalsIgnoreCase(type)) {
			mShareFileType = ShareType.SHARE_TYPE_CONTACT;
		} else {
			if (CommonUtils.MIMETYPE_APPLICATION.equalsIgnoreCase(type)) {
				mShareFileType = ShareType.SHARE_TYPE_APP;
			} else {
				mShareFileType = ShareType.SHARE_TYPE_OTHER;
			}			
		}
    }
    
    private void addAttachment(String type, Uri uri, int num) {
        if (uri != null) {
            Log.d(TAG, "addAttachment type=" + type + ", uri=" + uri);

            mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
            String scheme = uri.getScheme();

            boolean wildcard = "*/*".equals(type);
            if (type.startsWith("image/") || (wildcard && uri.toString().startsWith(CommonUtils.IMAGE_URI))) {
                mShareFileType = ShareType.SHARE_TYPE_IMAGE;
                doWithMediaUri(uri);
            } else if (type.startsWith("video/") ||
                    (wildcard && uri.toString().startsWith(CommonUtils.VIDEO_URI))) {
                mShareFileType = ShareType.SHARE_TYPE_VIDEO;
                doWithMediaUri(uri);
            } else if (type.startsWith("audio/") ||
                    (wildcard && uri.toString().startsWith(CommonUtils.AUDIO_URI))) {
                mShareFileType = ShareType.SHARE_TYPE_AUDIO;
                doWithMediaUri(uri);
            } else if (CommonUtils.MIMETYPE_VCARD.equalsIgnoreCase(type)) {
                mShareFileType = ShareType.SHARE_TYPE_CONTACT;
                doWithVCardUri(uri);
            } else if (scheme.equals("file")) {
            	if (CommonUtils.MIMETYPE_APPLICATION.equalsIgnoreCase(type)) {
            		mShareFileType = ShareType.SHARE_TYPE_APP;
            	} else {
            		mShareFileType = ShareType.SHARE_TYPE_OTHER;
            	}
                mShareFilePath = uri.getPath();
                mShareFileDisplayName = mShareFilePath.substring(mShareFilePath.lastIndexOf('/') + 1);
            }
        }
    }


    private int doWithVCardUri(Uri uri) {
        if (uri.getScheme().equals("file")) {
            mShareFilePath = uri.getPath();
            mShareFileDisplayName = mShareFilePath.substring(mShareFilePath.lastIndexOf('/') + 1);
        }else{
        	mShareFilePath = CommonUtils.getVCardByUri(this, uri);
        	if (mShareFilePath != null) {
        		mShareFileDisplayName = CommonUtils.getFileNameByPath(mShareFilePath);
        	}
        }
        return 0;
    }

    private int doWithMediaUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme.equals("content")) {
            String[] proj = { MediaStore.MediaColumns.DATA,
                              MediaStore.MediaColumns.TITLE};
            Cursor cursor = this.getContentResolver().query(uri,proj,null,null,null);
            if (cursor != null) {
                int data_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                int name_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE);
                cursor.moveToFirst();
                mShareFilePath = cursor.getString(data_index);
                mShareFileDisplayName = cursor.getString(name_index);
                cursor.close();
            } else {
                mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
            }
        } else if (uri.getScheme().equals("file")) {
            mShareFilePath = uri.getPath();
            mShareFileDisplayName = mShareFilePath.substring(mShareFilePath.lastIndexOf('/') + 1);
        }
        return 0;
    }

	private void getPersonInformation() {
		TextView userName = ((TextView) actionbar.findViewById(R.id.text_info));
		userName.setText(mLocalSetting.getMySelf().mNickName);

		ImageView imageleft = (ImageView) actionbar.findViewById(R.id.btn_left);
		CommonUtils.getPersonPhoto(imageleft,mLocalSetting.getMySelf().mImage);
	}

    public class IntentAsyncTask extends AsyncTask<Runnable, Void, Integer> {
        private Handler aHandler;
        public IntentAsyncTask(Handler handler) {
            aHandler = handler;
        }
        @Override
        protected Integer doInBackground(Runnable... params) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    params[i].run();
                }
            }
            return 0;
        }

        @Override  
        protected void onPostExecute(Integer result) {
            aHandler.sendEmptyMessage(MESSAGE_INTENT_PROCESSED);
        }
    }

    private void UpdateSendContent() {
        mFileText.setText(mShareFileDisplayName);

        switch (mShareFileType) {

        case ShareType.SHARE_TYPE_APP: {
        	mFileImage.setImageResource(R.drawable.ic_file_type_apk);
			if (mFileCount == 1) {
				PackageManager pm = getPackageManager();
				PackageInfo info = pm.getPackageArchiveInfo(mListPath.get(0),
						PackageManager.GET_ACTIVITIES);
				ApplicationInfo appInfo;
				try {
					appInfo = info.applicationInfo;
					appInfo.sourceDir = mListPath.get(0);
					appInfo.publicSourceDir = mListPath.get(0);
					mFileImage.setImageDrawable(appInfo.loadIcon(pm));
					mFileText.setText(appInfo.loadLabel(pm));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				String content = String.format(getString(R.string.share_file_content), mFileCount);
				mFileText.setText(content);
			}
        	break;
        }
        case ShareType.SHARE_TYPE_IMAGE: {
            mFileImage.setImageResource(R.drawable.ic_file_type_image);
            mFileImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			Bitmap bitmap = CommonUtils.DecodeBitmap(mShareFilePath, 256*256);
			if (bitmap != null) {
				mFileImage.setImageBitmap(bitmap);
			}
            break;
        }
        case ShareType.SHARE_TYPE_VIDEO:
            mFileImage.setImageResource(R.drawable.ic_file_type_video);
            break;
        case ShareType.SHARE_TYPE_AUDIO:
            mFileImage.setImageResource(R.drawable.ic_file_type_music);
            break;
        case ShareType.SHARE_TYPE_CONTACT:
            mFileImage.setImageResource(R.drawable.ic_file_type_vcard);
            break;
        case ShareType.SHARE_TYPE_OTHER:
            mFileImage.setImageResource(R.drawable.ic_file_type_other_file);
            break;
        }
    }

	private void doOnDestroy() {
	    
	    if( mShareFileType == ShareType.SHARE_TYPE_CONTACT 
	            && mShareFilePath.startsWith(LocalSetting.getInstance().getLocalPath())){
            File vcardfile = new File(mShareFilePath);
            if(vcardfile.exists()){
                vcardfile.delete();
            }
	    }
	    
    	unregisterReceiver(mPersonReceiver);
    	mPersonReceiverRegister = false;
	}
	
	private void uninit() {
	    unregisterReceiver(mNetworkReceiver);
	}

	@Override
	public void onDestroy(){
        UserTrace.addTrace(UserTrace.ACTION_EXIT_SHARE);

	    doOnDestroy();
	    uninit();
	    super.onDestroy();
	}

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        mHandler.sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
    }


    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(IvyMessages.INTENT_NETWORK_AIRPLANE)) {
                int isAirePlane = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_AIRPLANE_FLAG,
                        IvyMessages.VALUE_NETWORK_AIRPLANE_FLAG_TRUE);
                /* if (isAirePlane == IvyMessages.VALUE_NETWORK_AIRPLANE_FLAG_TRUE) {
                } else {
                } */
                mHandler.sendEmptyMessage(MESSAGE_NETWORK_STATE_CHANGED);
            } else if (action.equals(IvyMessages.INTENT_NETWORK_STATECHANGE)) {
                int stateIndex = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_STATE, 0);

                mHandler.sendEmptyMessage(MESSAGE_NETWORK_STATE_CHANGED);                
            } else if (action.equals(IvyMessages.INTENT_NETWORK_FINISHSCANIVYROOM)) {
                
            }
                        
        }
        
    }
}
