package com.ivy.ivyengine.control;

import android.content.Context;
import android.util.Log;

import com.ivy.ivyengine.constdefines.IvyMessages;
import com.ivy.ivyengine.data.ImData;
import com.ivy.ivyengine.data.Table_Message;
import com.ivy.ivyengine.im.Im;
import com.ivy.ivyengine.im.Im.FileType;
import com.ivy.ivyengine.im.Person;

public class ImListener implements Im.OnSendFileListener,
                                    Im.OnReceiveFileListener,
                                    Im.OnMessageListener,
                                    Im.OnErrorListener,
                                    Im.OnUserListener {
    private static final String TAG = "ImListener";

    private Context mContext;
    private ImData mImData;
    private PersonMessages mPersonMessage;
    private GroupMessages mGroupMessages;
    private TranslateFileControl mTranslateFileControl;


    public ImListener(Context context, ImData imData, PersonMessages personMessages, GroupMessages groupMessages) {
    	mContext = context;
        mImData = imData;
        mPersonMessage = personMessages;
        mGroupMessages = groupMessages;
        mTranslateFileControl = new TranslateFileControl(mContext, mImData, personMessages, groupMessages);
    }

    public TranslateFileControl getTranslateFileControl() {
        return mTranslateFileControl;
    }

    // the im's call back ----------------------------------------------------------
    @Override
    public void onBeginSend(long id, Person person, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }
        Log.i(TAG, "Begin Send File " + name);
        mImData.updateMessageState((int)id, Table_Message.STATE_BEGIN);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_UPDATE, Table_Message.STATE_BEGIN, (int)id, fileType.ordinal(), name, true, person);
    }

    @Override
    public void onTranslateProcess(long id, Person person, String name, FileType fileType, long pos, long total) {
        if (mImData == null) {
            return;
        }

        mTranslateFileControl.onSendFileProcess((int)id, person, name, fileType, pos, total);
    }

    @Override
    public void onCompleteSend(long id, Person person, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }
        Log.i(TAG, "File send Over " + name);
        mImData.updateMessageState((int)id, Table_Message.STATE_OK);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_UPDATE, Table_Message.STATE_OK, (int)id, fileType.ordinal(), name, true, person);
    }

    @Override
    public void onSendFileTimeOut(long id,  Person person, String filename, FileType fileType) {
        if (mImData == null) {
            return;
        }
        Log.i(TAG, "File Send Timeout" + filename);
        mImData.updateMessageState((int)id, Table_Message.STATE_TIMEOUT);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_UPDATE, Table_Message.STATE_TIMEOUT, (int)id, -1, filename, true, null);
    }

    @Override
    public void onSendFileError(long id,  Person person, String filename, FileType fileType) {
        if (mImData == null) {
            return;
        }
        mImData.updateMessageState((int)id, Table_Message.STATE_FAILED);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_UPDATE, Table_Message.STATE_FAILED, (int)id, -1, filename, true, null);
    }

    @Override
    public void onReceiveMessage(Person from, String msg) {
        if (mImData == null) {
            return;
        }
        Log.d(TAG, "Receive message " + msg);
        int ret = mImData.addMessage(from, FileType.FileType_CommonMsg, msg, false, System.currentTimeMillis(), Table_Message.STATE_OK);
        mPersonMessage.addMessage(from);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_OK, ret, 
        		FileType.FileType_CommonMsg.ordinal(), msg, false, from);
    }

    // receive file------------------------------------------------------------
    @Override
    public RequestResult requestFileTranslate(Person from, String id, String name,
            long size, long time, FileType fileType) {
        if (mImData == null) {
            return null;
        }
        Log.i(TAG, "Request Receive File " + name);

        RequestResult requestResult = new RequestResult();
        requestResult.bIsSaveThisfile = true;
        requestResult.strSavePath = LocalSetting.getInstance().getLocalFileReceivePath(fileType);

        String path = new StringBuilder(LocalSetting.getInstance().getLocalFileReceivePath(fileType)).
                append(name).toString();
        int ret = mImData.addMessage(from, fileType, path, false, System.currentTimeMillis(), Table_Message.STATE_BEGIN);

        mTranslateFileControl.beginReceiveFile(id, ret);
        mPersonMessage.addMessage(from);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_BEGIN, ret, fileType.ordinal(), path, false, from);
        return requestResult;
    }

    @Override
    public void onTranslateProcess(Person from, String id, String name, FileType fileType, long pos, long total) {
        mTranslateFileControl.onReceiveProcess(from, id, name, fileType, pos, total);
    }

    @Override
    public void onCompleteFileReceive(Person from, String id, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }

        Log.i(TAG, "Complete Receive File " + name);
        mTranslateFileControl.endReceiveFile(from, id, name, fileType);
    }

    @Override
    public void onReceiveFileError(Person from, String id, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }

        Log.i(TAG, "File Receive Failed" + name);
        mTranslateFileControl.removeReceiveFileTaskOnly(id, name);
    }


    @Override
    public void onMsgSendFailed(long id, String msg) {
        if (mImData == null) {
            return;
        }
        Log.i(TAG, "Message Send Failed" + msg);
        mImData.updateMessageState((int)id, Table_Message.STATE_FAILED);
        IvyMessages.sendMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_UPDATE, Table_Message.STATE_FAILED, (int)id, -1, msg, true, null);
    }

    // for group message--------------------------------------------------------
    @Override
    public void onReceiveGroupMessage(Person from, String msg) {
        if (mImData == null) {
            return;
        }
        Log.d(TAG, "Receive group message " + msg);
        int ret = mImData.addGroupMessage(true, "", from, FileType.FileType_CommonMsg,msg, 
        		false, System.currentTimeMillis(), Table_Message.STATE_OK);
        mGroupMessages.addBroadCastMessage(true, "", from, FileType.FileType_CommonMsg,msg, 
        		false, System.currentTimeMillis(), Table_Message.STATE_OK, ret);
        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_OK, ret,
        		FileType.FileType_CommonMsg.ordinal(), msg, true, "", false);
    }

    @Override
    public RequestResult requestFileTranslate_Group(Person from, String id, String name, long size, long time, FileType fileType) {
        RequestResult requestResult = new RequestResult();
        requestResult.bIsSaveThisfile = true;
        requestResult.strSavePath = LocalSetting.getInstance().getLocalFileReceivePath(fileType);

        Log.i(TAG, "Request Receive Group File " + name);

        String path = new StringBuilder(LocalSetting.getInstance().getLocalFileReceivePath(fileType)).
                append(name).toString();
        int ret = mImData.addGroupMessage(true, "", from, fileType, path, 
        		false, System.currentTimeMillis(), Table_Message.STATE_BEGIN);

        mTranslateFileControl.beginReceiveFile(id, ret);

        mGroupMessages.addBroadCastMessage(true, "", from, fileType, path, 
        		false, System.currentTimeMillis(), Table_Message.STATE_BEGIN, ret);

        IvyMessages.sendGroupMessageIntent(mContext, IvyMessages.VALUE_MESSAGETYPE_NEW, Table_Message.STATE_BEGIN, ret,
        		fileType.ordinal(), path, true, "", false);
        return requestResult;
    }

    @Override
    public void onTranslateProcess_Group(Person from, String id, String name, FileType fileType, long pos, long total) {
        mTranslateFileControl.onReceiveGroupProcess(true, "", from, id, name, fileType, pos, total);
    }

    @Override
    public void onCompleteFileReceive_Group(Person from, String id, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }

        Log.i(TAG, "Complete Receive Group File " + name);
        mTranslateFileControl.endReceiveGroupFile(true, "", from, id, name, fileType);
    }

    @Override
    public void onReceiveFileError_Group(Person from, String id, String name, FileType fileType) {
        if (mImData == null) {
            return;
        }

        Log.i(TAG, "Group File Receive Failed" + name);
        mTranslateFileControl.removeReceiveGroupFileTaskOnly(true, "", id, name);
    }


    // for person--------------------------------------------------------------
    @Override
    public void onNewUser(Person p) {
        if (p == null || mImData == null) {
            return;
        }

        // Log.d(TAG, "on new user " + p.mNickName);
        mImData.updateOneUser(p);
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_NEW_USER, p);
    }

    @Override
    public void onSomeoneAbsence(Person p) {
        if (p == null || mImData == null) {
            return;
        }
        Log.d(TAG, "on onSomeoneAbsence" + p.mNickName);
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_SOMEONE_ABSENCE, p);
    }

    @Override
    public void onClearAll() {
        if (mImData == null) {
            return;
        }

        Log.d(TAG, "on clear all");
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_CLEAR_ALL, null);
    }

    @Override
    public void onSomeoneExit(Person person) {
        if (person == null || mImData == null) {
            return;
        }
        Log.d(TAG, "on someone exit " + person.mNickName);
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_SOMEONE_EXIT, person);
    }

    @Override
    public void onSomeoneHeadIcon(Person p) {
        if (p == null || mImData == null) {
            return;
        }
        IvyMessages.sendPersonBroadCast(mContext, IvyMessages.VALUE_PERSONTYPE_SOMEONE_ABSENCE, p);
    }
}
