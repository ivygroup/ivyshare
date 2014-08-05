package com.ivyshare.ui.chat.abstractchat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.Drawable;

import com.ivy.ivyengine.im.Im;
import com.ivy.ivyengine.im.Im.FileType;

public class FileBasket {
    public static final String TAG = "FileBasket";

    private List<SelectedItemInfo> mData;
    //private DataListener mListener;


    public FileBasket() {//DataListener listener) {
        mData = new LinkedList<FileBasket.SelectedItemInfo>();
        //mListener = listener;
    }

    //public void setListener(DataListener listener) {
    //    mListener = listener;
    //}

    public List<SelectedItemInfo> getAllFiles() {
        synchronized (mData) {
            return new ArrayList<FileBasket.SelectedItemInfo>(mData);
        }
    }

    public ArrayList<Integer> getFilesTypeList() {
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
        	list.add(mData.get(i).mFileType.ordinal());
        }
        return list;
    }

    public ArrayList<String> getFilesPathList() {
    	ArrayList<String> list = new ArrayList<String>();
    	int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
        	list.add(mData.get(i).mPath);
        }
        return list;
    }

    public int getFilesCount() {
        synchronized (mData) {
            return mData.size();
        }
    }

    public void addToBasket(FileType type, String path, String displayName, Drawable icon) {
        SelectedItemInfo info = new SelectedItemInfo();
        info.mFileType = type;
        info.mIcon = icon;
        info.mDisplayName = displayName;
        info.mPath = path;

        synchronized (mData) {
            mData.add(info);
        }
        //if (mListener != null) {
        //    mListener.fileBasketDataChanged();
        //}
    }

    public void removeFromBasket(String path) {
    	int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
            SelectedItemInfo info = mData.get(i);
            if (info.mPath.compareTo(path) == 0) {
                removeFile(i);
            	break;
            }
        }
    }

    public boolean isPathInBasket(String path) {
    	int nSize = mData.size();
        for (int i = 0; i < nSize; i++) {
            SelectedItemInfo info = mData.get(i);
            if (info.mPath.compareToIgnoreCase(path) == 0) {
            	return true;
            }
        }
        return false;
    }

    public void removeFile(int position) {
        if (position < 0 || position >= mData.size()) {
            return;
        }

        synchronized (mData) {
            removeFile_l(position);
        }

        //if (mListener != null) {
       //     mListener.fileBasketDataChanged();
       // }
    }

    public void removeAllFiles() {
        synchronized (mData) {
            for (int i = 0; i < mData.size(); i++) {
                removeFile_l(i);
            }
            mData.clear();
        }

       // if (mListener != null) {
        //    mListener.fileBasketDataChanged();
       // }
    }


    // ========================================================
    private void removeFile_l(int position) {
        mData.remove(position);
    }


    // ========================================================
    public static class SelectedItemInfo {
        public String mDisplayName;
        public Drawable mIcon;
        public Im.FileType mFileType;
        public String mPath;    // the full path
    }

    public static interface DataListener {
        public void fileBasketDataChanged();
    }
}
