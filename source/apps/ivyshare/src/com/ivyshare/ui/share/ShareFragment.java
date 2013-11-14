package com.ivyshare.ui.share;

import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ivyshare.R;
import com.ivyshare.engin.connection.ConnectionState;
import com.ivyshare.engin.connection.NetworkManager;
import com.ivyshare.engin.control.ImManager;
import com.ivyshare.engin.control.LocalSetting;
import com.ivyshare.httpserver.HttpServer;
import com.ivyshare.httpserver.HttpServerManager;
import com.ivyshare.trace.UserTrace;
import com.ivyshare.ui.setting.BasePopMenuAdapter;
import com.ivyshare.widget.SimplePopMenu;

public class ShareFragment extends Fragment implements OnClickListener, OnLongClickListener {
	private static final String TAG = ShareFragment.class.getSimpleName();
		
    private int mShareFileType = ShareType.SHARE_TYPE_UNKNOWN;
    
    private String mShareFilePath = null;
    private String mShareFileDisplayName;
    private String mShareUrl;
    //private ImageButton mShareUrlButton;
    private ImageButton mSendUrlButton;
    private ImageButton mCopyUrlButton;
    
    private TextView mTextAddress = null;
    private ImageView m2dImage;
    private TextView mWlanUnavailable;
    private LinearLayout mWlanLayout;
    
    private View mRootView = null;
    private ImManager mImManager = null;
    private NetworkManager mNetworkManager = null;
    private SimplePopMenu mPopMenu;
    private ShareMenuAdapter mShareMenuAdapter;
    //private View mShareButton;

    private boolean isSave = false;
    private boolean isShare = false;
    private boolean isNetWorkConnect = false;
    
	private SendSelectActivity mActivity = null;
	
	private final int QRCODE_SIZE = 300;
	
	private int mFileCount = 0;
    private Integer mListType;
    private List<String> mListPath;
    
    private HttpServerManager mHttpServerManager;
    
