package com.ivyshare.util;

import com.ivy.ivyengine.im.Im;
import com.ivyshare.MyApplication;
import com.ivyshare.R;

public class StringUtils {

	public static String getStateString(int state) {
	    int stateID = 1;

	    switch (state) {
	        case Im.State_Active:
	            stateID = R.string.state_active;
	            break;
	        case Im.State_OffLine:
	            stateID = R.string.state_offline;
                break;
	        case Im.State_Idle:
	            stateID = R.string.state_idle;
                break;
	        case Im.State_Screen_Off:
	            stateID = R.string.state_screen_off;
                break;
	        case Im.State_Sleep:
	            stateID = R.string.state_sleep;
                break;
	    }

	    return MyApplication.getInstance().getString(stateID);
	}
	
}
