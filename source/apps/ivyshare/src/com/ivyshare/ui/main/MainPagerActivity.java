package com.ivyshare.ui.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ivyshare.R;
import com.ivyshare.connection.ConnectionState;
import com.ivyshare.connection.IvyNetService;
import com.ivyshare.connection.IvyNetwork;
import com.ivyshare.constdefines.IvyMessages;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.engin.data.Table_Message;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.chat.chat.ChatActivity;
import com.ivyshare.ui.chat.groupchat.GroupChatActivity;
import com.ivyshare.ui.main.contact.ContactAdapter;
import com.ivyshare.ui.main.contact.ContactFragment;
import com.ivyshare.ui.setting.AboutActivity;
import com.ivyshare.ui.setting.NetworkSettingActivity;
import com.ivyshare.ui.setting.SettingMenuAdapter;
import com.ivyshare.ui.setting.SystemSettingActivity;
import com.ivyshare.ui.setting.UserEditActivity;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyFragmentActivityBase;
import com.ivyshare.updatemanager.UpdateManager;
import com.ivyshare.widget.SimplePopMenu;
import com.ivyshare.widget.SimplePopMenu.OnPopMenuItemClickListener;

/**
 * Main Activity, UI structure
 * 
 * @author b456
 */
@SuppressLint("NewApi")
public class MainPagerActivity extends IvyFragmentActivityBase implements OnClickListener {
	public static final String TAG = MainPagerActivity.class.getSimpleName();

	private ViewPager mPagerView;
	private SectionsPagerAdapter mSectionsPagerAdapter;

	public static final String OPEN_PAGE = "open_page";
    public static final int PAGE_SESSION = 1;
    public static final int PAGE_CONTACTS = 0;
    public static final int PAGE_FREESHARE = 2;
    public static final int PAGE_COUNT = 3;

	private static final int MESSAGE_SERVICE_CONNECTED = 0;
    private static final int MESSAGE_NETWORK_STATE_CHANGED = 10;    //
                                                                    // arg1 = connection type,
                                                                    // arg2 = connection state
                                                                    // obj = ssid (if state = ivy wifi state)
    private static final int MESSAGE_NETWORK_SCAN_FINISH = 11;
    private static final int MESSAGE_NETWORK_CLEAR_IVYROOM= 12;
    private static final int MESSAGE_NETWORK_DISCOVERYWIFIP2P= 13;


	private Handler mHandler;
    private ContactAdapter mContactAdapter = null;
    private SessionAdapter mSessionAdapter = null;
    private FreeShareAdapter mFreeShareAdapter = null;
	private ContactFragment mContactFragment = null;
	private SessionFragment mSessionFragment = null;	
	private FreeShareFragment mFreeShareFragment = null;
	private LocalSetting mLocalSetting;

	private PersonBroadCastReceiver mPersonReceiver = null;
    private MessageBroadCastReceiver mMessageReceiver = null;
    private GroupMessageBroadCastReceiver mGroupMessageReceiver = null;
    private NetworkReceiver mNetworkReceiver = null;
    private boolean mMessageReceiverRegister = false;
    private boolean mPersonReceiverRegister = false;

    private ImageButton mWifiSettings;
    private ProgressBar mWifiSwitchingBar;
    
    private NetworkMenuAdapter mNetworkMenuAdapter;
    private int mNetworkState;

    //Tab title bar
    private ImageView mTabCursorImage, mTabMessageIcon;
    private TextView mTabText1, mTabText2, mTabText3;
    private int mOffect, mCurrentTabIndex, mCursorWidth;
    
    SimplePopMenu mPopMenu;
    View actionbar;
    private IntentFilter mIntentFilter;
    int oneStep, twoStep;

    private static final int[] mWifiIcons = {
        R.drawable.ic_setting_wifi,
        R.drawable.ic_setting_hotspot_open,
        R.drawable.ic_setting_hotspot_lock,
        R.drawable.ic_setting_network_unavailable
    };

