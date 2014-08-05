package com.ivyshare.ui.chat.abstractchat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivyshare.R;
import com.ivyshare.ui.setting.BasePopMenuAdapter;
import com.ivyshare.util.IvyActivityBase;
import com.ivyshare.widget.SimplePopMenu;
import com.ivyshare.widget.SimplePopMenu.OnPopMenuItemClickListener;

public class FileSelectActivity extends IvyActivityBase 
implements OnItemClickListener, OnClickListener, View.OnLongClickListener {

	private ListView mFileListView;
    private ArrayAdapter<FileInfo> mAdapter;
    private ArrayList<FileInfo> mFileList = new ArrayList<FileInfo>();
    private ArrayList<String> mPathList = new ArrayList<String>();
    private View mUpLevel;
    private TextView mNavigationBarText;
    private String mCurrentPath;
    private String mRootPath = "/";
    private ImageView mImageleft;
    private PathChooseAdapter mPopMenuAdapter;
    private ImageButton mButtonMid;
	private ImageButton mButtonRight;
	private TextView mTextSelected;
	private FileBasket mFileBasket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mFileBasket = new FileBasket();
		setContentView(R.layout.file_explorer);
		View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.title_files);

        mImageleft = (ImageView)actionbar.findViewById(R.id.btn_left);
        mImageleft.setImageResource(R.drawable.ic_ac_document);
        mImageleft.setOnClickListener(mPopMenuListener);
        
        mButtonMid = ((ImageButton) actionbar.findViewById(R.id.btn_mid));
		mButtonMid.setImageResource(R.drawable.unselected_pressed);
		mButtonMid.setOnClickListener(this);
		mButtonMid.setOnLongClickListener(this);

		mButtonRight = ((ImageButton) actionbar.findViewById(R.id.btn_right));
		mButtonRight.setImageResource(R.drawable.ic_left_title_share);
		mButtonRight.setVisibility(View.VISIBLE);
		mButtonRight.setOnClickListener(this);
		mButtonRight.setOnLongClickListener(this);
		
		mTextSelected = ((TextView) actionbar
				.findViewById(R.id.center_text_info));
		mTextSelected.setVisibility(View.VISIBLE);

        mPopMenuAdapter = new PathChooseAdapter(FileSelectActivity.this);
        if (LocalSetting.mListVolume.size() > 0) {
            mCurrentPath = LocalSetting.mListVolume.get(0);
            mPopMenuAdapter.setItemCheckd(0);
        } else {
            mCurrentPath = getSdDirectory();
        }

        mFileListView = (ListView) findViewById(R.id.file_path_list);
        mAdapter = new FileListAdapter(this, R.layout.file_browser_item, mFileList);
        mFileListView.setAdapter(mAdapter);
        mFileListView.setOnItemClickListener(this);

        boolean sdCardReady = isSDCardReady();
        View noSdView = findViewById(R.id.sd_not_available_page);
        noSdView.setVisibility(sdCardReady ? View.GONE : View.VISIBLE);

        View navigationBar = findViewById(R.id.navigation_bar);
        navigationBar.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);
        mFileListView.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);

        mUpLevel = findViewById(R.id.path_up_level_button);
        mUpLevel.setOnClickListener(this);

        mNavigationBarText = (TextView)findViewById(R.id.current_path_view);

        View pane = findViewById(R.id.current_path_pane);
        pane.setOnClickListener(mPopMenuListener);

        RefreshCurrentpath();
	}
	
	private void setSelectItemText(int count) {
		if (0 == count) {
			mButtonMid.setVisibility(View.INVISIBLE);
			mTextSelected.setVisibility(View.INVISIBLE);
			//mTextSelected.setText(R.string.choose_files);
		} else {
			mButtonMid.setVisibility(View.VISIBLE);
			mTextSelected.setText(String.format(
					getString(R.string.selected_files), count));
			mTextSelected.setVisibility(View.VISIBLE);
		}
	}

	private View.OnClickListener mPopMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SimplePopMenu popMenu = new SimplePopMenu(FileSelectActivity.this, mPopMenuAdapter);
            popMenu.setOnPopMenuItemClickListener(new OnPopMenuItemClickListener() {
                @Override
                public void onPopMenuItemClick(int index, String menuText) {
                    mPopMenuAdapter.setItemCheckd(index);
                    refreshFileList(LocalSetting.mListVolume.get(index));
                    mPathList.add(mCurrentPath);
                }
            });
            popMenu.show(mNavigationBarText);
        }
    };
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mPathList.size() > 1) {
				mPathList.remove(mPathList.size() - 1);
				refreshFileList(mPathList.get(mPathList.size() -1));
				return true;
			}
			break;
		}
        return super.onKeyDown(keyCode, event);
    }

	public void RefreshCurrentpath() {
    	refreshFileList(mCurrentPath);
    	mPathList.add(mCurrentPath);
    }

    public boolean refreshFileList(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }

        mFileList.clear();

        File[] listFiles = file.listFiles();
        if (listFiles == null)
            return true;

        for (File child : listFiles) {
        	String absolutePath = child.getAbsolutePath();
            if (shouldShowFile(new File(absolutePath))) {
                FileInfo info = getFileInfo(child);
                if (info != null) {
                    	info.mSelected = isFileSelected(info.mFilePath);
                	mFileList.add(info);
                }
            }
        }

        Collections.sort(mFileList, new FileComparator());
        mAdapter.notifyDataSetChanged();
        showEmptyView(mFileList.size() == 0);

        mCurrentPath = path;
        updateNavigationPane();

        return true;
    }

    private void showEmptyView(boolean show) {
        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
        	emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private class FileComparator implements Comparator<FileInfo> {
        @Override
        public int compare(FileInfo info1, FileInfo info2) {
            if (info1.mIsDirecotry == info1.mIsDirecotry) {
                return info1.mFileName.compareToIgnoreCase(info2.mFileName);
            }
            return info1.mIsDirecotry ? -1 : 1;
        }
    }

    public static String getSdDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static boolean isSDCardReady() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static FileInfo getFileInfo(File file) {
        if (file == null || !file.exists())
            return null;

        FileInfo info = new FileInfo();
        info.mCanRead = file.canRead();
        info.mCanWrite = file.canWrite();
        info.mIsHidden = file.isHidden();
        info.mFileName = file.getName();
        info.mModifiedDate = file.lastModified();
        info.mIsDirecotry = file.isDirectory();
        info.mFilePath = file.getPath();
        info.mFileSize = file.length();

        if (info.mIsDirecotry) {
            int count = 0;
            File[] files = file.listFiles();
            if (files == null) {
                return null;
            }

            for (File child : files) {
                if (shouldShowFile(child)) {
                    count++;
                }
            }
            info.mCount = count;
        } else {
        	info.mFileSize = file.length();
        }
        return info;
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo info = mFileList.get(position);

        if (info == null) {
            return;
        }

        if (!info.mIsDirecotry) {
            info.mSelected = !info.mSelected;
            mAdapter.notifyDataSetChanged();
            onFileSelectedChanged(info);
            return;
        }
      
        refreshFileList(info.mFilePath);
        mPathList.add(mCurrentPath);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
        case R.id.path_up_level_button:
            onOperationUpLevel();
            break;
		case R.id.btn_mid:
			mFileBasket.removeAllFiles();
			RefreshCurrentpath();
			setSelectItemText(mFileBasket.getFilesCount());
			break;

		case R.id.btn_right: {
			if (mFileBasket.getFilesCount() > 0) {
				Intent data = new Intent();
                data.putStringArrayListExtra("FilePathName", mFileBasket.getFilesPathList());
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

    public static boolean shouldShowFile(File file) {
        if (file.isHidden())
            return false;

        if (file.getName().startsWith("."))
            return false;

        return true;
    }

    public boolean onOperationUpLevel() {
        if (!mCurrentPath.equals(mRootPath)) {
            refreshFileList(new File(mCurrentPath).getParent());
            mPathList.add(mCurrentPath);
            return true;
        }
        return false;
    }

    private void updateNavigationPane() {
        mUpLevel.setEnabled(mRootPath.equals(mCurrentPath) ? false : true);
        mNavigationBarText.setText(mCurrentPath);
    }

    public class PathChooseAdapter extends BasePopMenuAdapter {
        public PathChooseAdapter(Context context) {
            super(context);
        }

        @Override
        protected void initMenuType() {
            mMenuType = MENU_CHECKBOX;
        }

        @Override
        protected void initMenuItem() {
            int nSize = LocalSetting.mListVolume.size();
            mMenuItem = new String[nSize];
            for (int i=0; i<nSize; i++) {
                mMenuItem[i] = LocalSetting.mListVolume.get(i);
            }
        }
    }
    
    public boolean isFileSelected(String path) {
        return mFileBasket.isPathInBasket(path);
    }

    public void onFileSelectedChanged(FileInfo info) {
        if (info.mSelected) {
            mFileBasket.addToBasket(FileType.FileType_OtherFile, info.mFilePath, info.mFileName, null);
        } else {
            mFileBasket.removeFromBasket(info.mFilePath);
        }       
        setSelectItemText(mFileBasket.getFilesCount());
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
