package com.ivyshare.ui.main;

import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyActivityBase;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FreeShareHistoryActivity extends IvyActivityBase {

    private ListView mListView = null;
    private BaseAdapter mAdapter = null;
    private View mNoContentLayout = null;
    private ImageView mImageRight;
	private Handler mHandler;
	private static final int MESSAGE_SERVICE_CONNECTED = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_freeshare_history);

        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.tab_freeshare);

        ImageView imageleft= (ImageView)actionbar.findViewById(R.id.btn_left);
        imageleft.setImageResource(R.drawable.ic_left_title_share);
        imageleft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mImageRight= (ImageView)actionbar.findViewById(R.id.btn_right);
        mImageRight.setVisibility(View.GONE);
        mImageRight.setImageResource(R.drawable.ic_menu_delete);
        mImageRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                askDeletePersonMessage();
            }
        });

    	mNoContentLayout = (View)findViewById(R.id.no_content_layout);
		TextView noSessionText = (TextView) mNoContentLayout.findViewById(R.id.no_content_text);
		noSessionText.setText(R.string.no_freeshare);
        mListView = (ListView)findViewById(R.id.list_freeshare);

		mHandler = new Handler(this.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case MESSAGE_SERVICE_CONNECTED:
						updateHisotryList();
						break;
				}
			}
		};
	}

	private void updateHisotryList() {
		if (mImManager == null) {
			return;
		}
		mAdapter = new FreeShareAdapter(FreeShareHistoryActivity.this, mImManager.getFreeShareHistory());
		mListView.setAdapter(mAdapter);
		if (mAdapter.getCount() > 0) {
			mNoContentLayout.setVisibility(View.GONE);
		} else {
			mNoContentLayout.setVisibility(View.VISIBLE);
		}
		showDeleteButton();
	}

    private void showDeleteButton() {
        if (mAdapter.getCount() == 0) {
            mImageRight.setVisibility(View.GONE);
        } else {
            mImageRight.setVisibility(View.VISIBLE);
        }
    }

    private void askDeletePersonMessage() {
        if (mImManager != null) {
            Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(this).
                setTitle(R.string.clear_title).
                setMessage(R.string.makesure_clearhistory).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override 
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMesssages();
                    }
                }).setNegativeButton(R.string.cancel, null).create();
            alertDialog.show();
        }
    }

    protected void deleteMesssages() {
        if (mImManager != null) {
            mImManager.clearFreeShareHistory();
            UserTrace.addTrace(UserTrace.ACTION_DELETE_FREESHARE);
            updateHisotryList();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        mHandler.sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
    }
}