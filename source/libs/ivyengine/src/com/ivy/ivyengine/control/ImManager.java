package com.ivy.ivyengine.control;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.ivy.ivyengine.constdefines.IvyMessages;
import com.ivy.ivyengine.control.SessionMessages.SessionMessage;
import com.ivy.ivyengine.data.ImData;
import com.ivy.ivyengine.data.Table_Message;
import com.ivy.ivyengine.im.Im;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivy.ivyengine.im.ImFactory;
import com.ivy.ivyengine.im.Person;
public class ImManager {
    private static final String TAG = "ImManager";

    private Context mContext;
    private Im mIm;
    private ImData mImData;
    private PersonMessages mPersonMessages;
    private PersonManager mPersonManager;
    private GroupMessages mGroupMessages;
    private SessionMessages mSessionMessages;
    private DaemonNotifactionInterface mDaemonNotifaction;
    private ImListener mImListener;
    private UserStateMonitor mUserStateMonitor;


    public ImManager(Context context) { 
        Log.d(TAG, "onCreate");

        mContext = context;

        // Init ImData here, make sure it's the first time init it
        mImData = new ImData(mContext);
        mImData.ResetUnEndMessage();

        mPersonMessages = new PersonMessages(mImData);
        mGroupMessages = new GroupMessages(mImData);
        mSessionMessages = new SessionMessages(mImData, mGroupMessages);
        mPersonManager = PersonManager.getInstance();

        mIm = ImFactory.getSimpleIm();
        mIm.init();

        mImListener = new ImListener(mContext, mImData, mPersonMessages, mGroupMessages);
        mIm.setOnUserListener(mImListener);
        mIm.setOnSendFileListener(mImListener);
        mIm.setOnMessageListener(mImListener);
        mIm.setOnFileListener(mImListener);
        mIm.setOnErrorListener(mImListener);

        mUserStateMonitor = new UserStateMonitor(mContext, this);
    }

    public void release() {
        Log.d(TAG, "release");

        mImData.ResetUnEndMessage();
        mImData.release();
        mImData = null;

        if (mDaemonNotifaction != null) {
            mDaemonNotifaction.release();
            mDaemonNotifaction = null;
        }

        mIm.release();
        mIm = null;
    }

    // interface for Im
    public void upLine() {
        Log.i(TAG, "upLine");
        mIm.upLine();
        // mIm.getList();   //temp close this feature.
    }

    public void upLine(Person to) {
        mIm.upLine(to);
    }

    public void downLine() {
        mIm.downLine();
    }

    public void absence() {
        mIm.absence();
    }

    public void sendHeadIcon() {
        mIm.sendHeadIcon();
    }

    public void changeUserState(int state) {
        mIm.changeUserState(state);
    }

    // for Data read and write.
    public Person getPerson(String key) {
        if (mImData == null) {
            return null;
        }
        return mPersonManager.getPerson(key);
    }

    public List<Person> getPersonListClone() {
        return mPersonManager.getPersonList();
    }

    public List<SessionMessage> getSessionMessageListClone() {
        return mSessionMessages.getSessionMessageList();
    }

    public List<GroupMessage> getGroupMessageListClone(boolean isBroadcast, String groupName) {
        return mGroupMessages.getGroupMessageListClone(isBroadcast, groupName);
    }

    public Cursor getPersonHistoryMessage(Person person) {
        return mImData.getMsgHistory(person);
    }

    public void saveFreeShare(FileType type, String content) {
        mImData.addFreeShare(type, content, System.currentTimeMillis());
    }

    public Cursor getFreeShareHistory() {
        return mImData.getFreeShareHistory();
    }
    public void clearFreeShareHistory() {
        mImData.clearFreeShareHistory();
    }

