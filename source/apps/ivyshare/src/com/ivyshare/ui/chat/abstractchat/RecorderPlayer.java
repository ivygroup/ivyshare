/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivyshare.ui.chat.abstractchat;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import com.ivy.ivyengine.control.LocalSetting;

public class RecorderPlayer implements OnCompletionListener, OnErrorListener {
    static final String SAMPLE_PREFIX = "recording";
    static final String SAMPLE_PATH_KEY = "sample_path";
    static final String SAMPLE_LENGTH_KEY = "sample_length";

    private static final String TAG = RecorderPlayer.class.getSimpleName();

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;
    
    int mState = IDLE_STATE;
    
	static final int BITRATE_AMR = 12200; // bits/sec
	static final int BITRATE_3GPP = 12200;

    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int IN_CALL_RECORD_ERROR = 3;

    public interface OnStateChangedListener {
        public void onStateChanged(int state);
        public void onError(int error);
    }
    OnStateChangedListener mOnStateChangedListener = null;
    
    private long mSampleStart = 0;       // time at which latest record or play operation started
    private int mSampleLength = 0;      // length of current sample
    private File mSampleFile = null;
    private String mSoundFile = "";

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private VMChangeListener mVMChangListener = null;
    private PlayChangeListener mPlayChangeListener = null;
    private int mPlayValue;
    

	final Handler mHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			updateVolume();
		}
	};
	Runnable mUpdatePlayTimer = new Runnable() {
		public void run() {
			updatePlay();
		}
	};

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public interface  VMChangeListener{
    	public int onVMChanged(int value);
    }
    public interface  PlayChangeListener{
    	public void onPlayChange(int value);
    	public void onPlayOver();
    }

    public void setVMChangeListener(VMChangeListener listener) {
    	mVMChangListener = listener;
    }
    public void setPlayeChangedListener(PlayChangeListener listener) {
    	mPlayChangeListener = listener;
    }

    public int state() {
        return mState;
    }

    private static final int mVolumeRank[] = {0, 512, 1024, 2056, 4096, 8192, 16384};
    private void updateVolume() {
    	if (mState != RECORDING_STATE) {
    		return;
    	}

    	int volume = mRecorder.getMaxAmplitude();
    	int value = 0;
    	int size = mVolumeRank.length;
    	for (int i=0; i<size; i++) {
    		if (volume < mVolumeRank[i]) {
    			break;
    		}
    		value = i;
    	}

    	if (mVMChangListener != null) {
    		mVMChangListener.onVMChanged(value);
    	}

    	mHandler.postDelayed(mUpdateTimer, 500);
    }

    private void updatePlay() {
    	if (mState != PLAYING_STATE) {
    		return;
    	}
    	if (mPlayChangeListener != null) {
    		mPlayChangeListener.onPlayChange(mPlayValue);
    	}
    	mPlayValue++;

    	mHandler.postDelayed(mUpdatePlayTimer, 500);
    }

    public int progress() {
        if (mState == RECORDING_STATE || mState == PLAYING_STATE)
            return (int) ((System.currentTimeMillis() - mSampleStart)/1000);
        return 0;
    }
    
    public int getSoundLength() {
        return mSampleLength;
    }

    public String getSoundFile() {
        return mSoundFile;
    }

    public void setSoundFile(String path) {
    	mSoundFile = path;
    	mSampleFile = new File(path);
    }

    public void clear() {
        stop();
        mSampleLength = 0;
        mSampleFile = null;
        mSoundFile = null;
        signalStateChanged(IDLE_STATE);
    }

    public void startRecording(Context context) {
    	clear();
        
        if (mSampleFile == null) {
           File sampleDir = new File(LocalSetting.getInstance().getLocalPath()+ "/record");
           sampleDir.mkdirs();

           try {
            	long current = System.currentTimeMillis();
            	String datetime = DateFormat.format("yyyyMMdd_kkmmss", current).toString();
            	String path = sampleDir.getAbsolutePath() + "/" + datetime + ".dat";//".3gpp";
                mSoundFile = path;
                mSampleFile = new File(path);
            } catch (Exception e) {
                setError(SDCARD_ACCESS_ERROR);
                return;
            }
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
        mRecorder.setAudioEncodingBitRate(BITRATE_3GPP);

        // Handle IOException
        try {
            mRecorder.prepare();
        } catch(IOException exception) {
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.start();
        } catch (RuntimeException exception) {
            AudioManager audioMngr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            boolean isInCall = ((audioMngr.getMode() == AudioManager.MODE_IN_CALL));
            if (isInCall) {
                setError(IN_CALL_RECORD_ERROR);
            } else {
                setError(INTERNAL_ERROR);
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }
        mSampleStart = System.currentTimeMillis();
        setState(RECORDING_STATE);

        updateVolume();
    }
    
    public void stopRecording() {
        if (mRecorder == null)
            return;

        try {
			mRecorder.stop();
        } catch (RuntimeException exception) {
            Log.d(TAG, "" + exception.getMessage());
		} finally {
			mRecorder.release();
			mRecorder = null;
		}
 
		mSampleLength = (int)( (System.currentTimeMillis() - mSampleStart + 500)/1000);
		setState(IDLE_STATE);
    }
   
    public void startPlayback() {
        stop();

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mSampleFile.getAbsolutePath());
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            setError(INTERNAL_ERROR);
            mPlayer = null;
            return;
        } catch (IOException e) {
            setError(SDCARD_ACCESS_ERROR);
            mPlayer = null;
            return;
        }
        
        mSampleStart = System.currentTimeMillis();
        setState(PLAYING_STATE);

        mPlayValue = 0;
        updatePlay();
    }

    public void stopPlayback() {
        if (mPlayer == null) // we were not in playback
            return;

        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        setState(IDLE_STATE);

        if (mPlayChangeListener != null) {
        	mPlayChangeListener.onPlayOver();
        }
    }
    
    public void stop() {
        stopRecording();
        stopPlayback();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        stop();
        setError(SDCARD_ACCESS_ERROR);
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        stop();
    }
    
    private void setState(int state) {
        if (state == mState)
            return;
        mState = state;
        signalStateChanged(mState);
    }
    
    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onStateChanged(state);
    }
    
    private void setError(int error) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onError(error);
    }
}
