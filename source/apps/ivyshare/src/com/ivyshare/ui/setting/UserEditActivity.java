package com.ivyshare.ui.setting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.ivy.ivyengine.control.DaemonNotifactionInterface;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.R;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.main.MainPagerActivity;
import com.ivyshare.util.CommonUtils;
import com.ivyshare.util.IvyActivityBase;
import com.ivyshare.widget.SimpleImageButton;

public class UserEditActivity extends IvyActivityBase implements OnClickListener {

    private static final int REQUEST_CODE_CAMERA_WITH_DATA = 1001;
    private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 1002;
    private static final int REQUEST_CODE_CAMERA_CROP_WITH_DATA = 1003;
    
    private static final int REQUEST_CODE_NICKNAME_EDIT = 2001;
    private static final int REQUEST_CODE_GROUPNAME_EDIT = 2002;
    private static final int REQUEST_CODE_SIGN_EDIT = 2003;
    
	private static final int MESSAGE_NETWORK_STATE_CHANGED = 10;

    private static final String CAPTURE_PATH = LocalSetting.getInstance().getLocalPath() + "capture.jpg";
    private static final String CROP_PATH = LocalSetting.getInstance().getLocalPath() + "crop.dat";
    // private static final String PHOTO_PATH = LocalSetting.getInstance().getLocalPath() + "photo.dat";
    private ImageView mImagePhoto = null;
    private TextView mTextNickName = null;
    private EditText mEditNickName = null;
    private TextView mTextGroupName = null;
    private EditText mEditGroupName = null;
    private TextView mTextSign = null;
    private EditText mEditSign = null;
    private ImageView imageleft = null;
    private SimpleImageButton imageRight = null;
    private LinearLayout mAccountLayout;
    private LinearLayout mAccountListLayout;

    private boolean isCanEdit = true;
    private boolean isEdit = false;
    private boolean isClick = false;
    private LocalSetting mLocalSetting;
    private Person person;

