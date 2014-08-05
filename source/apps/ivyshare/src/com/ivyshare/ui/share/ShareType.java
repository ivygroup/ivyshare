package com.ivyshare.ui.share;

import com.ivy.ivyengine.im.Im.FileType;

public class ShareType {
    
        //    public enum FileType {
        //        FileType_App,
        //        FileType_Contact,
        //        FileType_Picture,
        //        FileType_Music,
        //        FileType_Video,
        //        FileType_OtherFile,
        //        FileType_Record,
        //        FileType_CommonMsg,     // not a file, but a common message. it used in ImData.java
        //        FileType_HeadIcon,      // the SimpleIm engin use this type, trans the user head icon.
        //    }
		public static final int SHARE_TYPE_APP	 = 0;
        public static final int SHARE_TYPE_CONTACT = 1;
        public static final int SHARE_TYPE_IMAGE = 2;
        public static final int SHARE_TYPE_AUDIO = 3;
        public static final int SHARE_TYPE_VIDEO = 4;
        public static final int SHARE_TYPE_OTHER = 5;
        public static final int SHARE_TYPE_UNKNOWN = 5;
        
        public static FileType getFileType(int type){
            return FileType.values()[type];
        }
}