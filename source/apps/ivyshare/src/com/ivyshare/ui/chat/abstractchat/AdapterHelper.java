package com.ivyshare.ui.chat.abstractchat;

import java.sql.Date;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.control.ChatMessage;
import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.control.PersonManager;
import com.ivy.ivyengine.data.Table_Message;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.R;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.APKCheck;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.ImageLoader;

public abstract class AdapterHelper implements View.OnClickListener, 
        ImageLoader.LoadFinishListener, APKCheck.CheckFinishListener, View.OnLongClickListener,
        View.OnTouchListener, AdapterInterface, RecorderPlayer.PlayChangeListener{
    private static final String TAG = AdapterHelper.class.getSimpleName();

    private Context mContext;
    private ImManager mImManager;
    private Map<Integer, Integer> mMapFileProcess;
    private Set<Integer> mSetExpandMessage;
    private ImageLoader mImageLoader;
    private APKCheck mAPKCheck;
    private TypedArray mVoiceLeft;
    private TypedArray mVoiceRight;
    ListView mListView;
    private PopupWindow mPopupWindow;
    private PopupWindow mResendPopupWindow;
    private ImageView mPopupImage;
    private View mPopupBackground;
    private LayoutInflater mInflater;
    private UserIconEnvironment mUserIconEnvironment;
    private ViewClass mRecordViewClass;
    private RecorderPlayer mRecordPlayer;

    private Handler mHandler;

    public static final int APK_ACTION_INSTALL 		= 0;
    public static final int APK_ACTION_VIEW			= 1;
    public static final int APK_ACTION_UPDATE		= 2;
    public static final int APK_ACTION_UNINSTALL	= 3;

    protected abstract Person getRemotePerson(ChatMessage message);
    protected abstract void notifyDataSetChanged();
    protected abstract int deleteMessage(ChatMessage message);
    protected abstract int resendMesssage(ChatMessage message);

    public AdapterHelper(Context context,
            ImManager imManager, Map<Integer, Integer> process, Set<Integer> expand, ListView view) {

        mContext = context;
        mImManager = imManager;
        mMapFileProcess = process;
        mSetExpandMessage = expand;
        mListView = view;
        mImageLoader = new ImageLoader(this);
        mAPKCheck = new APKCheck(this);
        
    	mVoiceLeft = mContext.getResources().obtainTypedArray(R.array.voice_play_left);
    	mVoiceRight = mContext.getResources().obtainTypedArray(R.array.voice_play_right);

        mHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (mPopupBackground != null) {
                    mPopupBackground.setBackgroundColor(mContext.getResources().getColor(R.color.transparent_black));
                }
            }
        };

        mRecordPlayer = new RecorderPlayer();
        mRecordPlayer.setPlayeChangedListener(this);

        mInflater = LayoutInflater.from(mContext);
        mUserIconEnvironment = LocalSetting.getInstance().getUserIconEnvironment();
    }

    public View newView() {
        View view = mInflater.inflate(R.layout.chat_listview_item, null);
        return view;
    }

    @SuppressLint("NewApi")
    public boolean needNewView(View view) {
        if (AbstractChatActivity.mSDKVersion >= AbstractChatActivity.mMinSDK &&
                (view.getTranslationX() != 0 || view.getTranslationY() != 0)) {
            return true;
        }
        return false;
    }

    class ButtonGroup {
        View mBtnGroup;
        Button mButton1;
        Button mButton2;
        Button mButton3;
        Button mButton4;
    }

    class PicContentGroup {
        View mPicContent;
        ImageView mPic;
        TextView mPicInfo;
    }

    class ProgressGroup {
        View mPgrsGroup;
        ProgressBar mProgressBar;
        TextView mProgressText;
    }

    class PromptGroup {
        View mPmptLayoutGroup;
        View mPmptGroup;
        ImageView mPromptImage;
        TextView mPromptText;
        TextView mPromptTimeText;
    }

    class ChatGroup {
        View mChatContent;        // the whole chat content include sub listitems
        TextView mMsgContent;   // message textview
        ImageView mRecord;        // Record Imageview
        PicContentGroup mPicContentGroup;

        PromptGroup mPromptGroup;
        ProgressGroup mProgressGroup;

        ChatGroup() {
            mPromptGroup = new PromptGroup();
            mProgressGroup = new ProgressGroup();
            mPicContentGroup = new PicContentGroup();
        }
    }

    class ItemGroup {
        View mChatView;
        View mPhotoView;
        ImageView mPhoto;
        ChatGroup mChatGroup;
        ButtonGroup mButtonGroup;
        
        ItemGroup() {
            mChatGroup = new ChatGroup();
            mButtonGroup = new ButtonGroup();
        }
    }
    
    class ViewClass {
        ItemGroup mGroupLeft;
        ItemGroup mGroupRight;
        ChatMessage mChatMessage;
        boolean mNeedRecovery;

        ViewClass() {
            mGroupLeft = new ItemGroup();
            mGroupRight = new ItemGroup();
            mNeedRecovery = false;
        }
    }

    public void setViewTag(View view, ChatMessage message) {
        ViewClass myClass = (ViewClass)view.getTag();
        if(myClass == null) {
            myClass = new ViewClass();
            ItemGroup listItem = myClass.mGroupLeft;
            listItem.mChatView = (View)view.findViewById(R.id.chat_layout_left);

            listItem.mPhotoView = (View)view.findViewById(R.id.chat_layout_left_photo);
            listItem.mPhoto = (ImageView)view.findViewById(R.id.chat_left_image);

            listItem.mChatGroup.mChatContent = (View)view.findViewById(R.id.chat_layout_left_item);
            listItem.mChatGroup.mMsgContent = (TextView)view.findViewById(R.id.chat_left_content);
            listItem.mChatGroup.mRecord = (ImageView)view.findViewById(R.id.chat_left_record);

            listItem.mChatGroup.mPicContentGroup.mPicContent = (View)view.findViewById(R.id.chat_layout_left_pic);
            listItem.mChatGroup.mPicContentGroup.mPic = (ImageView)view.findViewById(R.id.chat_left_pic);
            listItem.mChatGroup.mPicContentGroup.mPicInfo = (TextView)view.findViewById(R.id.chat_left_pic_prompt);

            listItem.mChatGroup.mProgressGroup.mPgrsGroup = (View)view.findViewById(R.id.chat_layout_left_progress);
            listItem.mChatGroup.mProgressGroup.mProgressBar = (ProgressBar)view.findViewById(R.id.chat_left_progress);
            listItem.mChatGroup.mProgressGroup.mProgressText = (TextView)view.findViewById(R.id.chat_left_progress_text);

            listItem.mChatGroup.mPromptGroup.mPmptLayoutGroup = (View)view.findViewById(R.id.chat_layout_left_log);
            listItem.mChatGroup.mPromptGroup.mPmptGroup = (View)view.findViewById(R.id.chat_layout_left_send_information);
            listItem.mChatGroup.mPromptGroup.mPromptImage = (ImageView)view.findViewById(R.id.chat_left_log_image);
            listItem.mChatGroup.mPromptGroup.mPromptText = (TextView)view.findViewById(R.id.chat_left_log_text);
            listItem.mChatGroup.mPromptGroup.mPromptTimeText = (TextView)view.findViewById(R.id.chat_left_time);

            listItem.mButtonGroup.mBtnGroup = (View)view.findViewById(R.id.chat_layout_left_button);
            listItem.mButtonGroup.mButton1 = (Button)view.findViewById(R.id.chat_left_button1);
            listItem.mButtonGroup.mButton2 = (Button)view.findViewById(R.id.chat_left_button2);
            listItem.mButtonGroup.mButton3 = (Button)view.findViewById(R.id.chat_left_button3);
            listItem.mButtonGroup.mButton4 = (Button)view.findViewById(R.id.chat_left_button4);
            
            listItem = myClass.mGroupRight;
            listItem.mChatView = (View)view.findViewById(R.id.chat_layout_right);

            listItem.mPhoto = (ImageView)view.findViewById(R.id.chat_right_image);
            listItem.mPhotoView = (View)view.findViewById(R.id.chat_layout_right_photo);

            listItem.mChatGroup.mChatContent = (View)view.findViewById(R.id.chat_layout_right_item);
            listItem.mChatGroup.mMsgContent = (TextView)view.findViewById(R.id.chat_right_content);
            listItem.mChatGroup.mRecord = (ImageView)view.findViewById(R.id.chat_right_record);
            
            listItem.mChatGroup.mPicContentGroup.mPicContent = (View)view.findViewById(R.id.chat_layout_right_pic);
            listItem.mChatGroup.mPicContentGroup.mPic = (ImageView)view.findViewById(R.id.chat_right_pic);
            listItem.mChatGroup.mPicContentGroup.mPicInfo = (TextView)view.findViewById(R.id.chat_right_pic_prompt);

            listItem.mChatGroup.mProgressGroup.mPgrsGroup = (View)view.findViewById(R.id.chat_layout_right_progress);
            listItem.mChatGroup.mProgressGroup.mProgressBar = (ProgressBar)view.findViewById(R.id.chat_right_progress);
            listItem.mChatGroup.mProgressGroup.mProgressText = (TextView)view.findViewById(R.id.chat_right_progress_text);

            listItem.mChatGroup.mPromptGroup.mPmptLayoutGroup = (View)view.findViewById(R.id.chat_layout_right_log);
            listItem.mChatGroup.mPromptGroup.mPmptGroup = (View)view.findViewById(R.id.chat_layout_right_send_information);
            listItem.mChatGroup.mPromptGroup.mPromptImage = (ImageView)view.findViewById(R.id.chat_right_log_image);
            listItem.mChatGroup.mPromptGroup.mPromptText = (TextView)view.findViewById(R.id.chat_right_log_text);
            listItem.mChatGroup.mPromptGroup.mPromptTimeText = (TextView) view.findViewById(R.id.chat_right_time);

            listItem.mButtonGroup.mBtnGroup = (View)view.findViewById(R.id.chat_layout_right_button);
            listItem.mButtonGroup.mButton1 = (Button)view.findViewById(R.id.chat_right_button1);
            listItem.mButtonGroup.mButton2 = (Button)view.findViewById(R.id.chat_right_button2);
            listItem.mButtonGroup.mButton3 = (Button)view.findViewById(R.id.chat_right_button3);
            listItem.mButtonGroup.mButton4 = (Button)view.findViewById(R.id.chat_right_button4);

            view.setTag(myClass);
        } else {
            if (myClass.mNeedRecovery) {
                view.setVisibility(View.VISIBLE);
                myClass.mNeedRecovery = false;
            }
        }
        myClass.mChatMessage = message;
    }


    public void initViewItem(View view) {
        ViewClass myClass = (ViewClass)view.getTag();
        ChatMessage message = myClass.mChatMessage;

        ItemGroup listItem = null;
        if (message.mDirect == Table_Message.DIRECT_LOCALUSER) {
            myClass.mGroupRight.mChatView.setVisibility(View.VISIBLE);
            myClass.mGroupLeft.mChatView.setVisibility(View.GONE);
            listItem = myClass.mGroupRight;
            setHeadIcon(listItem.mPhoto, LocalSetting.getInstance().getMySelf().mImage);
        } else {
            myClass.mGroupRight.mChatView.setVisibility(View.GONE);
            myClass.mGroupLeft.mChatView.setVisibility(View.VISIBLE);
            listItem = myClass.mGroupLeft;
            Person person = getRemotePerson(message);
            if (person != null) {
                setHeadIcon(listItem.mPhoto, person.mImage);
            }
        }

        listItem.mChatGroup.mChatContent.setClickable(false);
        listItem.mChatGroup.mMsgContent.setVisibility(View.GONE);
        listItem.mChatGroup.mRecord.setVisibility(View.GONE);
        listItem.mChatGroup.mPicContentGroup.mPicContent.setVisibility(View.GONE);

        listItem.mChatGroup.mProgressGroup.mPgrsGroup.setVisibility(View.GONE);
        listItem.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.GONE);
        listItem.mChatGroup.mPromptGroup.mPromptImage.setVisibility(View.GONE);

        listItem.mButtonGroup.mBtnGroup.setVisibility(View.GONE);
        listItem.mButtonGroup.mButton1.setVisibility(View.GONE);
        listItem.mButtonGroup.mButton2.setVisibility(View.GONE);
        listItem.mButtonGroup.mButton3.setVisibility(View.GONE);
        listItem.mButtonGroup.mButton4.setVisibility(View.GONE);

        switch (message.mType) {
        case FileType_CommonMsg:
            InitMessageContent(message, listItem);
            break;
        case FileType_App:
            InitAppContent(message, listItem, myClass);
            break;
        case FileType_Contact:
            InitContactContent(message, listItem);
            break;
        case FileType_Record:
            InitRecordContent(message, listItem);
            break;
        case FileType_Picture:
            InitPicContent(message, listItem, myClass);
            break;
        case FileType_Music:
            InitMusicContent(message, listItem);
            break;
        case FileType_Video:
            InitVideoContent(message, listItem);
            break;
        case FileType_OtherFile:
            InitOtherFileContent(message, listItem);
            break;
        }

        setMessageTime(message, listItem);

        if (message.mDirect == Table_Message.DIRECT_LOCALUSER) {
            SetLocalUserState(message, listItem);
        } else {
            SetRemoteUserState(message, listItem);
        }

        listItem.mPhotoView.setTag(message);			// for person icon on the left
        listItem.mPhotoView.setOnClickListener(this);

        listItem.mChatGroup.mChatContent.setTag(myClass);// for chat item
        listItem.mChatGroup.mChatContent.setOnClickListener(this);
        listItem.mChatGroup.mChatContent.setOnTouchListener(this);
        listItem.mChatGroup.mChatContent.setOnLongClickListener(this);

        listItem.mButtonGroup.mButton1.setTag(message);
        listItem.mButtonGroup.mButton1.setOnClickListener(this);
        listItem.mButtonGroup.mButton2.setTag(message);
        listItem.mButtonGroup.mButton2.setOnClickListener(this);
        listItem.mButtonGroup.mButton3.setTag(message);
        listItem.mButtonGroup.mButton3.setOnClickListener(this);
        listItem.mButtonGroup.mButton4.setTag(message);
        listItem.mButtonGroup.mButton4.setOnClickListener(this);
    }

    public void setHeadIcon(ImageView image, String headIcon) {
        if (mUserIconEnvironment.isExistHead(headIcon, -1)) {
            Bitmap bitmap = CommonUtils.DecodeBitmap(mUserIconEnvironment.getFriendHeadFullPath(headIcon), 256*256);
            image.setImageBitmap(bitmap);
        }
    }

    private void SetLocalUserState(ChatMessage message, ItemGroup item) {
        if (message.mState == Table_Message.STATE_WAITING) {
            item.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.VISIBLE);
            item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.wait_send);
        } else if (message.mState == Table_Message.STATE_BEGIN) {
            if (!mMapFileProcess.containsKey(message.mId)) {
                item.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.VISIBLE);
                item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.ready_send);
            } else {
                int progress = mMapFileProcess.get(message.mId);
                item.mChatGroup.mProgressGroup.mPgrsGroup.setVisibility(View.VISIBLE);
                item.mChatGroup.mProgressGroup.mProgressBar.setProgress(progress);
                item.mChatGroup.mProgressGroup.mProgressText.setText(progress + "%");
            }
        } else if (message.mState == Table_Message.STATE_OK) {
        } else if (message.mState == Table_Message.STATE_FAILED ||
                    message.mState == Table_Message.STATE_TIMEOUT) {
            item.mChatGroup.mPromptGroup.mPromptImage.setVisibility(View.VISIBLE);
            item.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.VISIBLE);
            if (message.mState == Table_Message.STATE_FAILED) {
                item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.send_failed);
            } else {
                item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.send_timeout);
            }

            item.mButtonGroup.mButton3.setVisibility(View.VISIBLE);
            item.mButtonGroup.mButton3.setText(R.string.btn_resend);

            item.mChatGroup.mChatContent.setClickable(true);
            if (mSetExpandMessage.contains(message.mId)) {
                //item.mButtonGroup.mBtnGroup.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "Unknown state " + message.mState);
        }
    }

    private void SetRemoteUserState(ChatMessage message, ItemGroup item) {
         if (message.mState == Table_Message.STATE_BEGIN) {
            if (!mMapFileProcess.containsKey(message.mId)) {
                item.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.VISIBLE);
                item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.ready_receive);
            } else {
                int progress = mMapFileProcess.get(message.mId);
                item.mChatGroup.mProgressGroup.mPgrsGroup.setVisibility(View.VISIBLE);
                item.mChatGroup.mProgressGroup.mProgressBar.setProgress(progress);
                item.mChatGroup.mProgressGroup.mProgressText.setText(progress + "%");
            }
        } else if (message.mState == Table_Message.STATE_OK) {
        } else if (message.mState == Table_Message.STATE_FAILED) {
            item.mChatGroup.mPromptGroup.mPromptImage.setVisibility(View.VISIBLE);
            item.mChatGroup.mPromptGroup.mPmptGroup.setVisibility(View.VISIBLE);
            item.mChatGroup.mPromptGroup.mPromptText.setText(R.string.receive_failed);
        } else {
            Log.d(TAG, "Unknown state " + message.mState);
        }
    }

    private void setMessageTime(ChatMessage message, ItemGroup item) {
        long time = message.mTime;
        Date today = new Date(System.currentTimeMillis());
        Date messageDay = new Date(time);
        if (today.getYear() == messageDay.getYear()
                && today.getMonth() == messageDay.getMonth()
                && today.getDate() == messageDay.getDate()) {
            item.mChatGroup.mPromptGroup.mPromptTimeText.setText(DateFormat.format("kk:mm", time).toString());
        } else {
            item.mChatGroup.mPromptGroup.mPromptTimeText.setText(DateFormat.format("MM-dd kk:mm", time).toString());
        }
    }
    
    private void setDefaultPicture(ItemGroup item, int imageid, String text) {
        item.mChatGroup.mPicContentGroup.mPicContent.setVisibility(View.VISIBLE);
        LayoutParams param = item.mChatGroup.mPicContentGroup.mPic.getLayoutParams();
        param.width = param.height = 80;
        item.mChatGroup.mPicContentGroup.mPic.setImageResource(imageid);
        item.mChatGroup.mPicContentGroup.mPic.setScaleType(ImageView.ScaleType.FIT_XY);
        item.mChatGroup.mPicContentGroup.mPic.setLayoutParams(param);
        item.mChatGroup.mPicContentGroup.mPicInfo.setText(CommonUtils.getFileNameByPath(text));
    }

    private void InitMessageContent(ChatMessage message, ItemGroup item) {
        item.mChatGroup.mMsgContent.setText(message.mContent);
        item.mChatGroup.mMsgContent.setVisibility(View.VISIBLE);
    }

    private void InitRecordContent(ChatMessage message, ItemGroup item) {
        item.mChatGroup.mRecord.setVisibility(View.VISIBLE);
    }

    private void InitAppContent(ChatMessage message, ItemGroup item, ViewClass myClass) {
        setDefaultPicture(item, R.drawable.ic_file_type_unknown_app, message.mContent);
        if (message.mDirect == Table_Message.DIRECT_LOCALUSER) {
            item.mChatGroup.mPicContentGroup.mPic.setTag(myClass);
            if (mImageLoader != null) {
                mImageLoader.loadImage(item.mChatGroup.mPicContentGroup.mPic, 
                        message.mContent, message.mId, message.mType);
            }
        } else {
            if (message.mState == Table_Message.STATE_OK) {
                item.mChatGroup.mPicContentGroup.mPic.setTag(myClass);
                if (mImageLoader != null) {
                    mImageLoader.loadImage(item.mChatGroup.mPicContentGroup.mPic, 
                            message.mContent, message.mId, message.mType);
                }
            }
        }
    }

    private void InitContactContent(ChatMessage message, ItemGroup item) {
        setDefaultPicture(item, R.drawable.ic_file_type_vcard, message.mContent);

        if (message.mDirect == Table_Message.DIRECT_LOCALUSER) {
        } else {
            if (message.mState == Table_Message.STATE_OK) {
                item.mButtonGroup.mButton1.setVisibility(View.VISIBLE);
                item.mButtonGroup.mButton2.setVisibility(View.VISIBLE);
                item.mButtonGroup.mButton1.setText(R.string.btn_view);
                item.mButtonGroup.mButton2.setText(R.string.btn_import);

                item.mChatGroup.mChatContent.setClickable(true);
                if (mSetExpandMessage.contains(message.mId)) {
                   // item.mButtonGroup.mBtnGroup.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    private void InitPicContent(ChatMessage message, ItemGroup item, ViewClass myClass) {
        setDefaultPicture(item, R.drawable.ic_file_type_image, message.mContent);

        if (message.mDirect == Table_Message.DIRECT_LOCALUSER) {
            item.mChatGroup.mPicContentGroup.mPic.setTag(myClass);
            if (mImageLoader != null) {
                mImageLoader.loadImage(item.mChatGroup.mPicContentGroup.mPic, 
                        message.mContent, message.mId, message.mType);
            }
        } else {
            if (message.mState == Table_Message.STATE_OK) {
                item.mChatGroup.mPicContentGroup.mPic.setTag(myClass);
                if (mImageLoader != null) {
                    mImageLoader.loadImage(item.mChatGroup.mPicContentGroup.mPic, 
                            message.mContent, message.mId, message.mType);
                }
            }
        }
    }

    private void InitMusicContent(ChatMessage message, ItemGroup item) {
        setDefaultPicture(item, R.drawable.ic_file_type_music, message.mContent);
    }

    private void InitVideoContent(ChatMessage message, ItemGroup item) {
        setDefaultPicture(item, R.drawable.ic_file_type_video, message.mContent);
    }

    private void InitOtherFileContent(ChatMessage message, ItemGroup item) {
        setDefaultPicture(item, R.drawable.ic_file_type_other_file, message.mContent);
    }

    private void onPersonPhotoClicked(View view) {
        ChatMessage msg = (ChatMessage)view.getTag();
        Person person = getRemotePerson(msg);
        if (person != null) {
            Intent intent = new Intent();
            intent.putExtra("chatpersonKey", PersonManager.getPersonKey(person));
            intent.setClass(mContext, QuickPersonInfoActivity.class);
            mContext.startActivity(intent);
        }
    }

    private void onChatContentClicked(View view) {
    	ViewClass myClass = (ViewClass)view.getTag();
        // could direct view under some condition
		if ((myClass.mChatMessage.mDirect == Table_Message.DIRECT_REMOTEPERSON
				&& myClass.mChatMessage.mState == Table_Message.STATE_OK)
				|| (myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER)) {
			viewFiles(myClass);
		}

        if (mSetExpandMessage.contains(myClass.mChatMessage.mId)) {
            mSetExpandMessage.remove(myClass.mChatMessage.mId);
        } else {
            mSetExpandMessage.add(myClass.mChatMessage.mId);
        }
        if (mListView.getTranscriptMode() != ListView.TRANSCRIPT_MODE_NORMAL) {
            mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        }
        notifyDataSetChanged();
    }

    //short click list item ,view files or copy content.
    private void viewFiles(ViewClass myClass) {
        if (myClass.mChatMessage.mType == FileType.FileType_Picture
                || myClass.mChatMessage.mType == FileType.FileType_Music
                || myClass.mChatMessage.mType == FileType.FileType_Video) {
            CommonUtils.viewFile(mContext, myClass.mChatMessage.mType,myClass.mChatMessage.mContent);
            // collapse the list
            if (mSetExpandMessage.contains(myClass.mChatMessage.mId)) {
                mSetExpandMessage.remove(myClass.mChatMessage.mId);
                if (mListView.getTranscriptMode() != ListView.TRANSCRIPT_MODE_NORMAL) {
                    mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
                }
                notifyDataSetChanged();
            }
            return;
        } else if (myClass.mChatMessage.mType == FileType.FileType_Record) {
        	onPlayOver();
            mRecordViewClass = myClass;
            Log.i(TAG, "start play " + myClass.mChatMessage.mContent);
            mRecordPlayer.setSoundFile(myClass.mChatMessage.mContent);
            mRecordPlayer.startPlayback();
            return;
        } else if (myClass.mChatMessage.mType == FileType.FileType_Contact) {
            CommonUtils.viewFile(mContext, myClass.mChatMessage.mType,myClass.mChatMessage.mContent);
            return;
        } else if (myClass.mChatMessage.mType == FileType.FileType_CommonMsg) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                Toast.makeText(mContext, R.string.copy_message_to_clipboard,Toast.LENGTH_SHORT).show();
                ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setText(myClass.mChatMessage.mContent);
            }
            return;
        } else if (myClass.mChatMessage.mType == FileType.FileType_App) {
            CommonUtils.installApp(mContext, myClass.mChatMessage.mContent);
            return;
        }
    }

    private void doAPKAction(View view) {
    	ChatMessage msg = (ChatMessage)view.getTag();
        int type = Integer.valueOf(view.getTag(R.id.tag_first).toString());
        if (type == APK_ACTION_INSTALL || type == APK_ACTION_UPDATE) {
        	CommonUtils.installApp(mContext, msg.mContent);
        } else if (type == APK_ACTION_VIEW) {
        	CommonUtils.viewFile(mContext, FileType.FileType_App, view.getTag(R.id.tag_second).toString());
        }else if (type == APK_ACTION_UNINSTALL) {
        	CommonUtils.unInstallApp(mContext, view.getTag(R.id.tag_second).toString());
        }
        notifyDataSetChanged();
    }

    private void onButtonClicked(View view) {
    	ChatMessage msg = (ChatMessage)view.getTag();

    	switch(view.getId()) {
		    case R.id.chat_left_button1:
		    case R.id.chat_right_button1: {
		        switch (msg.mType) {
		            case FileType_App:
		            	doAPKAction(view);
		                break;
		            case FileType_Contact:
		            	CommonUtils.viewFile(mContext, msg.mType, msg.mContent);
		                break;
		        }
		        break;
		    }
		    
		    case R.id.chat_left_button2:
		    case R.id.chat_right_button2: {
		        switch (msg.mType) {
		        	case FileType_App:
		        		doAPKAction(view);
		        		break;
		        	case FileType_Contact:
		            	CommonUtils.importVcard(mContext, msg.mContent);
		                break;
		        }
		        break;
		    }
		
		    case R.id.chat_left_button3:
		    case R.id.chat_right_button3: {
		        resendMesssage(msg);
		        break;
		    }
		
		    // all button4 is used to delete message
		    case R.id.chat_left_button4:
		    case R.id.chat_right_button4: {
		        deleteMessage(msg);
		        break;
		    }
    	}
    }

    @Override
    public void onClick(View arg0) {
        switch(arg0.getId()) {
            case R.id.chat_layout_left_photo:
            	onPersonPhotoClicked(arg0);
                break;
            case R.id.chat_layout_right_item:
            case R.id.chat_layout_left_item: 
            	onChatContentClicked(arg0);
                break;
            case R.id.chat_left_button1:
            case R.id.chat_right_button1:
            case R.id.chat_left_button2:
            case R.id.chat_right_button2:
            case R.id.chat_left_button3:
            case R.id.chat_right_button3:
            case R.id.chat_left_button4:
            case R.id.chat_right_button4:
                onButtonClicked(arg0);
                break;
        }
    }

    
    @Override
    public boolean onAPKLoadFinished(ImageView view, Drawable drawable, String name, 
            String packageName, int versionCode, String versionName, String path) {
        ViewClass myClass = (ViewClass)view.getTag();
        if (myClass == null) {
            return true;
        }
        ItemGroup item = myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ? 
                myClass.mGroupRight : myClass.mGroupLeft;
        item.mChatGroup.mPicContentGroup.mPicInfo.setText(name + "_" + versionName);
        if (drawable != null) {
            item.mChatGroup.mPicContentGroup.mPic.setImageDrawable(drawable);
        }
        item.mButtonGroup.mButton1.setTag(R.id.tag_second, packageName);
        item.mButtonGroup.mButton2.setTag(R.id.tag_second, packageName);
        item.mChatGroup.mChatContent.setClickable(true);
        if (mSetExpandMessage.contains(myClass.mChatMessage.mId)) {
            //item.mButtonGroup.mBtnGroup.setVisibility(View.VISIBLE);
        }
        if (mAPKCheck != null) {
            mAPKCheck.requestAPK(myClass, packageName, versionCode);
        }
        return true;
    }

    @Override
    public boolean onDrawableLoadFinished(ImageView view, Drawable drawable) {
        return false;
    }

    @Override
    public boolean onBitmapLoadFinished(ImageView view, Bitmap bitmap) {
        ViewClass myClass = (ViewClass)view.getTag();
        if (myClass == null) {
            return true;
        }
        ItemGroup item = myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ? 
                myClass.mGroupRight : myClass.mGroupLeft;

        if (bitmap != null) {
            LayoutParams param = item.mChatGroup.mPicContentGroup.mPic.getLayoutParams();
            param.width = bitmap.getWidth();
            param.height = bitmap.getHeight();
            Log.d(TAG, "Image width" + param.width + " height " + param.height);
            item.mChatGroup.mPicContentGroup.mPic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            item.mChatGroup.mPicContentGroup.mPic.setLayoutParams(param);
            item.mChatGroup.mPicContentGroup.mPicInfo.setText(null);
            item.mChatGroup.mPicContentGroup.mPic.setImageBitmap(bitmap);
        }
        return true;
    }

    @Override
    public boolean onAPKCheckFinished(Object obj, int type, int self) {
        ViewClass myClass = (ViewClass)obj;
        if (myClass == null) {
            return true;
        }
        ItemGroup item = myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ? 
                myClass.mGroupRight : myClass.mGroupLeft;

        switch (type) {
        case APKCheck.APK_TYPE_NOTINSTALL:		// install for not installed
            item.mButtonGroup.mButton1.setVisibility(View.VISIBLE);
            item.mButtonGroup.mButton1.setTag(R.id.tag_first, APK_ACTION_INSTALL);
        	item.mButtonGroup.mButton1.setText(R.string.btn_install);
            break;
        case APKCheck.APK_TYPE_SAME_VERSION:	// view for same version
            item.mButtonGroup.mButton1.setVisibility(View.VISIBLE);
            item.mButtonGroup.mButton1.setTag(R.id.tag_first, APK_ACTION_VIEW);
        	item.mButtonGroup.mButton1.setText(R.string.btn_view);
        	if (self != APKCheck.APK_TYPE_SELF) {// could update if not self
                item.mButtonGroup.mButton2.setVisibility(View.VISIBLE);
                item.mButtonGroup.mButton2.setTag(R.id.tag_first, APK_ACTION_UNINSTALL);
            	item.mButtonGroup.mButton2.setText(R.string.btn_uninstall);
        	}
            break;
        case APKCheck.APK_TYPE_LOWER_VERSION:
        case APKCheck.APK_TYPE_HIGHER_VERSION:	// could update for different version
            item.mButtonGroup.mButton1.setVisibility(View.VISIBLE);
            item.mButtonGroup.mButton1.setTag(R.id.tag_first, APK_ACTION_UPDATE);
        	item.mButtonGroup.mButton1.setText(R.string.btn_update);
        	if (self != APKCheck.APK_TYPE_SELF) {
                item.mButtonGroup.mButton2.setVisibility(View.VISIBLE);
                item.mButtonGroup.mButton2.setTag(R.id.tag_first, APK_ACTION_UNINSTALL);
            	item.mButtonGroup.mButton2.setText(R.string.btn_uninstall);
        	}
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        ViewClass myClass = (ViewClass) v.getTag();
        final ChatMessage msg = myClass.mChatMessage;
        //localuser:if message send failed or timeout,long click that item will resent.
        if (myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER
                && (myClass.mChatMessage.mState == Table_Message.STATE_FAILED ||
                    myClass.mChatMessage.mState == Table_Message.STATE_TIMEOUT)) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.resend_message, null);
            TextView mTextView = (TextView) view.findViewById(R.id.resend);
            mResendPopupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true);
            mResendPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mResendPopupWindow.setOutsideTouchable(true);
            if (!mResendPopupWindow.isShowing()) {
                mResendPopupWindow.showAtLocation(mListView, Gravity.CENTER, 0,0);
                mTextView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mResendPopupWindow.dismiss();
                        resendMesssage(msg);
                    }
                });
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mResendPopupWindow.dismiss();
                    }
                });
            }

        }else if (myClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER
                || myClass.mChatMessage.mDirect == Table_Message.DIRECT_REMOTEPERSON) {
            //if file type is pic,and message send state is (!failed and !timeout),will pop the pic
            if (myClass.mChatMessage.mState != Table_Message.STATE_FAILED
                    && myClass.mChatMessage.mState != Table_Message.STATE_TIMEOUT) {
                if (myClass.mChatMessage.mType == FileType.FileType_Picture) {
                    if (mPopupWindow == null) {
                        View view = LayoutInflater.from(mContext).inflate(R.layout.show_picture, null);
                        mPopupWindow = new PopupWindow(view,LayoutParams.MATCH_PARENT,
                                AbstractChatActivity.mViewFrameHeight - mListView.getTop(), true);
                        mPopupWindow.setOutsideTouchable(true);
                        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        mPopupBackground = view.findViewById(R.id.background);
                        mPopupImage = (ImageView) view.findViewById(R.id.image);
                    }
                    mPopupImage.setImageBitmap(CommonUtils.DecodeBitmap(
                                myClass.mChatMessage.mContent, 480 * 800));
                    mPopupBackground.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.transparent));
                    mPopupWindow.setAnimationStyle(R.style.popwin_anim_style);
                    mPopupWindow.showAtLocation(mListView,Gravity.CENTER_HORIZONTAL | Gravity.TOP,0,
                            AbstractChatActivity.mStatusBarHeight + mListView.getTop());
                    mHandler.sendEmptyMessageDelayed(0, 350);
                }
                    }
                }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	onTouchEvent(v, event);
        return false;
    }

	public void onPlayChange(int value) {
		if (mRecordViewClass == null) {
			return;
		}
        ItemGroup item = mRecordViewClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ? 
        		mRecordViewClass.mGroupRight : mRecordViewClass.mGroupLeft;
        int mRecordPlayImage = mRecordViewClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ?
        		mVoiceRight.getResourceId(value%3, 0) : mVoiceLeft.getResourceId(value%3, 0);
        item.mChatGroup.mRecord.setImageResource(mRecordPlayImage);
	}

	public void onPlayOver() {
		if (mRecordViewClass == null) {
			return;
		}
        ItemGroup item = mRecordViewClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ? 
        		mRecordViewClass.mGroupRight : mRecordViewClass.mGroupLeft;
        int mRecordImage = mRecordViewClass.mChatMessage.mDirect == Table_Message.DIRECT_LOCALUSER ?
        		R.drawable.record_voice_right : R.drawable.record_voice_left;
        item.mChatGroup.mRecord.setImageResource(mRecordImage);
	}

    // for interface of Adapter interface
    @Override
    public boolean canViewDismissed(View selectedView) {
        ViewClass myClass = (ViewClass)selectedView.getTag();
        return myClass.mChatMessage.mState != Table_Message.STATE_WAITING &&
                myClass.mChatMessage.mState != Table_Message.STATE_BEGIN &&
                myClass.mChatMessage.mState != Table_Message.STATE_PROCESS;
    }

    @Override
    public ChatMessage handleSwipe(View selectedView) {
        ViewClass myClass = (ViewClass)selectedView.getTag();
        UserTrace.addTrace(UserTrace.ACTION_DELETE_ONE);
        if (deleteMessage(myClass.mChatMessage) == 0) {
            myClass.mNeedRecovery = true;
            if (mListView.getTranscriptMode() != ListView.TRANSCRIPT_MODE_NORMAL) {
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
            }
            return myClass.mChatMessage;
        }
        return null;
    }

    @Override
    public void notifyProcessChanged() {
        notifyDataSetChanged();
    }

    @Override
    public void unInit() {
        if (mAPKCheck != null) {
            mAPKCheck.unInit();
            mAPKCheck = null;
        }
        if (mImageLoader != null) {
            mImageLoader.unInit();
            mImageLoader = null;
        }
    }

    public void onTouchEvent(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP){
            if (mPopupWindow != null && mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
        }
    }

}