    private Account[] accounts;
	private List<String> data;
	private SharedPreferences sp;
	private boolean mIsInited;
	private ArrayList<String> accountNames;
	private String profileName;
	private String mGoogleAccount;
	private String[] mDefaultAvatars;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_user_setting);

		sp = getSharedPreferences("SP", MODE_PRIVATE);
		mIsInited = sp.getBoolean("INIT", false);
		mLocalSetting = LocalSetting.getInstance();
		person = mLocalSetting.getMySelf();
		mDefaultAvatars = getResources().getStringArray(R.array.default_avatar);

		// for action bar
        View actionbar = (View)findViewById(R.id.layout_title);
        ((TextView)actionbar.findViewById(R.id.text_info)).setText(R.string.usersetting);
		mAccountLayout = (LinearLayout) findViewById(R.id.layoutaccountlist);
		mAccountLayout.setMinimumHeight(60);
		mAccountListLayout = (LinearLayout) findViewById(R.id.account_list);

		AccountManager accountManager = AccountManager.get(this);
		accounts = accountManager.getAccounts();
		data = new ArrayList<String>();
		accountNames = new ArrayList<String> ();
		if (accounts.length > 0 ) {
			for (Account account : accounts) {
				String mAccountName = account.name;
				if (mAccountName.contains("@")) {
					data.add(mAccountName);
					mAccountName = mAccountName.substring(0 , account.name.indexOf("@"));
					accountNames.add(mAccountName);
					if (account.type.equals("com.google")) {
						mGoogleAccount = mAccountName;
					}
				}
			}
			setMyCard(data);
		} else {
			mAccountLayout.setVisibility(View.GONE);
		}

		TelephonyManager phoneMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String mLine1Number = phoneMgr.getLine1Number();
		RelativeLayout mLine1NumberLayout = (RelativeLayout) findViewById(R.id.layoutline1number);
		TextView mTextLine1Number = (TextView)findViewById(R.id.line1number);
		if (mLine1Number != null && !"".equals(mLine1Number)) {
			mTextLine1Number.setText(mLine1Number);
		} else {
			mLine1NumberLayout.setVisibility(View.GONE);
		}

        imageleft = (ImageView)actionbar.findViewById(R.id.btn_left);
        imageleft.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mIsInited) {
					if (isEdit) {
						clickForModify();
					} else {
						saveChanges();
					}
				} else {
					isEdit = true;
					saveChanges();
				}
			}
		});

        imageRight = (SimpleImageButton)findViewById(R.id.btn_right);
        imageRight.setVisibility(View.VISIBLE);
        imageRight.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mIsInited) {
					if (isEdit) {
						saveChanges();
					} else {
						clickForModify();
					}
				} else {
					isEdit = true;
					saveChanges();
				}
			}

        });

		mImagePhoto = (ImageView)findViewById(R.id.photoimage);
		mImagePhoto.setOnClickListener(this);
		mTextNickName = (TextView)findViewById(R.id.nickname);
		mEditNickName = (EditText)findViewById(R.id.edit_nickname);

		mTextGroupName = (TextView)findViewById(R.id.groupname);
		mEditGroupName = (EditText)findViewById(R.id.edit_groupname);

		mEditSign = (EditText)findViewById(R.id.edit_signname);
		mTextSign = (TextView)findViewById(R.id.signname);

		setOnClick(mTextNickName,mEditNickName);
		setOnClick(mTextGroupName,mEditGroupName);
		setOnClick(mTextSign,mEditSign);
        InitLocalUser();
	}

    //when edit focus changed,save other edit text content.
	private void saveContext() {
		if (isEdit) {
			String groupName = mEditGroupName.getText().toString();
			String nickName = mEditNickName.getText().toString().trim();
			String signature = mEditSign.getText().toString();
			mLocalSetting.saveGroupName(groupName);
			mLocalSetting.saveNickName(nickName);
			mLocalSetting.saveSignContent(signature);
			if (mImManager != null) {
			    DaemonNotifactionInterface daemon = mImManager.getDaemonNotifaction();
			    if (daemon != null) {
			        daemon.startBackgroundNotificationIfNeed();
			    }
			    mImManager.absence();
            }
		}
	}

	//if nickName is null,stay this activity,
	private void saveChanges() {
		String nickName = mEditNickName.getText().toString().trim();
		boolean isNull = false;
		if (nickName == null || "".equals(nickName)) {
			isNull = true;
		}

		if (isEdit && isNull) {
			Toast.makeText(this, R.string.nick_name_empty, Toast.LENGTH_SHORT).show();
			return;
		} else {
			UserTrace.addTrace(UserTrace.ACTION_SELF_SETTING);
			saveContext();
			if (!mIsInited) {
				Editor editor = sp.edit();
				editor.putBoolean("INIT", true);
				editor.commit();
				Intent intent = new Intent();
				intent.setClass(this, MainPagerActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}


	private void setOnClick(TextView mTextView, final EditText mEditText) {
		mTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clickForModify();
			}
		});
		mEditText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				isClick = !isClick;
				// click two times,will select all text
				if (!isClick) {
					deleteOrigin(mEditText);
				}
			}
		});
	}

	// set edittext and textview visibility.
	private void setVisibly(TextView mTextView, EditText mEditText) {
		if (mIsInited) {
			if (isCanEdit) {
				mTextView.setVisibility(View.GONE);
				mEditText.setVisibility(View.VISIBLE);
				mEditText.setText(mTextView.getText().toString());
				mEditText.selectAll();
			} else {
				mTextView.setVisibility(View.VISIBLE);
				mEditText.setVisibility(View.GONE);
				isEdit = !isEdit;
			}
		} else {
			mTextView.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);
			mEditText.setText(mTextView.getText().toString());
			mEditText.selectAll();
		}
	}

	// when click change button or textview (nickname,sign,groupname)
	private void clickForModify() {
		if (isCanEdit) {
			isEdit = !isEdit;
			imageleft.setImageResource(R.drawable.action_cancel);
			imageRight.setImageResource(R.drawable.action_done);

			setVisibly(mTextNickName, mEditNickName);
			mEditNickName.setFocusable(true);
			mEditNickName.requestFocus();

			setVisibly(mTextGroupName, mEditGroupName);
			setVisibly(mTextSign, mEditSign);
		} else {
			CommonUtils.getPersonPhoto(imageleft,person.mImage);
			imageRight.setImageResource(R.drawable.action_edit);

			setVisibly(mTextNickName, mEditNickName);
			setVisibly(mTextGroupName, mEditGroupName);
			setVisibly(mTextSign, mEditSign);
		}
		isCanEdit = !isCanEdit;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void InitLocalUser() {
		if (mIsInited) {
	        imageRight.setImageResource(R.drawable.action_edit);
			mTextNickName.setText(person.mNickName);
			mTextGroupName.setText(person.mGroup);
			mTextSign.setText(person.mSignature);
			CommonUtils.getPersonPhoto(imageleft, person.mImage);
			CommonUtils.getPersonPhoto(mImagePhoto, person.mImage);
		} else {
	        imageRight.setImageResource(R.drawable.action_done);
			setVisibly(mTextNickName,mEditNickName);
			setVisibly(mTextGroupName,mEditGroupName);
			setVisibly(mTextSign,mEditSign);
			mEditGroupName.setText(person.mGroup);
			getDefaultNickNameAndPhoto();
		}
	}
	
	//if the app init,set defalut nickname for user.
	//nickname:1.Line1Number,2.profileName,3.google account,4.other account,5.phone model
	@SuppressLint("NewApi")
	private void getDefaultNickNameAndPhoto() {
		// if adk version >= 14, can get profile name.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			Cursor cursor = this.getContentResolver().query(Profile.CONTENT_URI, null, null, null, null);
			if (cursor.moveToNext()) {
				long mProfileId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
				String mProfileName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (mProfileName != null && !"".equals(mProfileName)) {
					profileName = mProfileName;
				}

				Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, mProfileId);
				InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), uri);
				if (null != input) {
					Bitmap bitmap = BitmapFactory.decodeStream(input);
					if (bitmap != null) {
						UserIconEnvironment userIconEnvironment = mLocalSetting.getUserIconEnvironment();
						//delete old path;
						{
							File old = new File(userIconEnvironment.getSelfHeadFullPath());
							old.delete();
						}
						String name = userIconEnvironment.generateRandName();
						mLocalSetting.saveImageName(name);
						String photoPath = userIconEnvironment.getSelfHeadFullPath();

						File myCaptureFile = new File(photoPath);
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(myCaptureFile);
							if(bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos)) {
								fos.flush();
								fos.close();
	                        }
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						mImagePhoto.setImageBitmap(bitmap);
						imageleft.setImageBitmap(bitmap);
                        if (mImManager != null) {
                            mImManager.absence();
                            mImManager.sendHeadIcon();
                        }
					}
				} else {
					getRandomDefaultAvatar();
				}

			}
			getRandomDefaultAvatar();
			cursor.close();
		} else {
			profileName = null;

			getRandomDefaultAvatar();
		}

		if (profileName != null && !"".equals(profileName)) {
			mEditNickName.setText(profileName);
		} else if (data.size() > 0) {
			if (mGoogleAccount != null && !"".equals(mGoogleAccount)) {
				mEditNickName.setText(mGoogleAccount);
			} else {
				mEditNickName.setText(accountNames.get(0));
			}
		} else {
			mEditNickName.setText(person.mName);
		}
	}

	private void getRandomDefaultAvatar() {
		int random = (int) (Math.random() * 10 - 1);
		mLocalSetting.saveImageName(mDefaultAvatars[random]);
		CommonUtils.getPersonPhoto(mImagePhoto, mDefaultAvatars[random]);
		CommonUtils.getPersonPhoto(imageleft, mDefaultAvatars[random]);
	}

	@Override
	public void onBackPressed() {
		if (mIsInited) {
			saveChanges();
		} else {
			mImManager = null;
			super.onBackPressed();
		}
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // Photo was chosen (either new or existing from gallery), and cropped.
            	case REQUEST_CODE_CAMERA_CROP_WITH_DATA:
            		File temp = new File(CAPTURE_PATH);
            		temp.delete();
            	case REQUEST_CODE_PHOTO_PICKED_WITH_DATA: {
                    Bitmap bitmap = BitmapFactory.decodeFile(CROP_PATH);
                    if (bitmap != null) {
                    	UserIconEnvironment userIconEnvironment = mLocalSetting.getUserIconEnvironment();

                		{
                		    File old = new File(userIconEnvironment.getSelfHeadFullPath());
                		    old.delete();
                		}

                		{
                		    String name = userIconEnvironment.generateRandName();
                		    mLocalSetting.saveImageName(name);
                		    String photoPath = userIconEnvironment.getSelfHeadFullPath();
                		    File file = new File(CROP_PATH);
                		    file.renameTo(new File(photoPath));
                		}

                        mImagePhoto.setImageBitmap(bitmap);
                        if (mImManager != null) {
                            mImManager.absence();
                            mImManager.sendHeadIcon();
                        }
                    }
				// if edittext is visible and modify photo, left button src don't change
				if (isCanEdit) {
					CommonUtils.getPersonPhoto(imageleft,person.mImage);
				}

                    break;
                }
                // Photo was successfully taken, now crop it.
                case REQUEST_CODE_CAMERA_WITH_DATA: {
                	doCropPhoto();
                	break;
                }

                case REQUEST_CODE_NICKNAME_EDIT: {
                	String content = data.getExtras().getString("EditResult");
                	mTextNickName.setText(content);
                	mLocalSetting.saveNickName(content);
                	if (mImManager != null) {
                	    mImManager.absence();
                	    mImManager.getDaemonNotifaction().startBackgroundNotificationIfNeed();
                	}
                	break;
                }
                case  REQUEST_CODE_GROUPNAME_EDIT: {
                	String content = data.getExtras().getString("EditResult");
                	mTextGroupName.setText(content);
                	mLocalSetting.saveGroupName(content);
                	if (mImManager != null) {
                	    mImManager.absence();
                	}
                	break;
                }
                case REQUEST_CODE_SIGN_EDIT: {
                	String content = data.getExtras().getString("EditResult");
                	mTextSign.setText(content);
                	mLocalSetting.saveSignContent(content);
                	if (mImManager != null) {
                	    mImManager.absence();
                	}
                	break;
                }
            }
        }
    }
    
    private void doCropPhoto() {  	
        final Uri inputPhotoUri = Uri.fromFile(new File(CAPTURE_PATH));
        final Uri croppedPhotoUri = Uri.fromFile(new File(CROP_PATH));

        MediaScannerConnection.scanFile(
                this,
                new String[] { CAPTURE_PATH },
                new String[] { null },
                null);

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputPhotoUri, CommonUtils.MIMETYPE_ALLIMAGES);
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, croppedPhotoUri);
        UserEditActivity.this.startActivityForResult(intent, REQUEST_CODE_CAMERA_CROP_WITH_DATA);
    }
	
	class SelectDialog extends AlertDialog{


		public SelectDialog(Context context, int theme) {
		    super(context, theme);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    
		    setContentView(R.layout.image_choose);
		    
			View defaultPhotoGV = (View) findViewById(R.id.choose_default_photo);
			GridView mGridView = (GridView) defaultPhotoGV.findViewById(R.id.photo_pick_gv);
			ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();

			for (int i = 0; i < mDefaultAvatars.length; i++) {
				try {
					InputStream is = UserEditActivity.this.getResources().getAssets()
							.open("default_heads/" + mDefaultAvatars[i]);
					Bitmap image = BitmapFactory.decodeStream(is);
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("ItemImage", image);
					lstImageItem.add(map);
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			SimpleAdapter saImageItems = new SimpleAdapter(UserEditActivity.this, lstImageItem,
					R.layout.default_photo, new String[] { "ItemImage" },
					new int[] { R.id.default_photo });

			saImageItems.setViewBinder(new ViewBinder() {
				@Override
				public boolean setViewValue(View view, Object data,String textRepresentation) {
					if (view instanceof ImageView && data instanceof Bitmap) {
						ImageView i = (ImageView) view;
						i.setImageBitmap((Bitmap) data);
						return true;
					}
					return false;
				}
			});

			mGridView.setAdapter(saImageItems);
			saImageItems.notifyDataSetChanged();

		mGridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {

					SelectDialog.this.cancel();
					mLocalSetting.saveImageName(mDefaultAvatars[arg2]);
					CommonUtils.getPersonPhoto(imageleft, mDefaultAvatars[arg2]);
					CommonUtils.getPersonPhoto(mImagePhoto, mDefaultAvatars[arg2]);
					if (mImManager != null) {
						mImManager.absence();
						mImManager.sendHeadIcon();
					}
				}

			});

		    Button btnTake = (Button)findViewById(R.id.fromcamera);
		    btnTake.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					SelectDialog.this.cancel();
			        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
			        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(CAPTURE_PATH)));
			        UserEditActivity.this.startActivityForResult(intent, REQUEST_CODE_CAMERA_WITH_DATA);
				}
			});
		    
		    Button btnPick = (Button)findViewById(R.id.fromgallery);
		    btnPick.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					SelectDialog.this.cancel();
			        final Uri croppedPhotoUri = Uri.fromFile(new File(CROP_PATH));
			        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
			        intent.setType(CommonUtils.MIMETYPE_ALLIMAGES);
			        intent.putExtra("crop", "true");
			        intent.putExtra("scale", true);
			        intent.putExtra("scaleUpIfNeeded", true);
			        intent.putExtra("aspectX", 1);
			        intent.putExtra("aspectY", 1);
			        intent.putExtra("outputX", 300);
			        intent.putExtra("outputY", 300);
			        intent.putExtra(MediaStore.EXTRA_OUTPUT, croppedPhotoUri);
			        UserEditActivity.this.startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
				}
			});
		}
	}
	
	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()) {
			case R.id.photoimage: {
					SelectDialog dialog= new SelectDialog(this, R.style.Theme_NoTitle);
					dialog.show();
				}
				break;
		}
	}

	//if has account, display it.
	private void setMyCard(List<String> accountList) {
		for (int i = 0;i < accountList.size();i++) {
			TextView mTextViewAccount = new TextView(this);
			mTextViewAccount.setHeight(60);
			mTextViewAccount.setPadding(0, 5, 0, -5);
			mTextViewAccount.setText(accountList.get(i));
			mTextViewAccount.setTextColor(getResources().getColor(R.color.listbar_secondray));
			mTextViewAccount.setTextSize(16);
			mAccountListLayout.addView(mTextViewAccount);
		}
	}

    //click edittext,select all the text,user can delete this with one delete key
    private void deleteOrigin(EditText mEditText) {
        mEditText.setText(mEditText.getText().toString());
        Spannable content =	mEditText.getText();
        Selection.selectAll(content);
    }
}
