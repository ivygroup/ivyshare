package com.ivyshare.ui.chat.abstractchat;

import java.io.File;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.im.Im.FileType;
import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.updatemanager.CurrentVersion;
import com.ivyshare.util.ImageLoader;
import com.ivyshare.util.IvyActivityBase;

public class ApplicationActivity extends IvyActivityBase implements
    ApplicationAdapter.SelectChangeListener, View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "ApplicationActivity";

    private GridView mGridView;
    private List<AppsInfo> mListAppInfo;
    private Handler mMainHandler;
    private ApplicationAdapter mAppAdapter;
    private Drawable mDefaultDrawable;
	private TextView mTextSelected;
    private ImageButton mButtonMid;
	private ImageButton mButtonRight;
    private Map<String, Set<Integer>> mMapAppPackageVersion;
    private Map<String, AppsInfo> mMapPackageApp;
    private ImageLoader mImageLoader;
    private Collator mCollator;
    private SearchAsyncTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.page_application);

        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.title_application);
        
		mTextSelected = ((TextView) actionbar
				.findViewById(R.id.center_text_info));
		mTextSelected.setVisibility(View.VISIBLE);

        ImageView imageleft= (ImageView)actionbar.findViewById(R.id.btn_left);
        imageleft.setImageResource(R.drawable.ic_ac_apps);
        imageleft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mGridView = (GridView) findViewById(R.id.gridview); 
      /*  mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= mListAppInfo.size()) {
                    return;
                }

                Intent data=new Intent();
                data.putExtra("FilePathName", mListAppInfo.get(position).sourceDir);
                data.putExtra("FileType", FileType.FileType_App.ordinal());
                setResult(Activity.RESULT_OK, data);
                finish();
            }
        });*/