    // for person message------------------------------------------------------
    public int sendMessage(Person to, String msg) {
        Log.i(TAG, "Send Message " + msg);
        int ret = mImData.addMessage(to, FileType.FileType_CommonMsg, msg, true, System.currentTimeMillis(), Table_Message.STATE_OK);
        mIm.sendMessage(ret, to, msg);

        mPersonMessages.addMessage(to);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_OK, ret, FileType.FileType_CommonMsg.ordinal(), msg, true, to);
        return ret;
    }

    public int sendFile(Person to, String msg, String filename, FileType fileType) {
        Log.i(TAG, "Send File " + filename);
        int ret = mImData.addMessage(to, fileType, filename, true, System.currentTimeMillis(), Table_Message.STATE_WAITING);
        mIm.sendFile(ret, to, msg, filename, fileType);
        mPersonMessages.addMessage(to);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_WAITING, ret, fileType.ordinal(), filename, true, to);
        return ret;
    }

    public void clearUnReadMessage(Person person) {
        Log.i(TAG, "clear UnRead Message");
        mImData.clearUnReadMessage(person);
        mPersonMessages.clearUnReadMessage(person);
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_UNREAD_MESSAGECHANGE, null);
    }

    public void deleteMessage(Person person, int id) {
        Log.i(TAG, "Delete One Person Message");
        mImData.RemoveMessage(id);
        mPersonMessages.deleteOneMessage(person);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_DELETE, Table_Message.STATE_OK, id, -1, null, true, person);
    }

    public void recoverMessage(Person person, FileType type, String msg, boolean isMeSay, long timeMillis, int state) {
        Log.i(TAG, "Recover One Person Message");
        int ret = mImData.addMessage(person, type, msg, isMeSay, timeMillis, state);
        mPersonMessages.addMessage(person);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_RECOVER, state, ret, type.ordinal(), msg, isMeSay, person);
    }

    public void deleteMessage(Person person) {
        Log.i(TAG, "Delete Person Message");
        mImData.RemoveMessage(person);
        mPersonMessages.deleteMessage(person);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_DELETE, Table_Message.STATE_OK, -1, -1, null, true, person);
    }

    public void deleteAllMessage() {
        Log.i(TAG, "Delete All Message");
        mImData.RemoveAllMessage();
        mPersonMessages.clearAllMessages();
        mGroupMessages.clearAllMessages();
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_UNREAD_MESSAGECHANGE, null);
    }

    // user state---------------------------------------------------------------
    public void onResumeMyActivity() {
        if (mUserStateMonitor != null) {
            mUserStateMonitor.onResumeMyActivity();
        }
    }

    public void checkMyActive() {
        if (mUserStateMonitor != null) {
            mUserStateMonitor.checkMyActive();
        }
    }

    public void destroyUserStateMonitor() {
        mUserStateMonitor = null;
    }

    //
    public ImListener getImListener() {
        return mImListener;
    }

    public void setDaemonNotifaction(DaemonNotifactionInterface daemon) {
        mDaemonNotifaction = daemon;
    }

    public DaemonNotifactionInterface getDaemonNotifaction() {
        return mDaemonNotifaction;
    }

    public ImData getImData() {
        return mImData;
    }

    public PersonMessages getPersonMessage() {
        return mPersonMessages;
    }
    
    public GroupMessages getGroupMessage() {
        return mGroupMessages;
    }
    // for group message-------------------------------------------------------
    public int sendGroupMessage(boolean isBroadCast, String groupName, String message) {
        Log.i(TAG, "Send Group Message " + message);
        int ret = mImData.addGroupMessage(true, groupName, null, FileType.FileType_CommonMsg, 
                message, true, System.currentTimeMillis(), Table_Message.STATE_OK);
        mGroupMessages.addBroadCastMessage(true, groupName, null, FileType.FileType_CommonMsg, 
                message, true, System.currentTimeMillis(), Table_Message.STATE_OK, ret);

        List<Person> listPersons = mPersonManager.getPersonList();
        int nSize = listPersons.size();
        for (int i=0; i<nSize; i++) {
            if (isBroadCast) {
                if (listPersons.get(i).isOnline()) {
                    mIm.sendGroupMessage(ret, listPersons.get(i), message);
                }
            }
        }

        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_OK, 
                ret, FileType.FileType_CommonMsg.ordinal(), message, true, groupName, true);
        return ret;
    }

    public int sendGroupFile(boolean isBroadCast, String groupName, String msg, String filename, FileType fileType) {
        Log.i(TAG, "Send Group File " + filename);
        int ret = mImData.addGroupMessage(true, groupName, null, fileType, 
                filename, true, System.currentTimeMillis(), Table_Message.STATE_OK);
        mGroupMessages.addBroadCastMessage(true, groupName, null, fileType, 
                filename, true, System.currentTimeMillis(), Table_Message.STATE_OK, ret);
        // send to every one in the group
        List<Person> listPersons = mPersonManager.getPersonList();
        int nSize = listPersons.size();
        for (int i=0; i<nSize; i++) {
            if (isBroadCast) {
                if (listPersons.get(i).isOnline()) {
                    mIm.sendGroupFile(ret, listPersons.get(i), msg, filename, fileType);
                }
            }
        }
        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_OK, 
                ret, fileType.ordinal(), filename, true, groupName, true);
        return ret;
    }

    public void clearGroupUnReadMessage(boolean isBroadCast, String groupName) {
        Log.i(TAG, "clear UnRead Group Message");
        mImData.clearGroupUnReadMessage(isBroadCast, groupName);
        mGroupMessages.clearUnReadMessage(isBroadCast, groupName);
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_GROUP_UNREAD_MESSAGECHANGE, null);
    }

    public void deleteGroupMessage(boolean isBroadCast, String groupName, int id) {
        Log.i(TAG, "Delete one Group Message");
        mImData.RemoveGroupMessage(id);
        mGroupMessages.deleteOneMessage(id);
        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_DELETE, Table_Message.STATE_OK, id, -1, null,
                isBroadCast, groupName, true);
    }

    public void recoverGroupMessage(boolean isBroadCast, String groupName, Person p, FileType type, String msg, 
    		boolean isMeSay, long timeMillis, int state) {
        Log.i(TAG, "Recover One Group Message");
        int ret = mImData.addGroupMessage(isBroadCast, groupName, p, type, 
        		msg, isMeSay, timeMillis, state);
        mGroupMessages.addBroadCastMessage(isBroadCast, groupName, p, type, msg, isMeSay, timeMillis, state, ret);
        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_RECOVER, state, 
                ret, type.ordinal(), msg, isBroadCast, groupName, isMeSay);
    }

    public void deleteGroupMessage(boolean isBroadCast, String groupName) {
        Log.i(TAG, "Delete Group Message");
        mImData.RemoveGroupMessage(isBroadCast, groupName);
        mGroupMessages.deleteMessage(isBroadCast, groupName);
        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_DELETE, Table_Message.STATE_OK, -1, -1, null,
                isBroadCast, groupName, true);
    }
}
