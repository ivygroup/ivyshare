package com.ivyshare.ui.main;

import com.ivyshare.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SessionFragment extends Fragment {
	private static final String TAG = SessionFragment.class.getSimpleName();

	private MainPagerActivity mActivity = null;
	private BaseAdapter mAdapter = null;
    private ListView mListView = null;
    private View mNoContentLayout = null;

    public void onAttach(Activity activity) {
    	Log.d(MainPagerActivity.TAG, TAG + " onAttach");
        super.onAttach(activity);
        mActivity = (MainPagerActivity)activity;
        mActivity.setFragment(MainPagerActivity.PAGE_SESSION, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	Log.d(MainPagerActivity.TAG, TAG + " onCreateView");
    	View rootView = inflater.inflate(R.layout.page_session, container, false);

    	mNoContentLayout = (View)rootView.findViewById(R.id.no_content_layout);
		TextView noSessionText = (TextView) mNoContentLayout.findViewById(R.id.no_content_text);
		noSessionText.setText(R.string.no_session);
        mListView = (ListView)rootView.findViewById(R.id.list_session);

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
}

