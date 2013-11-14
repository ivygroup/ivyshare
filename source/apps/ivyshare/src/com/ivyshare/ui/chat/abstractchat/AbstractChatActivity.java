package com.ivyshare.ui.chat.abstractchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ivyshare.MyApplication;
import com.ivyshare.R;
import com.ivyshare.engin.control.ChatMessage;
import com.ivyshare.engin.control.TranslateFileControl;
import com.ivyshare.engin.im.Im.FileType;
import com.ivyshare.engin.im.Person;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyActivityBase;

@SuppressLint("HandlerLeak")
public abstract class AbstractChatActivity extends IvyActivityBase implements OnClickListener,
    OnTouchListener, TranslateFileControl.TransProcessListener{
    private static final String TAG = AbstractChatActivity.class.getSimpleName();

    public static final int mSDKVersion = Build.VERSION.SDK_INT;
    public static final int mMinSDK = Build.VERSION_CODES.HONEYCOMB;

    private AdapterInterface mAdapterInterface;

    protected ChatListView mListView;
    private GridView mGridView;
    
    private View mChatInputTextView;
    private View mChatInputSayView;

    private static final int MESSAGE_SERVICE_CONNECTED = 0;
    private static final int MESSAGE_FILEPROCESS_CHANGED = 1;

    protected Map<Integer, Integer> mMapFileProcess; // id and process
    protected Set<Integer> mSetExpandMessage;

    private EditText mInputText;
    private Button mRecordButton;
    private RecorderPlayer mRecorderPlayer;
    private InputMethodManager mImManager;
    protected TextView mTitle;
    protected ImageView mImageLeft;
    private ImageView mImageRight;
    private PopupWindow  mPopupWindow;
    private PopupWindow mPopupUndoDelete;
    protected ChatMessage mMessage;
    private ImageView mVolumnImageView;
    private TextView mPopTextView;
    public static int mStatusBarHeight;
    public static int mViewFrameHeight;

    private static final int REQUEST_CODE_FILE_SELECT = 3001;
    private static final int PICK_CONTACT = 3002;
    private static final int PICK_IMAGE = 3003;
    private static final int PICK_AUDIO = 3004;
    private static final int PICK_VIDEO = 3005;
    private static final int PICK_FILES = 3006;

    private Handler mHandler;

    boolean isDisplayed = true;
    boolean mIsSetMovedText = false;

    protected MessageBroadCastReceiver mMessageReceiver = null;
    private boolean mMessageReceiverRegister = false;
    private String [] mTabTitles;
    private TypedArray mTabIcons;
    private TypedArray mVolumn;

    protected abstract AdapterInterface createAdapter();
    protected abstract boolean createMessageReceiver();
    protected abstract void clearUnReadMessage();
    protected abstract int SendMessage(FileType type, String content);
    protected abstract int SendMessage(FileType type, ArrayList<String> content);
    protected abstract boolean isReceiveMessage(Intent intent);
    protected abstract boolean needToScrollListView(Intent intent);
    protected abstract void changeData();
    protected abstract boolean onImServiceConnected();
    protected abstract void recoverMessage();
    protected abstract void deleteMesssages();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        mImManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mTabTitles = this.getResources().getStringArray(R.array.mTabTitles_string);
        mTabIcons = this.getResources().obtainTypedArray(R.array.mTabIcon_list);
        mVolumn = this.getResources().obtainTypedArray(R.array.record_volumn);

        // if recording,popup this layout,if not,dismiss it.
        View view = getLayoutInflater().inflate(R.layout.pop_record_layout,null);
        mPopupWindow = new PopupWindow(view, 300, 300, false);
        mVolumnImageView = (ImageView) view.findViewById(R.id.volumn);
        mPopTextView = (TextView) view.findViewById(R.id.move_to_cancel);

        View actionbar = (View)findViewById(R.id.layout_title);
        mTitle = ((TextView)actionbar.findViewById(R.id.text_info));

        mImageLeft = (ImageView)actionbar.findViewById(R.id.btn_left);

        mImageRight= (ImageView)actionbar.findViewById(R.id.btn_right);
        mImageRight.setVisibility(View.GONE);
        mImageRight.setImageResource(R.drawable.ic_menu_delete);
        mImageRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                askDeletePersonMessage();
            }
        });

        mListView = (ChatListView)findViewById(R.id.lv_chat);
        mGridView = (GridView) findViewById(R.id.fileSelect);

        View undoDelete = getLayoutInflater().inflate(R.layout.undo_delete,null);
        mPopupUndoDelete = new PopupWindow(undoDelete, LayoutParams.MATCH_PARENT, 100, true);
        mPopupUndoDelete.setOutsideTouchable(true);
        mPopupUndoDelete.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View undoButton = undoDelete.findViewById(R.id.action_button);
        undoButton.setOnClickListener(this);

        mChatInputTextView = View.inflate(this, R.layout.chat_input_text, null);        
        mChatInputSayView = View.inflate(this, R.layout.chat_input_say, null);

        ((LinearLayout)findViewById(R.id.chat_box)).addView(mChatInputTextView);        
        ((LinearLayout)findViewById(R.id.chat_box)).addView(mChatInputSayView);

        mChatInputSayView.setVisibility(View.VISIBLE);
        mChatInputTextView.setVisibility(View.GONE);

        ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();
        for (int i=0; i<mTabTitles.length; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("ItemImage",mTabIcons.getResourceId(i, 0));
            map.put("ItemText", mTabTitles[i]);
            lstImageItem.add(map);
        }

        SimpleAdapter saImageItems = new SimpleAdapter(this, lstImageItem,
                                                    R.layout.chat_file_select,
                                                    new String[] {"ItemImage","ItemText"},
                                                    new int[] {R.id.itemImage,R.id.itemText});
        mGridView.setAdapter(saImageItems);
        mGridView.setOnItemClickListener(new ItemClickListener());

        mInputText = (EditText)mChatInputTextView.findViewById(R.id.chat_input_text_input);
        mRecordButton = (Button)mChatInputSayView.findViewById(R.id.chat_input_say_saybtn);
        mRecordButton.setOnClickListener(this);
        mRecordButton.setOnTouchListener(this);
        
        ((ImageButton)findViewById(R.id.chat_input_text_converttosay)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.chat_input_say_converttotext)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.chat_input_text_type_select)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.chat_input_say_type_select)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.chat_input_text_send)).setOnClickListener(this);

        if (findViewById(R.id.chat_input_text_converttosay) == null) {
            Log.d(TAG, "Can't find the text");
        } else {
            Log.d(TAG, "Find the text.");
        }

        mMapFileProcess = new HashMap<Integer, Integer>();
        mSetExpandMessage = new HashSet<Integer>();

        mMessageReceiver = new MessageBroadCastReceiver();

        mHandler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_SERVICE_CONNECTED:
                        if (!onImServiceConnected()) {
                            finish();
                        }
                        doOnResume();
                        break;
                    case MESSAGE_FILEPROCESS_CHANGED:
                        if (!mMapFileProcess.containsKey(msg.arg1) || msg.arg2 > mMapFileProcess.get(msg.arg1)) {
                            mMapFileProcess.put(msg.arg1, msg.arg2);
                            if (mAdapterInterface != null) {
                                mAdapterInterface.notifyProcessChanged();
                            }
                        }
                        break;
                }
            }
        };

        //when user touch listview ,the soft input method will be hide and gridview will be visible
        mListView.setOnTouchListener(new OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	if (mAdapterInterface != null) {
            		mAdapterInterface.onTouchEvent(v, event);
            	}
                mImManager.hideSoftInputFromWindow(
                    ((EditText)findViewById(R.id.chat_input_text_input)).getWindowToken(), 0);
                if (mGridView.getVisibility() == View.VISIBLE) {
                    mGridView.setVisibility(View.GONE);
                }
                isDisplayed = false;
                return false;
            }
        });

        //touch input edittext ,gridview will gone.
        mInputText.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                if (mGridView.getVisibility() == View.VISIBLE) {
                    mGridView.setVisibility(View.GONE);
                }
                isDisplayed = false;
            }
        });
        
        mInputText.setOnKeyListener(mOnKeyListener);  

        mRecorderPlayer = new RecorderPlayer();
        mRecorderPlayer.setVMChangeListener(new RecorderPlayer.VMChangeListener(){
            @Override
            public int onVMChanged(int value) {
                mVolumnImageView.setImageResource(mVolumn.getResourceId(value, 0));
                return 0;
            }
        });
    }

    class  ItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

            switch (arg2) {
                case 0:
                    Intent apkIntent = new Intent();
                    apkIntent.setClass(AbstractChatActivity.this,ApplicationActivity.class);
                    startActivityForResult(apkIntent, REQUEST_CODE_FILE_SELECT);
                    break;
                case 1:
                    try {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                        startActivityForResult(intent, PICK_CONTACT);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    imageIntent.setType(CommonUtils.MIMETYPE_ALLIMAGES);
                    startActivityForResult(imageIntent, PICK_IMAGE);
                    break;
                case 3:
                    Intent audioIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    audioIntent.setType(CommonUtils.MIMETYPE_ALLAUDIOS);
                    startActivityForResult(audioIntent, PICK_AUDIO);
                    break;
                case 4:
                    Intent videoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    videoIntent.setType(CommonUtils.MIMETYPE_ALLVIDEOS);
                    startActivityForResult(videoIntent, PICK_VIDEO);
                    break;
                case 5:
                    Intent filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    filesIntent.setClass(AbstractChatActivity.this,FileSelectActivity.class);
                    startActivityForResult(filesIntent, PICK_FILES);
                    break;
            }
        }
    }

    private void doOnResume() {
        if (mImService != null) {
            clearUnReadMessage();

            if(!mMessageReceiverRegister) {
                mImService.getImListener().getTranslateFileControl().RegisterTransProcess(this);

                mAdapterInterface = createAdapter();
                mListView.setListViewCallBack(new ChatListView.ListViewCallBack() {
                    @Override
                    public void onChildDismissed(View v) {
                        mMessage = mAdapterInterface.handleSwipe(v);
                        if (mMessage != null) {
                            mPopupUndoDelete.showAtLocation(mListView, 
                                    Gravity.CENTER_HORIZONTAL|Gravity.TOP, 0, 
                                    mStatusBarHeight + mListView.getTop()+ mListView.getHeight() - mPopupUndoDelete.getHeight());
                        }
                    }
                    @Override
                    public boolean canDismissed(View v) {
                        return mAdapterInterface.canViewDismissed(v);
                    }
					@Override
					public void showCannotSwipe() {
						Toast.makeText(AbstractChatActivity.this, R.string.cannot_swipe_delete, Toast.LENGTH_SHORT).show();
					}
                });

                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                mStatusBarHeight = rect.top;
                mViewFrameHeight = rect.height();
                mListView.setStatusBarHeight(rect.top);
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                mListView.setSelection(mAdapterInterface.getCount());
                showDeleteButton();

                if (createMessageReceiver()) {
                    mMessageReceiverRegister = true;
                }
            }
        }
    }

    private void doOnPause() {
        if (mMessageReceiverRegister) {
            mImService.getImListener().getTranslateFileControl().UnRegisterTransProcess(this);

            unregisterReceiver(mMessageReceiver);
            mMessageReceiverRegister = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        doOnResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        doOnPause();
    }

    @Override
    public void onDestroy() {
        if (mAdapterInterface != null) {
            mAdapterInterface.unInit();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // Photo was chosen (either new or existing from gallery), and cropped.
                case REQUEST_CODE_FILE_SELECT:
                	ArrayList<String> path = data.getExtras().getStringArrayList("FilePathName");
                    //String path = data.getExtras().getString("FilePathName");
                    //int fileType = data.getExtras().getInt("FileType");
                    SendMessage(FileType.FileType_App, path);
                    break;
                case PICK_CONTACT:
                    String shareFilePath = CommonUtils.getVCardByUri(AbstractChatActivity.this, data.getData());
                    SendMessage(FileType.FileType_Contact, shareFilePath);
                    break;
                case PICK_IMAGE:
                    Uri uri = data.getData();
                    Cursor imageCursor = getContentResolver().query(uri, null, null, null, null);
                    imageCursor.moveToFirst();
                    String imgPath = imageCursor.getString(1); //image path
                    SendMessage(FileType.FileType_Picture, imgPath);
                    break;
                case PICK_AUDIO:
                    Cursor audioCursor = getContentResolver().query(data.getData(), null, null, null, null);
                    audioCursor.moveToFirst();
                    String audioPath = audioCursor.getString(1); //music path
                    SendMessage(FileType.FileType_Music, audioPath);
                    break;
                case PICK_VIDEO:
                    Cursor videoCursor = getContentResolver().query(data.getData(), null, null, null, null);
                    videoCursor.moveToFirst();
                    String videoPath = videoCursor.getString(1); //video path
                    SendMessage(FileType.FileType_Video, videoPath);
                    break;
                case PICK_FILES:
                	ArrayList<String> imgPaths = data.getExtras().getStringArrayList("FilePathName");
                	//String imgPaths = data.getExtras().getString("FilePathName");
                    SendMessage(FileType.FileType_OtherFile, imgPaths);
                    break;
            }
        }
    }

    @Override
    public void onClick(View arg0) {
        int Id = arg0.getId();
        if (Id == R.id.action_button) {
            mPopupUndoDelete.dismiss();
            if (mMessage != null) {
                recoverMessage();
            }
            return;
        }

        switch (Id) {
            case R.id.chat_input_text_converttosay:
                mImManager.hideSoftInputFromWindow(((EditText)findViewById(R.id.chat_input_text_input)).getWindowToken(), 0);
                mChatInputTextView.setVisibility(View.GONE);
                mChatInputSayView.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
                isDisplayed = false;
                break;
            case R.id.chat_input_say_converttotext:
                mChatInputSayView.setVisibility(View.GONE);
                mChatInputTextView.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
                isDisplayed = false;
                ((EditText)findViewById(R.id.chat_input_text_input)).requestFocus();
                mImManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                break;
            case R.id.chat_input_text_type_select:
            case R.id.chat_input_say_type_select:
                mImManager.hideSoftInputFromWindow(((EditText)findViewById(R.id.chat_input_text_input)).getWindowToken(), 0);
                isDisplayed = !isDisplayed;
                if (isDisplayed) {
                    mGridView.setVisibility(View.VISIBLE);
                } else {
                    mGridView.setVisibility(View.GONE);

                }

                break;
            case R.id.chat_input_text_send:
                mGridView.setVisibility(View.GONE);
                String str = mInputText.getText().toString();
                if (str.length() > 0) {
                    int ret = SendMessage(FileType.FileType_CommonMsg, str);
                    if (ret == 0) {
                        mInputText.setText("");
                    }
                }
                break;
            case R.id.chat_input_say_saybtn:
                mGridView.setVisibility(View.GONE);
                if (mRecorderPlayer.getSoundLength() > 0) {
                    SendMessage(FileType.FileType_Record, mRecorderPlayer.getSoundFile());
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
	View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				InputMethodManager imm = (InputMethodManager) v.getContext()
						.getSystemService(Context.INPUT_METHOD_SERVICE);

				if (imm.isActive()) {
					imm.hideSoftInputFromWindow(v.getApplicationWindowToken(),
							0);
					
	                String str = mInputText.getText().toString();
	                if (str.length() > 0) {
	                    int ret = SendMessage(FileType.FileType_CommonMsg, str);
	                    if (ret == 0) {
	                        mInputText.setText("");
	                    }
	                }
				}
				return true;
			}

			return false;
		}
	};
    

    @SuppressLint("NewApi")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.chat_input_say_saybtn:
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mRecordButton.setText(R.string.chat_say_stop);
                    mRecorderPlayer.startRecording(this);
                    mIsSetMovedText = false;
                    mPopTextView.setText(R.string.chat_say_moveup_to_cancel);
                    mPopupWindow.showAtLocation(mListView,Gravity.CENTER, 0, 0);

                 } else if (action == MotionEvent.ACTION_UP){
                    if (mPopupWindow.isShowing()) {
                        mPopupWindow.dismiss();
                    }
                    mRecorderPlayer.stopRecording();
                    mRecordButton.setText(R.string.chat_say_send);
                 } else if(action == MotionEvent.ACTION_MOVE) {
                    if (!mIsSetMovedText) {
                        if (event.getX() < 0 || event.getY() < 0) {
                            mPopTextView
                                    .setText(R.string.chat_say_release_to_cancel);
                            mIsSetMovedText = true;
                        }
                    }
                 }
                 break;
        }
        return false;
    }
    
    private class MessageBroadCastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent)
        {
            if (isReceiveMessage(intent)) {
                changeData();
                if (needToScrollListView(intent)) {
                    mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    mListView.setSelection(mAdapterInterface.getCount());
                }
                showDeleteButton();
                // only abort broadcast under this condition
                abortBroadcast();
            }
        }
    }

    private void showDeleteButton() {
        if (mAdapterInterface.getCount() == 0) {
            mImageRight.setVisibility(View.GONE);
        } else {
            mImageRight.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSendFileProcess(int id, Person to, String name,
            FileType fileType, long pos, long total) {
        int progress = (int)(pos*100/total);
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FILEPROCESS_CHANGED, id, progress));
    }

    @Override
    public void onReceiveProcess(int id, Person from, String name,
            FileType fileType, long pos, long total) {
        int progress = (int)(pos*100/total);
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FILEPROCESS_CHANGED, id, progress));
    }

    private void askDeletePersonMessage() {
        if (mImService != null) {
            Dialog alertDialog = CommonUtils.getMyAlertDialogBuilder(this).
                    setTitle(R.string.clear_title).
                    setMessage(R.string.makesure_clear).
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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        mHandler.sendEmptyMessage(MESSAGE_SERVICE_CONNECTED);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPopupUndoDelete.isShowing()) {
            mPopupUndoDelete.dismiss();
        }
    }
}