    public void onAttach(Activity activity) {
    	Log.d(SendSelectActivity.TAG, TAG + " onAttach");
        super.onAttach(activity);
        mFileCount = 0;
        mListPath = null;
        mListType = ShareType.SHARE_TYPE_UNKNOWN;
        mActivity = (SendSelectActivity)activity;
        mActivity.setFragment(SendSelectActivity.POS_SHARE, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView= inflater.inflate(R.layout.page_share, container, false);        
        
        mTextAddress = (TextView)mRootView.findViewById(R.id.address);
        mTextAddress.setOnClickListener(this);
        mWlanUnavailable = (TextView)mRootView.findViewById(R.id.no_wlan_connection);
        mWlanLayout = (LinearLayout) mRootView.findViewById(R.id.wlan_connect_layout);
        /*mShareUrlButton = (ImageButton)mRootView.findViewById(R.id.share_url);
        mShareUrlButton.setOnClickListener(this);
        mShareUrlButton.setVisibility(View.INVISIBLE);*/
        mSendUrlButton = (ImageButton)mRootView.findViewById(R.id.send_url);
        mSendUrlButton.setOnClickListener(this);
        mSendUrlButton.setOnLongClickListener(this);
        mSendUrlButton.setVisibility(View.INVISIBLE);
        mCopyUrlButton = (ImageButton)mRootView.findViewById(R.id.copy_url);
        mCopyUrlButton.setOnClickListener(this);
        mCopyUrlButton.setOnLongClickListener(this);
        mCopyUrlButton.setVisibility(View.INVISIBLE);
        
        m2dImage = (ImageView)mRootView.findViewById(R.id.td_code);
        m2dImage.setVisibility(View.INVISIBLE);
        
        isSave = false;
        
        View viesWifiPassword = (View)mRootView.findViewById(R.id.layoutwifipassword);
        viesWifiPassword.setVisibility(View.GONE);

        configNetworkDisplay();
        
		return mRootView;		
    }

    @Override
    public void onDestroyView (){
        try {
            HttpServer.stop();
            if (mHttpServerManager != null) {
                mHttpServerManager.stopShare();
                mHttpServerManager = null;
                Log.d(TAG, "mHttpServerManager stop");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        isSave = false;
        isShare = false;
        mFileCount = 0;
        
        super.onDestroyView();
    }

    public class ShareMenuAdapter extends BasePopMenuAdapter {
    	public ShareMenuAdapter(Context context) {
    		super(context);
    	}

    	@Override
    	protected void initMenuType() {
    		mMenuType = MENU_LIST;
    	}

    	@Override
    	protected void initMenuItem() {
    		mMenuItem = new String[] {
    				mContext.getResources().getString(R.string.copy_to_clipboard),
    				mContext.getResources().getString(R.string.send_by_sms)};

    		mMenuIcon = new int[] { R.drawable.ic_menu_copy,
    							    R.drawable.ic_menu_message };
    	}

    }

    @Override
    public void onClick(View arg0) {
    	switch (arg0.getId()) {
    	case R.id.send_url:
    		onMessageShare(mShareUrl);
        	break;
    	case R.id.copy_url:
    		ClipboardManager clip = (ClipboardManager)mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        	clip.setText(mShareUrl);
        	break;
    	case R.id.address:
    		Intent intent = new Intent();
    		intent.setData(Uri.parse("http://127.0.0.1:8080/"));
    		intent.setAction(Intent.ACTION_VIEW);
    		startActivity(intent); 
    		break;
    	}
    }
    
    public void setImManager(ImManager imManager) {
        mImManager = imManager;
        
        if( mShareFilePath != null && isSave == false ){
            mImManager.saveFreeShare( ShareType.getFileType(mShareFileType), mShareFilePath);
            isSave = true;
        }
    }
    
	public void setNetworkManager(NetworkManager networkManager) {
    	mNetworkManager = networkManager;
    }
	
    public void setShareContent( String path, int type){
        
        mShareFileType = type;
        mShareFilePath = path;
        mShareFileDisplayName = mShareFilePath.substring(mShareFilePath.lastIndexOf('/'));
        
        if( mImManager != null && isSave == false ){
            mImManager.saveFreeShare( ShareType.getFileType(mShareFileType), mShareFilePath);
            isSave = true;
        }
        
        if( isNetWorkConnect == true && isShare == false ){
            shareFiles();
            isShare = true;
        }
    }
    
    public void setShareContent(int count, List<String> path, Integer type){
        mFileCount = count;
        mListType = type;
        mListPath = path;
    }
    
    public void configNetworkDisplay(){

        int networkStatus = mNetworkManager.getConnectionState().getLastStateByFast();
        if( networkStatus == ConnectionState.CONNECTION_UNKNOWN ||
        		networkStatus == ConnectionState.CONNECTION_STATE_HOTSPOT_DISABLING ||
        				networkStatus == ConnectionState.CONNECTION_STATE_WIFI_DISCONNECTED)
        {
        	mWlanUnavailable.setVisibility(View.VISIBLE);
        	mWlanLayout.setVisibility(View.GONE);
            return;
        }else if( networkStatus == ConnectionState.CONNECTION_STATE_HOTSPOT_ENABLED){
            
            View viesWifiPassword = (View)mRootView.findViewById(R.id.layoutwifipassword);
            viesWifiPassword.setVisibility(View.VISIBLE);

            String password = mNetworkManager.getConnectionInfo().getIvyHotspotPassword();
            if( password != null ){
                TextView textWifiPassword = (TextView)mRootView.findViewById(R.id.wifi_password);
                textWifiPassword.setText(password);
            }
            
        }

        String mSSID = mNetworkManager.getConnectionInfo().getSSID(); 
        if( mSSID != null ){
            TextView textWifiName = (TextView)mRootView.findViewById(R.id.wifi_name);
            textWifiName.setText(mSSID);
        }
        
        isNetWorkConnect = true;
        
        /*if( mListPath != null && isShare == false ){
            shareFile();
            isShare = true;
        } */
        
    }
    
    public void shareFiles(){
        if (LocalSetting.getInstance().getMySelf().mIP != null) {
            
            mShareUrl = "http://"
                    +LocalSetting.getInstance().getMySelf().mIP.getHostAddress()
                    +":8080/";
            mTextAddress.setText(mShareUrl);
            //mShareUrlButton.setVisibility(View.VISIBLE);
            mSendUrlButton.setVisibility(View.VISIBLE);
            mCopyUrlButton.setVisibility(View.VISIBLE);
            
            m2dImage.setVisibility(View.VISIBLE);
            try {
                m2dImage.setImageBitmap(Create2DCode(mShareUrl));
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(getActivity(), 
                    R.string.share_network_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        UserTrace.addShareTrace(UserTrace.ACTION_FREESHARE, mShareFileType, SendSelectActivity.mLoadSource);
        String sharePath = mShareFilePath.substring(0,mShareFilePath.lastIndexOf('/'));
        Log.e(TAG, "the file share path is "+sharePath);
        try {
            HttpServer.start(sharePath,mShareUrl,mShareFileDisplayName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void shareFile(){
        /*if (!IvyNetwork.getInstance().getIvyNetService().getConnectionState().isConnected()) {
            return;
        }*/
        if (mImManager == null) {
            return;
        }
        if (mListPath == null || mListType == null || mFileCount == 0) {
            return;
        }
        if (isShare) {
            return;
        }

        if (LocalSetting.getInstance().getMySelf().mIP != null) {
            if (mHttpServerManager == null) {
                mHttpServerManager = new HttpServerManager();
            }
            mHttpServerManager.startShare(mListPath, 8080);
            Log.d(TAG, "mHttpServerManager start " + mHttpServerManager.getShareURL());

            mTextAddress.setText(mHttpServerManager.getShareURL());
            mShareUrl = mHttpServerManager.getShareURL();
            //mShareUrlButton.setVisibility(View.VISIBLE);
            mSendUrlButton.setVisibility(View.VISIBLE);
            mCopyUrlButton.setVisibility(View.VISIBLE);

            m2dImage.setVisibility(View.VISIBLE);
            try {
                m2dImage.setImageBitmap(Create2DCode(mHttpServerManager.getShareURL()));
            } catch (WriterException e) {
                e.printStackTrace();
            }

            for (int i=0; i<mFileCount; i++) {
                UserTrace.addShareTrace(UserTrace.ACTION_FREESHARE, mListType, SendSelectActivity.mLoadSource);
                if (mImManager != null) {
                	mImManager.saveFreeShare(ShareType.getFileType(mListType), mListPath.get(i));
                }
            }

            isShare = true;
        }else{
            Toast.makeText(getActivity(), 
                    R.string.share_network_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }
    }
    
    protected void onShare(String shareUrl) {
        String share_content_message = shareUrl;
        Intent it = new Intent(Intent.ACTION_SEND);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.putExtra(Intent.EXTRA_TEXT, share_content_message);
        it.setType("text/plain");
        final CharSequence chooseTitle = getText(R.string.lansharing);
        Intent newIntent = Intent.createChooser(it, chooseTitle);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(newIntent);
    }
    
    protected void onMessageShare(String url){
        String share_content_message = LocalSetting.getInstance().getMySelf().mName
                                       + getText(R.string.share_message_use)
                                       + getText(R.string.app_name)
                                       + getText(R.string.share_message_share)
                                       //+ mShareFileDisplayName
                                       + ","
                                       + getText(R.string.share_message_download)
                                       + url;
        
        Intent shareIntent = new Intent(Intent.ACTION_VIEW);
        shareIntent.setType("vnd.android-dir/mms-sms");  
        shareIntent.putExtra("sms_body", share_content_message);
        startActivity(shareIntent);
    }
    
    public Bitmap Create2DCode(String str) throws WriterException {
        
        Hashtable<EncodeHintType, Object> qrParam = new Hashtable<EncodeHintType, Object>();
        
        qrParam.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);  
        qrParam.put(EncodeHintType.CHARACTER_SET, "UTF-8");  
        
        BitMatrix matrix = new MultiFormatWriter().encode(str,BarcodeFormat.QR_CODE, QRCODE_SIZE
                                                          , QRCODE_SIZE);
        
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        
        int[] pixels = new int[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if(matrix.get(x, y)){
                    pixels[y * width + x] = 0xff000000;
                }
            }
        }
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        
        createQRCodeBitmapWithPortrait(bitmap);
        
        return bitmap;
    }
    
    private void createQRCodeBitmapWithPortrait(Bitmap qr) {
        
        Bitmap portrait = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.code_middle);
        
        int portrait_W = portrait.getWidth();  
        int portrait_H = portrait.getHeight();  
      
        int left = (QRCODE_SIZE - portrait_W) / 2;  
        int top = (QRCODE_SIZE - portrait_H) / 2;  
        int right = left + portrait_W;  
        int bottom = top + portrait_H;  
        Rect rect1 = new Rect(left, top, right, bottom);  
      
        Canvas canvas = new Canvas(qr);  
      
        Rect rect2 = new Rect(0, 0, portrait_W, portrait_H); 
        canvas.drawBitmap(portrait, rect2, rect1, null);  
    }

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		int toastTextId = 0;
		switch (v.getId()) {
		case R.id.send_url:
			toastTextId = R.string.send_by_sms;
			break;
		case R.id.copy_url:
			toastTextId = R.string.copy_to_clipboard;
			break;
		}
		Toast.makeText(mActivity, toastTextId, Toast.LENGTH_SHORT).show();
		return false;
	}  
}