    @Override
    protected void onNewIntent(Intent intent) {
    	changePagerByIntent(intent);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity_pager);
		mLocalSetting = LocalSetting.getInstance();
		actionbar = (View) findViewById(R.id.layout_title);
		//Init tab title bar
		initTabTitleBar();
		initTabText();
		initViewPager();
//		initWlanSettings();

		
		mPersonReceiver = new PersonBroadCastReceiver();
		mMessageReceiver = new MessageBroadCastReceiver();
		mGroupMessageReceiver = new GroupMessageBroadCastReceiver();
		mNetworkReceiver = new NetworkReceiver();

		mHandler = new Handler(this.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
			    IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();

				switch (msg.what) {
					case MESSAGE_SERVICE_CONNECTED:
						Log.d(TAG, "Service connect, Do onCreate and onResume");
						doOnCreate();
						doOnResume();
						doUpLine();
						break;

					case MESSAGE_NETWORK_STATE_CHANGED:
					    {
					    	int type = msg.arg1;
					    	int state = msg.arg2;

					        if (ConnectionState.isBusy(state)) {
					            onWifiSwitching(true);
					        } else {
					            onWifiSwitching(false);
					        }

					        if (mWifiSettings != null) {
	                            if (state == ConnectionState.CONNECTION_STATE_WIFI_PUBLIC_CONNECTED) {
	                                mWifiSettings.setImageResource(mWifiIcons[0]);
	                            } else if (state == ConnectionState.CONNECTION_STATE_WIFI_IVY_CONNECTED) {
	                                mWifiSettings.setImageResource(mWifiIcons[0]);
	                            } else if (state == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED){
	                                mWifiSettings.setImageResource(mWifiIcons[2]);
	                            } else {
	                                mWifiSettings.setImageResource(mWifiIcons[3]);
	                            }
	                        }

					        if (ConnectionState.isConnected(state)) {
                                doUpLine();
                            } else {
                                doDownLine();
                            }

					        if (mContactAdapter != null) {
					            String ssid = (String)msg.obj;
	                            mContactAdapter.setNetworkState(type, state, ssid);
	                            mContactAdapter.notifyDataSetChanged();
	                        }
					    }
						break;

                    case MESSAGE_NETWORK_SCAN_FINISH:
                        Log.d(TAG, "Service connect, Do onCreate and onResume");
                        if (mContactAdapter != null && ivyNetService!= null) {
                            mContactAdapter.setPointList(ivyNetService.getScanResult());
                            mContactAdapter.notifyDataSetChanged();
                        }
                        break;

                    case MESSAGE_NETWORK_CLEAR_IVYROOM:
                    {
                        if (mContactAdapter != null) {
                            mContactAdapter.setPointList(null);
                            mContactAdapter.notifyDataSetChanged();
                        }
                    }
                        break;

                    case MESSAGE_NETWORK_DISCOVERYWIFIP2P:
                    {
                        if (mContactAdapter != null && ivyNetService != null) {
                            mContactAdapter.setWifiP2pPeers(ivyNetService.getWifiP2pPeers());
                            mContactAdapter.notifyDataSetChanged();
                        }
                    }
                        break;
				}
				super.handleMessage(msg);
			}
		};


		startService(new Intent("com.ivyshare.IMSERVICE_START"));

		// initPopMenu();
		initNetworkSetting();
		initActionBarForHighVersion();
		getPersonInformation();

		Log.d(TAG, "Do onCreate in onCreate");
		doOnCreate();

        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(IvyMessages.INTENT_NETWORK_AIRPLANE);
            filter.addAction(IvyMessages.INTENT_NETWORK_STATECHANGE);
            filter.addAction(IvyMessages.INTENT_NETWORK_FINISHSCANIVYROOM);
            filter.addAction(IvyMessages.INTENT_NETWORK_DISCOVERYWIFIP2P);
            registerReceiver(mNetworkReceiver, filter);
        }
		
		startService(new Intent("com.ivyshare.IVYNETWORKSERVICE_START"));
	}
	
    private void updateTableColor() {
    	mTabText1.setTextColor(getResources().getColor(R.color.table_color_normal));
    	mTabText2.setTextColor(getResources().getColor(R.color.table_color_normal));
    	mTabText3.setTextColor(getResources().getColor(R.color.table_color_normal));
    	
    	switch (mCurrentTabIndex) {
    		case PAGE_FREESHARE:
    	    	mTabText3.setTextColor(getResources().getColor(R.color.table_color_selected));
    	    	break;
    		case PAGE_CONTACTS:
    	    	mTabText1.setTextColor(getResources().getColor(R.color.table_color_selected));
    	    	break;
    		case PAGE_SESSION:
    	    	mTabText2.setTextColor(getResources().getColor(R.color.table_color_selected));
    	    	break;
    	}
    }

	private void initViewPager() {
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
						animation = new TranslateAnimation(oneStep, 0, 0, 0);
					} else if (mCurrentTabIndex == 2) {
						animation = new TranslateAnimation(twoStep, 0, 0, 0);
					}
					break;
				case 1:
					if (mCurrentTabIndex == 0) {
						animation = new TranslateAnimation(0, oneStep, 0, 0);
					} else if (mCurrentTabIndex == 2) {
						animation = new TranslateAnimation(twoStep, oneStep, 0, 0);
					}
					break;
				case 2:
					if (mCurrentTabIndex == 0) {
						animation = new TranslateAnimation(0, twoStep, 0, 0);
					} else if (mCurrentTabIndex == 1) {
						animation = new TranslateAnimation(oneStep, twoStep, 0, 0);
					}
					break;
				}
				
				mCurrentTabIndex = arg0;
				animation.setFillAfter(true);// True: image stop position
				animation.setDuration(200);
				mTabCursorImage.startAnimation(animation);

				updateTableColor();
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}
		});

		changePagerByIntent(getIntent());
	}
	
	private void initTabTitleBar(){
		mTabCursorImage = (ImageView) findViewById(R.id.cursor);
		mCursorWidth = BitmapFactory.decodeResource(getResources(), R.drawable.tab_titlebar_cursor)
				.getWidth();// 获取图片宽度
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;// 获取分辨率宽度
		mOffect = (screenW / PAGE_COUNT - mCursorWidth) / 2;// 计算偏移量
		Matrix matrix = new Matrix();
		matrix.postTranslate(mOffect, 0);
		mTabCursorImage.setImageMatrix(matrix);// 设置动画初始位置
		oneStep = screenW / PAGE_COUNT;// tab1 -> tab2 offect
		twoStep = oneStep * 2;// tab1 -> tab3 offect
	}

	private void initNetworkSetting() {
	    mWifiSettings = (ImageButton)actionbar.findViewById(R.id.btn_right);
	    mWifiSettings.setVisibility(View.VISIBLE);
	    mWifiSwitchingBar = (ProgressBar) actionbar.findViewById(R.id.switching_bar);

	    mWifiSettings.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	UserTrace.addTrace(UserTrace.ACTION_WIFI_SETTING);
	            startTarget(NetworkSettingActivity.class);
	        }
	    });

	    mWifiSwitchingBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTarget(NetworkSettingActivity.class);
            }
        });
	}

	/**
	 * popup window menu
	 */
	private void initPopMenu() {
	    mWifiSettings = (ImageButton) actionbar
				.findViewById(R.id.btn_right);
	    mWifiSettings.setVisibility(View.VISIBLE);
	    mWifiSwitchingBar = (ProgressBar) actionbar.findViewById(R.id.switching_bar);
	    
	    mNetworkMenuAdapter = new NetworkMenuAdapter(this);
		mPopMenu = new SimplePopMenu(this, mNetworkMenuAdapter);
		mWifiSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mPopMenu.show(v);
			}
		});
		
		mPopMenu.setOnPopMenuItemClickListener(new OnPopMenuItemClickListener() {
			@Override
			public void onPopMenuItemClick(int index, String menuText) {
				// TODO Auto-generated method stub
/*
			    if (mNetworkMenuAdapter.isChecked(index)){
			        return;
			    }
//	             mWifiSettings.setEnabled(false);
			    onWifiSwitching(true);

	             mNetworkMenuAdapter.setItemCheckd(index);
				switch (index){
	            case 0:
	                NetworkUtils.getInstance().enableWifiManually();
	                mMaunallyMode = sModeWifi;
	                break;
	                
	            case 1:
	                NetworkUtils.getInstance().enableWifiApManually(false);
	                mMaunallyMode = sModeWifiAp;
	                break;
	                
	            case 2:
	                NetworkUtils.getInstance().enableWifiApManually(true);
	                mMaunallyMode = sModeWifiAp;
	                break;
				} */
			}
		});
		

	}

	/**
	 * if wifi mode switching, show the progress bar
	 * @param switching
	 */
	private void onWifiSwitching(boolean switching) {
	    if (mWifiSwitchingBar == null || mWifiSettings == null) {
	        return;
	    }

		if(switching){
			mWifiSwitchingBar.setVisibility(View.VISIBLE);
			mWifiSettings.setVisibility(View.GONE);
		}else {
			mWifiSwitchingBar.setVisibility(View.GONE);
			mWifiSettings.setVisibility(View.VISIBLE);
		}
	}

    private void initActionBarForHighVersion() {
        //----------------Action menu, if adk version >= 14, should check has menu hard key
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();
            if (!hasMenu) {
                final SettingMenuAdapter settingMenuAdapter = new SettingMenuAdapter(
                        this);
                final SimplePopMenu actionPopMenu = new SimplePopMenu(this,
                        settingMenuAdapter);
                final ImageButton actionMenuBtn = (ImageButton) findViewById(R.id.btn_menu);
                actionMenuBtn.setVisibility(View.VISIBLE);
                actionMenuBtn.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        actionPopMenu.show(v);
                    }
                });

                actionPopMenu
                .setOnPopMenuItemClickListener(new OnPopMenuItemClickListener() {
                    @Override
                    public void onPopMenuItemClick(int index,
                            String menuText) {
                        // TODO Auto-generated method stub
                        onMenuSelected(settingMenuAdapter.mMenuId[index]);
                    }
                });
            }
        }
    }

	/**
	 * if configuration change, should reset the cursor's position
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		int oldOneStep = oneStep;
		
		initTabTitleBar();
		
		TranslateAnimation animation = null;
		switch (mCurrentTabIndex) {
		case 0:
			animation = new TranslateAnimation(oneStep, 0, 0, 0);
			break;
		case 1:
			animation = new TranslateAnimation(oldOneStep, oneStep, 0, 0);
			break;
		case 2:
			animation = new TranslateAnimation(2 * oldOneStep, twoStep, 0, 0);
			break;
		}
		
		animation.setFillAfter(true);// True: image stop position
		animation.setDuration(200);
		mTabCursorImage.startAnimation(animation);
		
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * init tab text layout
	 */
	private void initTabText(){
		mTabText1 = (TextView) findViewById(R.id.tab_text1);
		mTabText2 = (TextView) findViewById(R.id.tab_text2);
		mTabText3 = (TextView) findViewById(R.id.tab_text3);
	
		mTabText1.setOnClickListener(new TabTextClickListener(0));
		mTabText2.setOnClickListener(new TabTextClickListener(1));
		mTabText3.setOnClickListener(new TabTextClickListener(2));

		if (PAGE_COUNT == 2) {
		    mTabText1.setVisibility(View.VISIBLE);
		    mTabText2.setVisibility(View.VISIBLE);
		    mTabText3.setVisibility(View.GONE);
		} else if (PAGE_COUNT == 3) {
		    mTabText1.setVisibility(View.VISIBLE);
            mTabText2.setVisibility(View.VISIBLE);
            mTabText3.setVisibility(View.VISIBLE);
		}
		
		mTabMessageIcon = (ImageView) findViewById(R.id.icon_message);
	}
	
	/**
	 * use this to set the tag message icon
	 * @param visible
	 */
	private void setMessageIconVisibility(boolean visible){
		if(visible){
			mTabMessageIcon.setVisibility(View.VISIBLE);
		}else {
			mTabMessageIcon.setVisibility(View.GONE);
		}
	}

	private void getPersonInformation() {
		TextView userName = ((TextView) actionbar.findViewById(R.id.text_info));
		userName.setText(mLocalSetting.getMySelf().mNickName);

		ImageView imageleft = (ImageView) actionbar.findViewById(R.id.btn_left);
		CommonUtils.getPersonPhoto(imageleft,mLocalSetting.getMySelf().mImage);
		imageleft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startTarget(UserEditActivity.class);
			}
		});
	}

	@Override
	public void onDestroy(){
	    doOnDestroy();
	    super.onDestroy();
	}

	private void doOnCreate() {
    	if (mImService != null) {
    		if(!mPersonReceiverRegister) {
				Log.d(TAG, "Register PersonReceiver and Init ContactFragment");

				mContactAdapter = new ContactAdapter(this, mImService);
				if (mContactFragment != null) {
					mContactFragment.setAdapter(mContactAdapter);
        			mContactFragment.adjustHeadPosition();
				}

				IntentFilter filter = new IntentFilter(IvyMessages.INTENT_PERSON);
				registerReceiver(mPersonReceiver, filter);

				mPersonReceiverRegister = true;
    		} else {
    			Log.d(TAG, "doOnCreate PersonReceiver has already been registered");
    		}
    	} else {
    		Log.d(TAG, "doOnCreate Service is null");
    	}

        IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();
        if (ivyNetService != null) {
            if (mContactAdapter != null
                    && ivyNetService.getConnectionState().getHotspotState()
                    	!= ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED) {
                mContactAdapter.setPointList(ivyNetService.getScanResult());
            }
            int state = ivyNetService.getConnectionState().getLastStateByFast();
            int type = ivyNetService.getConnectionState().getLastType();
            mNetworkState = state;
            if (mNetworkState != ConnectionState.CONNECTION_UNKNOWN) {
            	mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NETWORK_STATE_CHANGED, type, state, ivyNetService.getConnectionInfo().getSSID()));
            }
        }
	}
	
	private void doOnDestroy() {
		if (mPersonReceiverRegister) {
			unregisterReceiver(mPersonReceiver);
			mPersonReceiverRegister = false;
			Log.d(TAG, "UnRegister PersonReceiver");
		}

		unregisterReceiver(mNetworkReceiver);
	}

    private void doOnResume() {
        IvyNetService ivyNetService = IvyNetwork.getInstance().getIvyNetService();
        if (ivyNetService != null) {
            if (mContactAdapter != null
                    && ivyNetService.getConnectionState().getHotspotState() != ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED) {
                mContactAdapter.setPointList(ivyNetService.getScanResult());
                mContactAdapter.notifyDataSetChanged();
            }

        	int state = ivyNetService.getConnectionState().getLastStateByFast();
            int type = ivyNetService.getConnectionState().getLastType();
            
            if ((mNetworkState != ConnectionState.CONNECTION_UNKNOWN) && (state != mNetworkState)) {
            	mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NETWORK_STATE_CHANGED, type, state, ivyNetService.getConnectionInfo().getSSID()));
            }
        }

    	if (mImService != null) {
    		int state = mImService.getDaemonNotifaction().getNotificationState();
    		if (state == IvyMessages.NOTIFICATION_STATE_MESSAGE_ONE ||
    			state == IvyMessages.NOTIFICATION_STATE_MESSAGE_GROUP ||
    			state == IvyMessages.NOTIFICATION_STATE_MESSAGE_SOME ||
    			state == IvyMessages.NOTIFICATION_STATE_NONE) {
    			mImService.getDaemonNotifaction().startBackgroundNotification();
    		}

	    	if(!mMessageReceiverRegister) {
				Log.d(TAG, "Register MessageReceiver and Init SessionFragment and FreeShareFragment");

	    		mSessionAdapter = new SessionAdapter(MainPagerActivity.this, mImService.getSessionMessageListClone(), mImService);
	    		if (mSessionFragment != null) {
	    			mSessionFragment.setAdapter(mSessionAdapter);
	    		}
				changeSessionIcon();
	
				IntentFilter filter = new IntentFilter(IvyMessages.INTENT_MESSAGE);
				filter.setPriority(400);
				registerReceiver(mMessageReceiver, filter);

				IntentFilter filterGroup = new IntentFilter(IvyMessages.INTENT_GROUP_MESSAGE);
				filterGroup.setPriority(400);
				registerReceiver(mGroupMessageReceiver, filterGroup);
	
				mMessageReceiverRegister = true;

				mFreeShareAdapter = new FreeShareAdapter(MainPagerActivity.this, mImService.getFreeShareHistory());
				if (mFreeShareFragment != null) {
					mFreeShareFragment.setAdapter(mFreeShareAdapter);
				}
			} else {
				Log.d(TAG, "doOnResume MessageReceiver has already been registered");
			}
    	} else {
    		Log.d(TAG, "doOnResume Service is null");
    	}
    }

    private void doOnPause() {
    	if (mMessageReceiverRegister) {
    		unregisterReceiver(mMessageReceiver);
    		unregisterReceiver(mGroupMessageReceiver);
    		mMessageReceiverRegister = false;
    		Log.d(TAG, "UnRegister MessageReceiver");
    	}
    }

	private void doUpLine() {
		if (mImService == null) {
		    return;
		}

		mImService.upLine();
	}

	private void doDownLine() {
	    if (mImService == null) {
            return;
        }

        mImService.downLine();
	}

	private void changePagerByIntent(Intent intent) {
		int position = PAGE_CONTACTS;
		String uri = intent.getDataString();
		SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
		boolean hasInited = sp.getBoolean("INIT", false);

		if (uri != null) {
			int type = Integer.valueOf(uri.substring(uri.lastIndexOf('/')+1));
			switch (type) {
			case IvyMessages.NOTIFICATION_STATE_BACKGROUND:
				position = PAGE_CONTACTS;
				if (!hasInited) {
					Intent targetIntent = new Intent(this,UserEditActivity.class);
					startActivity(targetIntent);
					finish();
				}
				break;
			case IvyMessages.NOTIFICATION_STATE_MESSAGE_ONE:
				position = PAGE_CONTACTS;
				// start chat activity
				// uri type:custom://personkey/102
				Intent intentNew = new Intent(this, ChatActivity.class);
				String key = uri.substring(uri.indexOf('/')+2, uri.lastIndexOf('/'));
				intentNew.putExtra("chatpersonKey", key);
				startActivity(intentNew);
				break;
			case IvyMessages.NOTIFICATION_STATE_MESSAGE_GROUP:
				position = PAGE_CONTACTS;
				// start chat activity
				// uri type:custom://personkey/102
				Intent intentNew1 = new Intent(this, GroupChatActivity.class);
				intentNew1.putExtra("isBroadCast", true);
				intentNew1.putExtra("groupName", "");
				startActivity(intentNew1);
				break;
			case IvyMessages.NOTIFICATION_STATE_MESSAGE_SOME:
				position = PAGE_SESSION;
				break;
			}
		}

		if (mPagerView != null) {
			mPagerView.setCurrentItem(position);
			updateTableColor();
		}
	}

    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "Do onResume in onResume");
    	getPersonInformation();
    	doOnResume();
    }

    @Override
	public void onPause() {
		super.onPause();
		//if the pop menu is showing, dismiss it
		if (mPopMenu != null && mPopMenu.isShowing())
			mPopMenu.dismiss();
		doOnPause();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		mHandler.sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
	}

	public void onClick(View arg0) {
		switch(arg0.getId()) {
		case R.id.layout_user_setting:
			startTarget(UserEditActivity.class);
            break;
		case R.id.layout_system_setting:
			startTarget(SystemSettingActivity.class);
            break;
		case R.id.layout_about:
            startTarget(AboutActivity.class);
            break;
        /*case R.id.layout_update_version: {
        	UserTrace.addTrace(UserTrace.ACTION_CHECK_UPDATE);
            UpdateManager.getInstance(this).checkUpdate();
        }
        break;*/
        case R.id.layout_exit:
            if (mImService != null) {
                Log.d(TAG, "Call quit, and we will downline");
                mImService.downLine();
            }
            finish();

            stopService(new Intent("com.ivyshare.IMSERVICE_START"));
            stopService(new Intent("com.ivyshare.IVYNETWORKSERVICE_START"));

    		System.exit(0);
        break;
        }
	}

    private void startTarget(Class targetActivity) {
        Intent intent = new Intent(MainPagerActivity.this, targetActivity);
        startActivity(intent);
    }

	private void changeSessionIcon() {
		setMessageIconVisibility(false);
		if (mSessionAdapter != null) {
			if (mSessionAdapter.getUnReadCount() > 0) {
				setMessageIconVisibility(true);
			}
		}
	}

	public void setFragment(int pos, Fragment fragment) {
		switch(pos) {
			case PAGE_SESSION:
				mSessionFragment = (SessionFragment)fragment;
				if (mSessionAdapter != null) {
					mSessionFragment.setAdapter(mSessionAdapter);
				}
				break;
			case PAGE_CONTACTS:
				mContactFragment = (ContactFragment)fragment;
				if (mContactAdapter != null) {
					mContactFragment.setAdapter(mContactAdapter);
				}
				break;
			case PAGE_FREESHARE:
				mFreeShareFragment = (FreeShareFragment)fragment;
				if (mFreeShareAdapter != null) {
					mFreeShareFragment.setAdapter(mFreeShareAdapter);
				}
				break;
		}
	}

	private class PersonBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
        	int type = intent.getIntExtra(IvyMessages.PARAMETER_PERSON_TYPE, 0);
        	if (mImService != null) {
            	if (mContactAdapter != null) {
            		mContactAdapter.changeList(mImService.getPersonListClone());
            		mContactAdapter.notifyDataSetChanged();
            		if (mContactFragment != null) {
            			mContactFragment.showNoContentOrList();
            			mContactFragment.adjustHeadPosition();
            		}
            	}
        	}
        	if (mSessionAdapter != null) { // need to update session adapter when person change.
        		mSessionAdapter.notifyDataSetChanged();
        		if (mSessionFragment != null) {
        			mSessionFragment.showNoContentOrList();
        		}
        		changeSessionIcon();
        	}
        }
	}

	private class MessageBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
        	//int messageType = intent.getIntExtra(ImService.INTENT_MESSAGE_TYPE, 0);
        	int messageState = intent.getIntExtra(IvyMessages.PARAMETER_MESSGAE_STATE, 0);
        	boolean blnLocalUser = intent.getBooleanExtra(IvyMessages.PARAMETER_MESSAGE_SELF, true);
        	if (!blnLocalUser && messageState == Table_Message.STATE_OK && mImService != null) {
        		mImService.getDaemonNotifaction().addAndNotify(intent);
        	}

        	if (mImService != null && mSessionAdapter != null) {
        		mSessionAdapter.ChangeList(mImService.getSessionMessageListClone());
        		mSessionAdapter.notifyDataSetChanged();
        		if (mSessionFragment != null) {
        			mSessionFragment.showNoContentOrList();
        		}
        	}

        	// don't pass the message to next receiver
			abortBroadcast();
        }
	}
	
	private class GroupMessageBroadCastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
        	int messageState = intent.getIntExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_STATE, 0);
        	boolean blnLocalUser = intent.getBooleanExtra(IvyMessages.PARAMETER_GROUP_MESSAGE_SELF, true);
        	if (!blnLocalUser && messageState == Table_Message.STATE_OK && mImService != null) {
        		mImService.getDaemonNotifaction().addGroupAndNotify(intent);
        	}

        	if (mImService != null && mSessionAdapter != null) {
        		mSessionAdapter.ChangeList(mImService.getSessionMessageListClone());
        		mSessionAdapter.notifyDataSetChanged();
        		if (mSessionFragment != null) {
        			mSessionFragment.showNoContentOrList();
        		}
        	}

        	// don't pass the message to next receiver
			abortBroadcast();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if(onMenuSelected(item.getItemId()))
	    	return true;
	    return super.onOptionsItemSelected(item);
	}

	private boolean onMenuSelected(int menuId) {
		switch (menuId) {
		case R.id.action_usersetting:
			startTarget(UserEditActivity.class);
			return true;
		case R.id.action_systemsetting:
			startTarget(SystemSettingActivity.class);
			return true;
		/*case R.id.action_update:
			UpdateManager.getInstance(this).checkUpdate();
			return true;*/
		case R.id.action_quit: {
			if (mImService != null) {
				Log.d(TAG, "Call quit, and we will downline");
				mImService.downLine();
			}
			finish();
			stopService(new Intent("com.ivyshare.IMSERVICE_START"));
			stopService(new Intent("com.ivyshare.IVYNETWORKSERVICE_START"));
			return true;
		}
		default:
			return false;
		}
	}
	
	@Override
	public void onBackPressed() {
		// If side menu shown, close it when back key pressed
//		if (mMenuHorizontalScrollView.onBackPressed())
//			return;

		super.onBackPressed();
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

//		private int mCurrentPage = 0;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Log.d(TAG, "SectionsPagerAdapter getItem " + position);
            switch (position) {
	            case PAGE_CONTACTS:
	            	return new ContactFragment();
	            case PAGE_SESSION:
	            	return new SessionFragment();
	            case PAGE_FREESHARE:
	            	return new FreeShareFragment();
            }
			return null;
		}

		@Override
		public int getCount() {
			return PAGE_COUNT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PAGE_SESSION:
				return getString(R.string.tab_session);
			case PAGE_CONTACTS:
				return getString(R.string.tab_contacts);
			case PAGE_FREESHARE:
				return getString(R.string.tab_freeshare);
			}
			return null;
		}			
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


    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(IvyMessages.INTENT_NETWORK_STATECHANGE)) {
            	int type = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_TYPE, 0);
                int state = intent.getIntExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_STATE, 0);
                String ssid = intent.getStringExtra(IvyMessages.PARAMETER_NETWORK_STATECHANGE_SSID);

                mNetworkState = state;
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NETWORK_STATE_CHANGED, type, state, ssid));

            } else if (action.equals(IvyMessages.INTENT_NETWORK_FINISHSCANIVYROOM)) {
                boolean isclear = intent.getBooleanExtra(IvyMessages.PARAMETER_NETWORK_FINISHSCANIVYROOM_ISCLEAR, false);
                if (isclear) {
                    mHandler.sendEmptyMessage(MESSAGE_NETWORK_CLEAR_IVYROOM);
                } else {
                    mHandler.sendEmptyMessage(MESSAGE_NETWORK_SCAN_FINISH);    
                }
            } else if (action.equals(IvyMessages.INTENT_NETWORK_DISCOVERYWIFIP2P)) {
                mHandler.sendEmptyMessage(MESSAGE_NETWORK_DISCOVERYWIFIP2P);
            }
        }
        
    }
}
