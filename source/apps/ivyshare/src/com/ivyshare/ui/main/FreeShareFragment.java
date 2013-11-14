package com.ivyshare.ui.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.ivyshare.R;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.ui.chat.abstractchat.AbstractChatActivity;
import com.ivyshare.ui.chat.abstractchat.ApplicationActivity;
import com.ivyshare.ui.chat.abstractchat.FileSelectActivity;
import com.ivyshare.util.CommonUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FreeShareFragment extends Fragment {

	public static final int REQUEST_CODE_APP_SELECT = 3001;
	public static final int REQUEST_CODE_PICK_CONTACT = 3002;
	public static final int REQUEST_CODE_PICK_IMAGE = 3003;
	public static final int REQUEST_CODE_PICK_AUDIO = 3004;
	public static final int REQUEST_CODE_PICK_VIDEO = 3005;
	public static final int REQUEST_CODE_PICK_FILES = 3006;

	private static final String TAG = FreeShareFragment.class.getSimpleName();

    private String [] mTabTitles;
    private TypedArray mTabIcons;

	private MainPagerActivity mActivity = null;
    private ListView mListView = null;
    private GridView mGridView = null;
    private View mNoContentLayout = null;

    private SimpleAdapter mGridAdapter = null;
    private BaseAdapter mAdapter = null;

    public void onAttach(Activity activity) {
    	Log.d(MainPagerActivity.TAG, TAG + " onAttach");
        super.onAttach(activity);
        mActivity = (MainPagerActivity)activity;
        mActivity.setFragment(MainPagerActivity.PAGE_FREESHARE, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	Log.d(MainPagerActivity.TAG, TAG + " onCreateView");
    	View rootView = inflater.inflate(R.layout.page_freeshare, container, false);

		mTabTitles = this.getResources().getStringArray(R.array.mTabTitles_string);
		mTabIcons = this.getResources().obtainTypedArray(R.array.mTabIcon_list);

    	mNoContentLayout = (View)rootView.findViewById(R.id.no_content_layout);
		TextView noSessionText = (TextView) mNoContentLayout.findViewById(R.id.no_content_text);
		noSessionText.setText(R.string.no_freeshare);
        mListView = (ListView)rootView.findViewById(R.id.list_freeshare);
		mGridView = (GridView)rootView.findViewById(R.id.fileSelect);

		ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();
	    for (int i=0; i<mTabTitles.length; i++) {
	        HashMap<String, Object> map = new HashMap<String, Object>();
	        map.put("ItemImage",mTabIcons.getResourceId(i, 0));
	        map.put("ItemText", mTabTitles[i]);
	        lstImageItem.add(map);
	    }

		mGridAdapter = new SimpleAdapter(mActivity, lstImageItem,
                R.layout.chat_file_select,
                new String[] {"ItemImage","ItemText"},
                new int[] {R.id.itemImage,R.id.itemText});
		mGridView.setAdapter(mGridAdapter);
		mGridView.setOnItemClickListener(new ItemClickListener());

        if (mAdapter != null) {
        	Log.d(MainPagerActivity.TAG, TAG + " onCreateView setAdapter OK");
        	mListView.setAdapter(mAdapter);
        } else {
        	Log.d(MainPagerActivity.TAG, TAG + " onCreateView setAdapter mAdapter is null");
        }

        showNoContentOrList();
		return rootView;
    }

    public void setAdapter(BaseAdapter adapter) {
    	mAdapter = adapter;
    	if (mListView != null) {
    		Log.d(MainPagerActivity.TAG, TAG + " setAdapter setAdapter OK");
    		mListView.setAdapter(adapter);
    	} else {
    		Log.d(MainPagerActivity.TAG, TAG + " setAdapter setAdapter mListView is null");
    	}
    	showNoContentOrList();
    }

    public void showNoContentOrList() {
    	if (mListView == null || mNoContentLayout == null) {
    		return;
    	}
    	if (mAdapter == null || mAdapter.getCount() == 0) {
			mListView.setVisibility(View.GONE);
			mNoContentLayout.setVisibility(View.VISIBLE);
    	} else {
			mListView.setVisibility(View.VISIBLE);
			mNoContentLayout.setVisibility(View.GONE);
    	}
    }

    class  ItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            switch (arg2) {
                case 0:
                    Intent apkIntent = new Intent();
                    apkIntent.setClass(mActivity, ApplicationActivity.class);
                    startActivityForResult(apkIntent, REQUEST_CODE_APP_SELECT);
                    break;
                case 1:
                    try {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    imageIntent.setType(CommonUtils.MIMETYPE_ALLIMAGES);
                    startActivityForResult(imageIntent, REQUEST_CODE_PICK_IMAGE);
                    break;
                case 3:
                    Intent audioIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    audioIntent.setType(CommonUtils.MIMETYPE_ALLAUDIOS);
                    startActivityForResult(audioIntent, REQUEST_CODE_PICK_AUDIO);
                    break;
                case 4:
                    Intent videoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    videoIntent.setType(CommonUtils.MIMETYPE_ALLVIDEOS);
                    startActivityForResult(videoIntent, REQUEST_CODE_PICK_VIDEO);
                    break;
                case 5:
                    Intent filesIntent = new Intent();
                    filesIntent.setClass(mActivity, FileSelectActivity.class);
                    startActivityForResult(filesIntent, REQUEST_CODE_PICK_FILES);
                    break;
            }
        }
    }

    private void startShareActivity(Uri uri, String type) {
    	Intent intent = new Intent(Intent.ACTION_SEND);
    	intent.putExtra(Intent.EXTRA_STREAM, uri);
    	intent.setType(type);
        Intent directIntent = new Intent(intent);
        directIntent.setComponent(new ComponentName("com.ivyshare","com.ivyshare.ui.share.SendSelectActivity"));
        try{
            startActivity(directIntent);
        } catch(ActivityNotFoundException e){
            startActivity(intent);
        }
    }
    
    private void startShareActivitys(ArrayList<String> pathList, String type) {
    	Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
    	intent.putStringArrayListExtra(Intent.EXTRA_STREAM, pathList);
    	intent.setType(type);
        Intent directIntent = new Intent(intent);
        directIntent.setComponent(new ComponentName("com.ivyshare","com.ivyshare.ui.share.SendSelectActivity"));
        try{
            startActivity(directIntent);
        } catch(ActivityNotFoundException e){
            startActivity(intent);
        }
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        // Photo was chosen (either new or existing from gallery), and cropped.
        case REQUEST_CODE_APP_SELECT:
        	ArrayList<String> path = data.getExtras().getStringArrayList("FilePathName");
			startShareActivitys(path, CommonUtils.MIMETYPE_APPLICATION);
        	break;
        case REQUEST_CODE_PICK_CONTACT:
        	startShareActivity(data.getData(), CommonUtils.MIMETYPE_VCARD);
            break;
        case REQUEST_CODE_PICK_IMAGE:
        	startShareActivity(data.getData(), CommonUtils.MIMETYPE_ALLIMAGES);
            break;
        case REQUEST_CODE_PICK_AUDIO:
        	startShareActivity(data.getData(), CommonUtils.MIMETYPE_ALLAUDIOS);
        	break;
        case REQUEST_CODE_PICK_VIDEO:
        	startShareActivity(data.getData(), CommonUtils.MIMETYPE_ALLVIDEOS);
        	break;
        case REQUEST_CODE_PICK_FILES:
        	/*String imgPaths = data.getExtras().getString("FilePathName");
        	startShareActivity(Uri.fromFile(new File(imgPaths)), CommonUtils.MIMETYPE_OTHERFILES );*/
        	ArrayList<String> paths = data.getExtras().getStringArrayList("FilePathName");
			startShareActivitys(paths, CommonUtils.MIMETYPE_OTHERFILES);
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
        	if (requestCode >= FreeShareFragment.REQUEST_CODE_APP_SELECT &&
        			requestCode <= FreeShareFragment.REQUEST_CODE_PICK_FILES) {
        		onResult(requestCode, resultCode, data);
        	}
        }
    }
}