//        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                if (position < 0 || position >= mListAppInfo.size()) {
//                    return false;
//                }
//
//                Intent intent = getPackageManager().
//                getLaunchIntentForPackage(mListAppInfo.get(position).packageName);
//                startActivity(intent);
//                return false;
//            }
//        });
        
		mButtonMid = ((ImageButton) actionbar.findViewById(R.id.btn_mid));
		mButtonMid.setImageResource(R.drawable.unselected_pressed);
		mButtonMid.setOnClickListener(this);
		mButtonMid.setOnLongClickListener(this);

		mButtonRight = ((ImageButton) actionbar.findViewById(R.id.btn_right));
		mButtonRight.setImageResource(R.drawable.ic_left_title_share);
		mButtonRight.setVisibility(View.VISIBLE);
		mButtonRight.setOnClickListener(this);
		mButtonRight.setOnLongClickListener(this);

        mDefaultDrawable = getResources().getDrawable(R.drawable.ic_file_type_apk);

        mMapAppPackageVersion = new HashMap<String, Set<Integer>>();
        mMapPackageApp = new HashMap<String, AppsInfo>();

        mListAppInfo = new ArrayList<AppsInfo>();

        mAppAdapter = new ApplicationAdapter(this, mListAppInfo, this);
        mGridView.setAdapter(mAppAdapter);

        mCollator = Collator.getInstance(Locale.getDefault());

        mMainHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                AppsInfo app = (AppsInfo)msg.obj;
                while (app.appLabel.length() > 0 && app.appLabel.charAt(0) == ' ') {
                	app.appLabel = app.appLabel.substring(1);
                }
                // if find a same version, dismiss it
                if (compareApkVersion(app) != 0) {
                	return;
                }
                if (app.packageName.compareToIgnoreCase(CurrentVersion.appPackName) == 0) {
                    mListAppInfo.add(0, app);
                } else {
                	int nSize = mListAppInfo.size();
                	int nPos = 0;
                	for (;nPos<nSize; nPos++) {
                		AppsInfo temp = mListAppInfo.get(nPos);
                		if (temp.packageName.compareToIgnoreCase(CurrentVersion.appPackName) == 0) {
                			continue;
                		}
//                		if (app.appLabel.compareToIgnoreCase(temp.appLabel) < 0) {
//                			break;
//                		}
	    				CollationKey key1 = mCollator.getCollationKey(app.appLabel);
	    				CollationKey key2 = mCollator.getCollationKey(temp.appLabel);
	    				if (key1.compareTo(key2) < 0) {
	    					break;
	    				}
                	}
                	mListAppInfo.add(nPos, app);
                    //mListAppInfo.add(app);
                }
                mAppAdapter.notifyDataSetChanged();
            }
        };

        mTask = new SearchAsyncTask();
        mTask.execute(0);
    }
    
	@Override
	public boolean onLongClick(View v) {
		int toastTextId = 0;
		switch (v.getId()) {
		case R.id.btn_mid:
			toastTextId = R.string.toast_unselect;
			break;
		case R.id.btn_right:
			toastTextId = R.string.toast_send;
			break;
		}
		Toast.makeText(this, toastTextId, Toast.LENGTH_SHORT).show();
		return false;
	}
    
	private void setSelectItemText(int count) {
		if (0 == count) {
			mButtonMid.setVisibility(View.INVISIBLE);
			//mTextSelected.setText(R.string.choose_app);
			mTextSelected.setVisibility(View.INVISIBLE);
		} else {
			mButtonMid.setVisibility(View.VISIBLE);
			mTextSelected.setText(String.format(
					getString(R.string.selected_files), count));
			mTextSelected.setVisibility(View.VISIBLE);
		}
	}

    @Override
	public void onSelectedChanged() {
		setSelectItemText(mAppAdapter.getSelectItemCount());
	}
    
    private int compareApkVersion(AppsInfo app) {
        if (mMapAppPackageVersion.containsKey(app.packageName)) {
            Set<Integer> set = mMapAppPackageVersion.get(app.packageName);
            if (set.contains(app.versionCode)) {
                return -1;
            }
            set.add(app.versionCode);
            app.appLabel += "_" + app.versionName;

            // once find another version, update the label
            if (mMapPackageApp.containsKey(app.packageName)) {
            	AppsInfo oldApp = mMapPackageApp.get(app.packageName);
            	oldApp.appLabel += "_" + oldApp.versionName;
            	mMapPackageApp.remove(app.packageName);
            }
        } else {
            Set<Integer> set = new HashSet<Integer>();
            set.add(app.versionCode);
            mMapAppPackageVersion.put(app.packageName, set);

            mMapPackageApp.put(app.packageName, app);
        }
        return 0;
    }

    public class SearchAsyncTask extends AsyncTask<Integer, Void, Integer> 
        implements ImageLoader.LoadFinishListener{

    	private boolean mQuiting = false;
        private void readFile(final File[] files){
        	if (mQuiting) {
        		return;
        	}
            if(files!=null && files.length>0){
                for(int i=0;i<files.length;i++) {
                	if (mQuiting) {
                		return;
                	}
                    if (files[i].isDirectory()) {
                        readFile(files[i].listFiles());
                    } else {
                        String path = files[i].getPath();
                        String suffix = path.substring(path.lastIndexOf('.')+1);
                        if (suffix.compareToIgnoreCase("APK") == 0) {
                        	if (mImageLoader != null) {
                                mImageLoader.loadImage(null, path, 0, FileType.FileType_App);
                        	}
                        }
                    }
                }
            }
        }

        private void queryInstalledAppInfo() {
            PackageManager pm = MyApplication.getInstance().getPackageManager();

            List<PackageInfo> listPackages = 
                    pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);

            for (PackageInfo packageinfo : listPackages) {
            	if (mQuiting) {
            		return;
            	}
                ApplicationInfo app = packageinfo.applicationInfo;
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                    AppsInfo appInfo = new AppsInfo();
                    appInfo.appLabel = (String)app.loadLabel(pm);
                    appInfo.appIcon = app.loadIcon(pm);
                    appInfo.packageName = app.packageName;
                    appInfo.sourceDir = app.sourceDir;
                    appInfo.type = AppsInfo.APP_INSTALLED;
                    appInfo.versionCode = packageinfo.versionCode;
                    appInfo.versionName = packageinfo.versionName;
                    mMainHandler.sendMessage(mMainHandler.obtainMessage(AppsInfo.APP_INSTALLED, appInfo));
                }
            }
        }

        public void quit() {
        	mQuiting = true;
            Log.d(TAG, "Quit SearchTask");
        }
        public SearchAsyncTask() {
        	mQuiting = false;
            mImageLoader = new ImageLoader(this);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            // first, find from packet manager
        	queryInstalledAppInfo();

            // second, find from files
            readFile(Environment.getExternalStorageDirectory().listFiles());
            return 0;
        }
        
        @Override  
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "Search Over");
        }

        @Override
        public boolean onAPKLoadFinished(ImageView view, Drawable drawable,
                String name, String packageName, int versionCode, String versionName, String path) {
            AppsInfo appInfo = new AppsInfo();
            if (drawable != null) {
                appInfo.appLabel = name;
                appInfo.appIcon = drawable;
                appInfo.packageName = packageName;
            } else {
                appInfo.appIcon = mDefaultDrawable;
                appInfo.appLabel = path.substring(path.lastIndexOf('/')+1);
                appInfo.packageName = "";
            }
            appInfo.sourceDir = path;
            appInfo.type = AppsInfo.APP_STORAGECARD;
            appInfo.versionCode = versionCode;
            appInfo.versionName = versionName;
            mMainHandler.sendMessage(mMainHandler.obtainMessage(AppsInfo.APP_STORAGECARD, appInfo));
            return true;
        }

        @Override
        public boolean onDrawableLoadFinished(ImageView view, Drawable drawable) {
            return false;
        }

        @Override
        public boolean onBitmapLoadFinished(ImageView view, Bitmap bitmap) {
            return false;
        }
    }

	@Override
	public void onDestroy(){
		if (mImageLoader != null) {
			mImageLoader.unInit();
			mImageLoader = null;
		}
		if (mTask != null) {
			mTask.quit();
			mTask.cancel(true);
			mTask = null;
		}
	    super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_mid:
			if (mAppAdapter != null) {
				mAppAdapter.disSelectAll();
			}
			break;

		case R.id.btn_right: {
			if (mAppAdapter.getSelectItemCount() > 0) {
				/*mAppAdapter
						.getSelectItems(NeedSendAppList.getInstance().mListAppInfo);

				Intent intent = new Intent();
				intent.setClass(this, SendActivity.class);
				startActivity(intent);*/
				Intent data = new Intent();
                data.putStringArrayListExtra("FilePathName", mAppAdapter.getFilesPathList());
                data.putExtra("FileType", FileType.FileType_App.ordinal());
                setResult(Activity.RESULT_OK, data);
                finish();
			} else {
				// TODO: Toast.
				Toast.makeText(this, R.string.choose_files, Toast.LENGTH_SHORT)
						.show();
			}
		}
			break;
		}

	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
