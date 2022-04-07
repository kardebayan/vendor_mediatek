/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ims;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ims.ImsExternalCallState;
import android.telephony.Rlog;

import com.mediatek.ims.ImsConstants;
import com.mediatek.ims.ril.ImsCommandsInterface;

import vendor.mediatek.hardware.radio.V3_0.Dialog;
/**
 * ImsEventPackageAdapter, adapter class to handle event package
 */
public class ImsEventPackageAdapter {
    private static final String LOG_TAG = "ImsEventPackageAdapter";

    // ASCII TAG from raw data
    private static final String TAG_NEXT_LINE = "<ascii_10>";
    private static final String TAG_RETURN = "<ascii_13>";
    private static final String TAG_DOUBLE_QUOTE = "<ascii_34>";

    // Type of event package
    private static final int TYPE_CONFERENCE_EVT_PKG = 1;
    private static final int TYPE_DIALOG_EVT_PKG = 2;
    private static final int TYPE_MWI = 3;

    private ImsCommandsInterface mImsRilAdapter;
    private MyHandler mHandler;
    private Context mContext;
    private int mPhoneId;

    private String mCEPData;  // Conference event package data
    private String mDEPData;  // Dialog event package data
    private String mMWIData;  // MWI data

    static final int EVENT_LTE_MESSAGE_WAITING = 0;
    static final int EVENT_IMS_DIALOG_INDICATION = 1;

    ImsEventPackageAdapter(Context ctx, Handler handler, ImsCommandsInterface imsRilAdapter,
                           int phoneId) {
        Rlog.d(LOG_TAG, "ImsEventPackageAdapter()");
        mHandler = new MyHandler(handler.getLooper());
        mImsRilAdapter = imsRilAdapter;
        mContext = ctx;
        mPhoneId = phoneId;
        mImsRilAdapter.registerForLteMsgWaiting(mHandler, EVENT_LTE_MESSAGE_WAITING, null);
        mImsRilAdapter.registerForImsDialog(mHandler, EVENT_IMS_DIALOG_INDICATION, null);
    }

    public void close() {
        mImsRilAdapter.unregisterForLteMsgWaiting(mHandler);
        mImsRilAdapter.unregisterForImsDialog(mHandler);
    }

    private void handleLetMessageWaiting(String[] msg) {
        Rlog.d(LOG_TAG, "handleLetMessageWaiting()");
        int intDataCount = 4;
        int[] intData = new int[intDataCount];
        try {
            for (int i = 0; i < intDataCount; ++i) {
                intData[i] = Integer.parseInt(msg[i]);
            }
        } catch(NumberFormatException e) {
            Rlog.d(LOG_TAG, "handleLetMessageWaiting failed: invalid params");
            return;
        }

        int callId = intData[0];
        int urcIdx = intData[2];
        int totalUrcCount = intData[3];
        String rawData = msg[4];

        if (msg.length >= 6) {
            int phoneId = Integer.parseInt(msg[5]);
            if (mPhoneId != phoneId) {
                Rlog.d(LOG_TAG, "handleLetMessageWaiting ignore, not the correct phone id");
                return;
            }
        }

        boolean isFirstPkt = (urcIdx == 1);
        mMWIData = concatData(isFirstPkt, mMWIData, rawData);
        if (urcIdx != totalUrcCount || mContext == null) {
            // do nothing
            return;
        }
        mMWIData = recoverDataFromAsciiTag(mMWIData);

        Intent intent = new Intent(ImsConstants.ACTION_LTE_MESSAGE_WAITING_INDICATION);
        intent.putExtra(ImsConstants.EXTRA_LTE_MWI_BODY, mMWIData);
        intent.putExtra(ImsConstants.EXTRA_PHONE_ID, mPhoneId);
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mContext.sendBroadcast(intent);

    }

    private String concatData(boolean isFirst, String origData, String appendData) {
        if (isFirst) {
            return appendData;
        }
        return origData.concat(appendData);
    }

    private String recoverDataFromAsciiTag(String data) {
        data = data.replaceAll(TAG_RETURN, "\r");
        data = data.replaceAll(TAG_DOUBLE_QUOTE, "\"");
        data = data.replaceAll(TAG_NEXT_LINE, "\n");
        return data;
    }

    private void handleDialogEventPackage(ArrayList<Dialog> dialogList) {
        Rlog.d(LOG_TAG, "handleDialogEventPackage()");

        ArrayList<ImsExternalCallState> result = new ArrayList<ImsExternalCallState>();
        for (Dialog dialog : dialogList) {
            Uri addr = Uri.parse(dialog.address);
            ImsExternalCallState exCallState = new ImsExternalCallState(dialog.dialogId,
                    addr, dialog.isPullable, dialog.callState,
                    dialog.callType, dialog.isCallHeld);
            result.add(exCallState);
            Rlog.d(LOG_TAG, "handleDialogEventPackage exCallState:" + dialog.dialogId +
                    dialog.address + dialog.isPullable + dialog.callState +
                    dialog.callType + dialog.isCallHeld);
        }
        Intent intent = new Intent(ImsConstants.ACTION_IMS_DIALOG_EVENT_PACKAGE);
        intent.putParcelableArrayListExtra(ImsConstants.EXTRA_DEP_CONTENT, result);
        mContext.sendBroadcast(intent);
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            Rlog.d(LOG_TAG, "MsgId: " + msg.what);
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                Rlog.d(LOG_TAG, "message error");
                return;
            }
            switch(msg.what) {
                case EVENT_LTE_MESSAGE_WAITING:
                    handleLetMessageWaiting((String[]) ar.result);
                    break;
                case EVENT_IMS_DIALOG_INDICATION:
                    handleDialogEventPackage((ArrayList<Dialog>) ar.result);
                default:
                    Rlog.d(LOG_TAG, "Unregistered event");
                    break;
            }
        }
    }
}
