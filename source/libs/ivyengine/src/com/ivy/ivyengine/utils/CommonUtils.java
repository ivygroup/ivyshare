package com.ivy.ivyengine.utils;

import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;

public class CommonUtils {
	public static final String VIDEO_URI = Video.Media.getContentUri("external").toString();
	public static final String IMAGE_URI = Images.Media.getContentUri("external").toString();
	public static final String AUDIO_URI = Audio.Media.getContentUri("external").toString();
}
