package com.ivyshare.ui.share;


import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.trace.UserTrace;

public class SendFragment extends Fragment implements OnClickListener{
	private static final String TAG = SendFragment.class.getSimpleName();

	private SendSelectActivity mActivity = null;
    private int mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
    private String mShareFilePath;
    //private String mShareFileDisplayName;

    private BaseAdapter mAdapter = null;
    private ListView mListView = null;
    private ImManager mImManager = null;
    
    private int mFileCount = 0;
    private Integer mListType;
    private List<String> mListPath;
    
    private Button mSendButton;

    public void onAttach(Activity activity) {
    	Log.d(SendSelectActivity.TAG, TAG + " onAttach");
        super.onAttach(activity);
        mFileCount = 0;
        mListType = ShareType.SHARE_TYPE_UNKNOWN;
        mListPath = null;
        mActivity = (SendSelectActivity)activity;
        mActivity.setFragment(SendSelectActivity.POS_SEND, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	Log.d(TAG, "SendFragment onCreateView");

		View rootView = inflater.inflate(R.layout.page_send, container, false);

        mListView = (ListView)rootView.findViewById(R.id.listPerson);
        if (mAdapter != null) {
        	mListView.setAdapter(mAdapter);
        }

        mSendButton = (Button)mActivity.findViewById(R.id.PushPull);
        mSendButton.setOnClickListener(this);

		return rootView;		
    }
    
    public void setAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        if (mListView != null) {
            mListView.setAdapter(adapter);
        }
    }
    
    public void setService(ImManager imManager) {
        mImManager = imManager;
    }
    
    public void setShareContent( String path, String name, int type){
        mShareFilePath = path;
        //mShareFileDisplayName = name;
        mShareFileType = type;
    }

    public void setSendContent(int count, List<String> path, Integer type){
        mFileCount = count;
        mListType = type;
        mListPath = path;
    }

    
    private void sendFile(Person person){
        mImManager.sendFile(person, "", mShareFilePath, ShareType.getFileType(mShareFileType));
    }
    @Override
    public void onClick(View arg0) {
        if (arg0.getId() == R.id.PushPull) {
            if (mImManager == null) {
                return;
            }
            if (mListPath == null || mListType == null || mFileCount == 0) {
                Log.d(TAG, "Count " + mFileCount + " listtype " + mListType + " listpath " + mListPath);
				if (mShareFilePath == null) {
					return;
				}
            }
            
            List<Person> list = ((SharePersonAdapter)mListView.getAdapter()).getSelectItem();
            if (list.size() <= 0 ) {
				Toast.makeText(MyApplication.getInstance(), R.string.share_to,Toast.LENGTH_SHORT).show();
                return;
            }

            if (mFileCount == 0 && mShareFilePath != null) {
                UserTrace.addShareTrace(UserTrace.ACTION_SHARE_MULTISEND, mShareFileType, SendSelectActivity.mLoadSource);
                for(Person tmp:list) {
                    Person obj = tmp;
                    sendFile(obj);
                }
			} else {
				for (int i = 0; i < mFileCount; i++) {
					UserTrace.addShareTrace(UserTrace.ACTION_SHARE_MULTISEND,
							mListType, SendSelectActivity.mLoadSource);
					for (Person person : list) {
						mImManager.sendFile(person, "", mListPath.get(i),
								ShareType.getFileType(mListType));
					}
				}
			}

            mActivity.finish();
        }
    }
}